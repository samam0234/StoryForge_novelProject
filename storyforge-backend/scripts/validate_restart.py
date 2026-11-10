"""임시 DB와 mock AI로 후속 챕터 생성 중 실제 Java 프로세스 종료·복구를 확인합니다.

실행: ../storyforge-ai/.venv/Scripts/python -m scripts.validate_restart --container <로컬 PostgreSQL 컨테이너>
먼저 mvnw verify로 실행 JAR를 만듭니다. 기존 DB는 사용하지 않으며 생성한 DB만 정리합니다.
"""
import argparse
import concurrent.futures
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import socket
import subprocess
import sys
import tempfile
import threading
import time
import uuid

import httpx


def emit(stage, **values):
    print(json.dumps({"stage": stage, **values}, ensure_ascii=True), flush=True)


def free_port():
    with socket.socket() as probe:
        probe.bind(("127.0.0.1", 0))
        return probe.getsockname()[1]


def java_binary():
    # Windows Oracle javapath는 실제 JVM을 자식으로 띄우는 실행기일 수 있습니다.
    # 실제 JDK 바이너리를 실행해야 Popen.kill이 검증 대상 JVM을 종료합니다.
    probe = subprocess.run(["java", "-XshowSettings:properties", "-version"],
        capture_output=True, text=True, check=True)
    match = re.search(r"^\s*java.home\s*=\s*(.+)$", probe.stderr, re.MULTILINE)
    if not match:
        raise RuntimeError("실제 JDK 경로를 확인하지 못했습니다")
    binary = Path(match.group(1).strip()) / "bin" / ("java.exe" if os.name == "nt" else "java")
    if not binary.is_file():
        raise FileNotFoundError("실제 JDK 실행 파일을 찾지 못했습니다")
    return str(binary)


def stop(process, *, crash=False):
    if process is not None and process.poll() is None:
        process.kill() if crash else process.terminate()
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=10)


def wait_ready(url, process):
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError("검증 프로세스가 시작 중 종료됐습니다. 임시 로그를 확인하세요.")
        try:
            if httpx.get(url, timeout=1, trust_env=False).status_code == 200:
                return
        except httpx.HTTPError:
            pass
        time.sleep(0.2)
    raise TimeoutError("검증 서버 시작 시간 초과")


