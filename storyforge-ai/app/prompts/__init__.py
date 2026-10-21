"""프롬프트 템플릿 로더. 템플릿 원문은 이 폴더의 *.j2 파일에 둡니다."""
from pathlib import Path

from jinja2 import Environment, FileSystemLoader, StrictUndefined

_env = Environment(
    loader=FileSystemLoader(Path(__file__).parent),
    undefined=StrictUndefined,
    trim_blocks=True,
    lstrip_blocks=True,
    keep_trailing_newline=False,
)


def render(name: str, **context: object) -> str:
    return _env.get_template(f"{name}.j2").render(**context).strip()
