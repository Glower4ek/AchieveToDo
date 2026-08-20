from __future__ import annotations

import csv
import json
import re
from collections import OrderedDict
from pathlib import Path
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parents[1]

CURRENT_OVERLAY_PATH = ROOT / "src" / "main" / "resources" / "assets" / "minecraft" / "lang" / "ru_ru.json"
OFFICIAL_1_18_PATH = ROOT / "tmp_ru_audit" / "bacap-language-pack-1.18" / "assets" / "minecraft" / "lang" / "ru_ru.json"
OFFICIAL_1_21_ZIP_PATH = ROOT / "reference" / "localization" / ".bacap_language_pack_1.21.zip"
OFFICIAL_1_21_ZIP_MEMBER = "assets/minecraft/lang/ru_ru.json"
FULL_REAUDIT_TSV_PATH = ROOT / "reference" / "localization" / "phase_a_ru_full_reaudit.tsv"
FULL_MANUAL_CANDIDATES_TSV_PATH = ROOT / "reference" / "localization" / "phase_a_manual_ru_final_v2.tsv"
FINAL_MANUAL_TSV_PATH = ROOT / "reference" / "localization" / "phase_a_manual_ru_final_v2_remaining.tsv"
UI_OFFICIAL_AUDIT_TSV_PATH = ROOT / "reference" / "localization" / "phase_a_manual_ru_ui_official_audit.tsv"
PHASE_A_BACAP_ZIP_PATH = ROOT / "reference" / "phase_a_preservation" / "files" / "final" / "bacap.zip"

JSON_TRANSLATE_PATTERN = re.compile(r'"translate"\s*:\s*"((?:\\.|[^"\\])*)"')
COMMAND_TRANSLATE_PATTERN = re.compile(r'translate\s*:\s*"((?:\\.|[^"\\])*)"')

FINAL_MANUAL_OVERRIDES = {
    "\"No, let's detonate instead!\" says his brother, Cherry #2": "«Нет, давай лучше всё взорвём!» — говорит его братец, Вишенка №2",
    "Newton's third law - the only way humans have ever": "Третий закон Ньютона — единственный известный человечеству способ куда-то попасть:",
    "figured out of getting somewhere is to leave something behind": "нужно оставить что-то позади",
    "Created by ": "Создал ",
    "Let Me Out": "Выпустите меня",
    "This sign can’t stop me because I can't see!": "Этот знак меня не остановит, ведь я его не вижу!",
    "It's a sword, axe and…": "Это меч, топор и...",
    "This isn't just any fence post…": "Это не какой-то обычный столб забора...",
}

INTENTIONAL_ENGLISH_KEYS = {
    "'s Advancements Pack!",
    "and",
}


def parse_json_text(raw: str) -> OrderedDict[str, str]:
    cleaned_lines = [line for line in raw.splitlines() if not line.lstrip().startswith("#")]
    return json.loads("\n".join(cleaned_lines), object_pairs_hook=OrderedDict)


def load_json(path: Path) -> OrderedDict[str, str]:
    return parse_json_text(path.read_text(encoding="utf-8"))


def load_zip_json(path: Path, member: str) -> OrderedDict[str, str]:
    with ZipFile(path) as archive:
        return parse_json_text(archive.read(member).decode("utf-8"))


def load_tsv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle, delimiter="\t"))


def normalize_key(value: str) -> str:
    return (
        value.replace("\u2019", "'")
        .replace("\u2018", "'")
        .replace("\u201c", '"')
        .replace("\u201d", '"')
        .replace("\u2026", "...")
    )


def build_ui_official_mapping(ui_rows: list[dict[str, str]], official_1_21: dict[str, str]) -> dict[str, str]:
    mapping: dict[str, str] = {}
    for row in ui_rows:
        official_key = row["official_1_21_key"]
        if not official_key:
            continue
        if official_key not in official_1_21:
            raise KeyError(f"Missing official 1.21 UI key: {official_key}")
        mapping[normalize_key(row["key"])] = official_1_21[official_key]
    return mapping


def build_normalized_mapping(source: dict[str, str]) -> dict[str, str]:
    mapping: dict[str, str] = {}
    for key, value in source.items():
        normalized_key = normalize_key(key)
        if value and normalized_key not in mapping:
            mapping[normalized_key] = value
    return mapping


