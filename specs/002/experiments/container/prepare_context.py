#!/usr/bin/env python3
"""Copy ONLY named experiment sources into a new external Docker build context.

Never send the project root to docker build: local docs/templates may contain
unauthorized real files. No original documents, configuration or dependencies
are copied by this helper.
"""
import argparse
from pathlib import Path
import shutil


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    source = Path(__file__).resolve().parent
    output = args.output.resolve()
    if output.exists() or output.is_relative_to(source.parents[3]):
        parser.error("Output must be NEW and outside the repository")
    files = [source / name for name in (
        "Dockerfile", "probe.py", "payload_filter.py", "create_profile.py",
        "run_container.py", "prepare_context.py")]
    files.append(source.parent / "evaluate.py")
    output.mkdir(parents=True, mode=0o700)
    for path in files:
        relative = path.relative_to(source.parent)
        target = output / "specs/002/experiments" / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(path, target)
    shutil.copyfile(source / "Dockerfile", output / "Dockerfile")
    print("Minimal experimental context: " + str(output))


if __name__ == "__main__":
    main()