def validate(container):
    root = Path(__file__).resolve().parents[1]
    ai_root = root.parent / "storyforge-ai"
    jar = root / "target/storyforge-backend-0.0.1-SNAPSHOT.jar"
    if not jar.is_file():
        raise FileNotFoundError("mvnw verify로 실행 JAR를 먼저 만드세요")
    java = java_binary()
    # 인증값은 상속 환경으로만 전달하며 출력하거나 파일에 쓰지 않습니다.
    details = json.loads(subprocess.check_output(["docker", "inspect", container], text=True))[0]
    db_env = dict(item.split("=", 1) for item in details["Config"]["Env"] if "=" in item)
    db_user = db_env.get("POSTGRES_USER", "postgres")
    db_password = db_env.get("POSTGRES_PASSWORD", "")
    bindings = details["NetworkSettings"]["Ports"]["5432/tcp"]
    binding = next((item for item in bindings if item["HostIp"] == "127.0.0.1"), None)
    if binding is None:
        raise ValueError("127.0.0.1에 연결된 PostgreSQL 컨테이너가 필요합니다")
    database = "storyforge_restart_" + uuid.uuid4().hex[:12]

    def sql(statement):
        return subprocess.check_output(["docker", "exec", container, "psql", "-v", "ON_ERROR_STOP=1",
            "-U", db_user, "-d", "postgres", "-tAc", statement], text=True)

    backend_port, ai_port = free_port(), free_port()
    while ai_port == backend_port:
        ai_port = free_port()
    directory = Path(tempfile.mkdtemp(prefix="storyforge-restart-validation-"))
    ai_process = backend = None
    created = False
    logs = []
    started = time.perf_counter()
    try:
        sql(f'CREATE DATABASE "{database}"')
        created = True
        shutil.copyfile(ai_root / "data/names.json", directory / "names.json")
        runner = directory / "mock_ai.py"
        runner.write_text("import os\nfrom pathlib import Path\nimport app.services.character_bank as bank\n"
            "import uvicorn\nbank._ROOT = Path(__file__).parent\n"
            "uvicorn.run('app.main:app', host='127.0.0.1', port=int(os.environ['QA_AI_PORT']))\n", encoding="utf-8")
        environment = dict(os.environ, LLM_PROVIDER="mock", MOCK_STREAM_DELAY="0.05", MAIL_ENABLED="false",
            DB_URL=f"jdbc:postgresql://127.0.0.1:{binding['HostPort']}/{database}",
            DB_USER=db_user, DB_PASSWORD=db_password, JWT_SECRET=secrets.token_urlsafe(48),
            SERVER_ADDRESS="127.0.0.1", BACKEND_PORT=str(backend_port), QA_AI_PORT=str(ai_port),
            AI_SERVICE_URL=f"http://127.0.0.1:{ai_port}", PYTHONPATH=str(ai_root), PYTHONIOENCODING="utf-8")
        flags = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0

        def launch(command, log_name, cwd):
            output = (directory / log_name).open("wb")
            logs.append(output)
            return subprocess.Popen(command, cwd=cwd, env=environment, stdout=output,
                stderr=subprocess.STDOUT, creationflags=flags)

        ai_process = launch([sys.executable, str(runner)], "ai.log", ai_root)
        wait_ready(f"http://127.0.0.1:{ai_port}/health", ai_process)

        def launch_backend(log_name):
            process = launch([java, "-jar", str(jar)], log_name, root)
            try:
                wait_ready(f"http://127.0.0.1:{backend_port}/actuator/health", process)
            except BaseException:
                stop(process)
                raise
            return process

        backend = launch_backend("backend-before.log")
        base = f"http://127.0.0.1:{backend_port}/api/v1"
        with httpx.Client(timeout=180, trust_env=False) as client:
            def request(method, path, body=None):
                response = client.request(method, base + path, json=body)
                response.raise_for_status()
                return response

            def data(method, path, body=None):
                return request(method, path, body).json()

            account = data("POST", "/auth/register", {"email": "restart@example.test", "password": secrets.token_urlsafe(24),
                "displayName": "복구 검증", "acceptTerms": True})
            client.headers["Authorization"] = "Bearer " + account["accessToken"]
            project = data("POST", "/projects", {"title": "후속 챕터 복구 검증", "genre": "미스터리"})["id"]
            prefix = f"/projects/{project}"
            request("POST", prefix + "/bible/generate", {"prompt": "고립된 등대에서 기록을 함께 찾는 두 사람"})
            request("PUT", prefix + "/bible/confirm")
            request("POST", prefix + "/outline/generate", {"chapterCount": 2})
            request("PUT", prefix + "/outline/confirm")

            def generate(number):
                response = client.post(base + prefix + f"/chapters/{number}/generate", headers={"Accept": "text/event-stream"})
                response.raise_for_status()
                assert "event:done" in response.text, "완료 이벤트가 없습니다"
                assert "event:error" not in response.text, "생성 오류 이벤트가 있습니다"

            generate(1)
            first = data("GET", prefix + "/chapters")[0]
            request("PUT", f"/chapters/{first['id']}/confirm")
            before = data("GET", f"/chapters/{first['id']}")
            entered = threading.Event()

            def interrupted_generation():
                try:
                    with client.stream("POST", base + prefix + "/chapters/2/generate",
                            headers={"Accept": "text/event-stream"}) as response:
                        response.raise_for_status()
                        for line in response.iter_lines():
                            if line.startswith("event:token"):
                                entered.set()
                    return "closed"
                except httpx.HTTPError as error:
                    return type(error).__name__

            with concurrent.futures.ThreadPoolExecutor(max_workers=1) as executor:
                interrupted = executor.submit(interrupted_generation)
                if not entered.wait(timeout=30):
                    raise TimeoutError("둘째 장 스트리밍 시작 실패")
                assert data("GET", prefix + "/status")["status"] == "CHAPTER_GENERATING"
                emit("crashing", backend_pid=backend.pid, chapter=2, first_status=before["status"])
                stop(backend, crash=True)
                emit("disconnected", result=interrupted.result(timeout=10))

            backend = launch_backend("backend-after.log")
            recovered = data("GET", prefix + "/status")["status"]
            assert recovered == "CHAPTER_REVIEW", recovered
            assert data("GET", f"/chapters/{first['id']}") == before, "이전 장이 변경됐습니다"
            assert len(data("GET", prefix + "/chapters")) == 1, "부분 둘째 장이 저장됐습니다"
            emit("recovered", status=recovered, first_chapter_preserved=True, partial_chapter_saved=False)
            generate(2)
            assert data("GET", prefix + "/status")["status"] == "CHAPTER_REVIEW"
            chapters = data("GET", prefix + "/chapters")
            assert len(chapters) == 2
            assert chapters[1]["status"] == "DRAFT"
            assert data("GET", f"/chapters/{first['id']}") == before
            emit("complete", elapsed_s=round(time.perf_counter() - started, 3), second_status=chapters[1]["status"],
                 logs=str(directory), model="mock", browser_verified=False)
    finally:
        stop(backend)
        stop(ai_process)
        for output in logs:
            output.close()
        if created:
            sql(f'DROP DATABASE "{database}"')


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--container", required=True, help="루프백 PostgreSQL 컨테이너 이름")
    args = parser.parse_args()
    validate(args.container)
