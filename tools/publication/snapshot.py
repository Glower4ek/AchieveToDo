"""Copy only the proposed publication inputs, without creating or modifying Git state."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[2]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def git_paths(*args):
    return subprocess.check_output(["git", *args, "-z"], cwd=ROOT).decode("utf-8").split("\0")[:-1]


def publication_paths():
    tracked = git_paths("ls-files")
    untracked = git_paths("ls-files", "--others", "--exclude-standard")
    accepted = []
    for name in untracked:
        if "/__pycache__/" in name or name.endswith("/audit_stdout.json"):
            continue
        if name.startswith(("src/main/java/", "src/main/resources/", "src/test/", "src/gametest/",
                            "tools/final19/", "tools/publication/", "reference/phase_a_planning/",
                            "reference/localization/fixtures/", "reference/publication/")):
            accepted.append(name)
        elif name == "PRE26_RELEASE_REPRODUCIBILITY.md":
            accepted.append(name)
        elif name.startswith(("src/main/generated/data/achievetodo/advancement/abilities/",
                              "src/main/generated/data/achievetodo/function/abilities/")):
            accepted.append(name)
    deleted = sorted(name for name in tracked if not (ROOT / name).is_file())
    included = sorted(name for name in set(tracked + accepted) if (ROOT / name).is_file())
    return included, deleted, sorted(set(untracked) - set(accepted))


def inventory():
    included, deleted, excluded = publication_paths()
    return {"schemaVersion": 1, "head": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT).decode().strip(),
            "included": {name: sha(ROOT / name) for name in included}, "deleted": deleted,
            "excludedVisibleUntracked": excluded,
            "note": "Ignored outputs/caches/local tooling are absent. Sealed XML/log/binary and runtime ZIP evidence are included."}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--destination", type=Path)
    parser.add_argument("--inventory", type=Path, required=True)
    args = parser.parse_args()
    data = inventory()
    if args.destination:
        target = args.destination.resolve()
        allowed = (ROOT / "build/tmp").resolve()
        if not target.is_relative_to(allowed) or target.exists():
            raise SystemExit("Probe must be a new directory beneath this repository's build/tmp")
        target.mkdir(parents=True)
        for name, expected in data["included"].items():
            source = ROOT / name
            if source.is_symlink():
                raise SystemExit("Publication input must not reach through a symlink: " + name)
            output = target / name
            output.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, output)
            if output.is_symlink() or sha(output) != expected:
                raise SystemExit("Probe copy mismatch: " + name)
        data["probe"] = str(target)
    args.inventory.parent.mkdir(parents=True, exist_ok=True)
    args.inventory.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"includedFiles": len(data["included"]), "deleted": data["deleted"],
                      "excludedVisibleUntracked": len(data["excludedVisibleUntracked"]), "probe": data.get("probe")}))


if __name__ == "__main__":
    main()
