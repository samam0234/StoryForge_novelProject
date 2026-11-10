"""허용된 수동 검증 도구를 선택하고 인자를 전달하는 실행 조정기."""
import argparse
import runpy
import sys

TOOLS = ("benchmark_ollama", "validate_load", "validate_pipeline", "validate_soak")


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("tool", choices=TOOLS)
    parser.add_argument("arguments", nargs=argparse.REMAINDER)
    args = parser.parse_args(argv)
    previous = sys.argv
    try:
        sys.argv = [f"scripts.{args.tool}", *args.arguments]
        runpy.run_module(f"scripts.{args.tool}", run_name="__main__")
    finally:
        sys.argv = previous