def collect_runtime_translate_keys() -> set[str]:
    keys: set[str] = set()
    for relative in (
        ROOT / "src" / "main" / "resources" / "resourcepacks" / "bacap_override",
        ROOT / "src" / "main" / "resources" / "resourcepacks" / "bacap_hardcore_override",
        ROOT / "src" / "main" / "resources" / "resourcepacks" / "bacap_terralith_override",
        ROOT / "src" / "main" / "resources" / "resourcepacks" / "bacap_amplified_nether_override",
        ROOT / "src" / "main" / "resources" / "resourcepacks" / "bacap_nullscape_override",
    ):
        if not relative.exists():
            continue
        for path in relative.rglob("*"):
            if path.is_file() and path.suffix in {".json", ".mcfunction"}:
                add_translate_keys(path.read_text(encoding="utf-8"), keys)
    with ZipFile(PHASE_A_BACAP_ZIP_PATH) as archive:
        for name in archive.namelist():
            if name.endswith(".json") or name.endswith(".mcfunction"):
                add_translate_keys(archive.read(name).decode("utf-8"), keys)
    return keys


def add_translate_keys(text: str, keys: set[str]) -> None:
    for pattern in (JSON_TRANSLATE_PATTERN, COMMAND_TRANSLATE_PATTERN):
        for match in pattern.finditer(text):
            fragment = match.group(1).replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t")
            try:
                keys.add(json.loads('"' + fragment + '"'))
            except json.JSONDecodeError:
                keys.add(match.group(1))


def build_runtime_variants(runtime_keys: set[str]) -> dict[str, set[str]]:
    variants: dict[str, set[str]] = {}
    for key in runtime_keys:
        variants.setdefault(normalize_key(key), set()).add(key)
    return variants


def apply_translation(
    overlay: OrderedDict[str, str],
    runtime_variants: dict[str, set[str]],
    key: str,
    value: str,
    *,
    only_if_missing: bool = False,
) -> None:
    if not only_if_missing or key not in overlay:
        overlay[key] = value
    for variant in runtime_variants.get(normalize_key(key), set()):
        if not only_if_missing or variant not in overlay:
            overlay[variant] = value


def main() -> None:
    overlay = load_json(CURRENT_OVERLAY_PATH)
    official_1_18 = load_json(OFFICIAL_1_18_PATH)
    official_1_21 = load_zip_json(OFFICIAL_1_21_ZIP_PATH, OFFICIAL_1_21_ZIP_MEMBER)
    official_1_18_mapping = build_normalized_mapping(official_1_18)
    reaudit_rows = load_tsv(FULL_REAUDIT_TSV_PATH)
    full_manual_rows = load_tsv(FULL_MANUAL_CANDIDATES_TSV_PATH)
    manual_rows = load_tsv(FINAL_MANUAL_TSV_PATH)
    ui_official_rows = load_tsv(UI_OFFICIAL_AUDIT_TSV_PATH)
    ui_official_mapping = build_ui_official_mapping(ui_official_rows, official_1_21)
    runtime_variants = build_runtime_variants(collect_runtime_translate_keys())

    for key, value in list(overlay.items()):
        if value:
            apply_translation(overlay, runtime_variants, key, value, only_if_missing=True)

    for row in reaudit_rows:
        key = row["key"]
        source = row["match_source"]
        if source == "OFFICIAL_LP_1_18":
            apply_translation(overlay, runtime_variants, key, official_1_18[key], only_if_missing=True)
        elif source == "OFFICIAL_LP_1_21_COMPATIBLE":
            official_key = row["official_1_21_key"] or key
            apply_translation(overlay, runtime_variants, key, official_1_21[official_key], only_if_missing=True)

    for row in manual_rows:
        key = row["key"]
        manual_value = row["ru_manual"]
        if manual_value:
            apply_translation(overlay, runtime_variants, key, manual_value)
            continue
        normalized_key = normalize_key(key)
        if normalized_key in ui_official_mapping:
            apply_translation(overlay, runtime_variants, key, ui_official_mapping[normalized_key])
        elif normalized_key in official_1_18_mapping:
            apply_translation(overlay, runtime_variants, key, official_1_18_mapping[normalized_key])

    for row in full_manual_rows:
        key = row["key"]
        normalized_key = normalize_key(key)
        if key not in overlay and normalized_key in ui_official_mapping:
            apply_translation(overlay, runtime_variants, key, ui_official_mapping[normalized_key], only_if_missing=True)
        elif key not in overlay and normalized_key in official_1_18_mapping:
            apply_translation(overlay, runtime_variants, key, official_1_18_mapping[normalized_key], only_if_missing=True)

    for key, value in FINAL_MANUAL_OVERRIDES.items():
        apply_translation(overlay, runtime_variants, key, value)

    for key in INTENTIONAL_ENGLISH_KEYS:
        overlay.pop(key, None)

    CURRENT_OVERLAY_PATH.write_text(
        json.dumps(overlay, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
