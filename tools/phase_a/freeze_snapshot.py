from __future__ import annotations

import json
import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
OUTPUT_ROOT = REPO_ROOT / "reference" / "phase_a_freeze"
OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)

ABILITY_PATH = REPO_ROOT / "src/main/java/com/diskree/achievetodo/ability/AbilityType.java"
LANDMARK_PATH = REPO_ROOT / "src/main/java/com/diskree/achievetodo/ability/LandmarkType.java"
PROGRESSIONS_PATH = REPO_ROOT / "src/main/java/com/diskree/achievetodo/ability/Progressions.java"
CHAOS_PATH = REPO_ROOT / "src/main/java/com/diskree/achievetodo/ability/ChaosProgressionGenerator.java"
INTERNAL_PACK_PATH = REPO_ROOT / "src/main/java/com/diskree/achievetodo/client/InternalPack.java"
CONSTANTS_PATH = REPO_ROOT / "src/main/java/com/diskree/achievetodo/server/Constants.java"
MODE_PATH = REPO_ROOT / "src/main/java/com/diskree/achievetodo/ability/ProgressionModeType.java"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def method_block(source: str, signature: str) -> str:
    start = source.index(signature)
    brace_start = source.index("{", start)
    depth = 0
    for i in range(brace_start, len(source)):
        ch = source[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return source[brace_start + 1:i]
    raise ValueError(f"Unclosed block for {signature}")


def enum_constants(source: str, enum_name: str) -> list[str]:
    enum_index = source.index(f"enum {enum_name}")
    brace_start = source.index("{", enum_index)
    depth = 0
    end = None
    for i in range(brace_start, len(source)):
        ch = source[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                end = i
                break
    if end is None:
        raise ValueError(f"Enum {enum_name} not closed")
    body = source[brace_start + 1:end]
    match = re.search(r"\n\s*private\s+", body)
    constants_region = body[:match.start()] if match else body
    matches = re.findall(r"^\s*([A-Z0-9_]+)\s*(?:\(|,|;)", constants_region, flags=re.MULTILINE)
    return matches


def ability_layers(source: str) -> dict[str, str]:
    layers: dict[str, str] = {}
    for name, layer in re.findall(
        r"^\s*([A-Z0-9_]+)\s*\(\s*AbilityUnlockedToastType\.[A-Z_]+,\s*AbilitiesHierarchyLayerType\.([A-Z_]+),",
        source,
        flags=re.MULTILINE,
    ):
        layers[name] = layer
    return layers


def parse_add_cycle_calls(block: str) -> list[tuple[str, list[str]]]:
    calls = []
    pattern = re.compile(r"addCycle\s*\(\s*progression\s*,\s*([^,]+?)\s*,(.*?)\);", re.DOTALL)
    for spacing_expr, args in pattern.findall(block):
        abilities = re.findall(r"\b([A-Z][A-Z0-9_]+)\b", args)
        calls.append((spacing_expr.strip(), abilities))
    return calls


def build_progression(initial_pairs: list[tuple[str, int]], cycles: list[tuple[int, list[str]]]) -> dict[str, int]:
    progression: dict[str, int] = {}
    for name, value in initial_pairs:
        progression[name] = value
    for spacing, abilities in cycles:
        required = max(progression.values()) if progression else 0
        for ability in abilities:
            required += spacing
            progression[ability] = required
    return progression


def fixed_cycles(progressions_source: str, extra_spacing: int) -> list[tuple[int, list[str]]]:
    helper = method_block(progressions_source, "private static void addFixedProgressionCycles")
    calls = parse_add_cycle_calls(helper)
    spacings = []
    current = extra_spacing + 2
    for _ in range(len(calls)):
        spacings.append(current)
        current += 2
    return [(spacings[i], calls[i][1]) for i in range(len(calls))]


def named_progression(progressions_source: str, method_name: str) -> dict[str, int]:
    block = method_block(progressions_source, f"public static @NotNull Map<AbilityType, Integer> {method_name}()")
    initials = [(name, int(value)) for name, value in re.findall(r"progression\.put\(\s*([A-Z0-9_]+)\s*,\s*(-?\d+)\s*\);", block)]
    cycles = []
    for spacing_expr, abilities in parse_add_cycle_calls(block):
        cycles.append((int(spacing_expr), abilities))
    fixed_match = re.search(r"addFixedProgressionCycles\(progression,\s*(\d+)\s*\);", block)
    if fixed_match:
        cycles.extend(fixed_cycles(progressions_source, int(fixed_match.group(1))))
    return build_progression(initials, cycles)


class JavaRandom:
    def __init__(self, seed: int) -> None:
        self.seed = (seed ^ 0x5DEECE66D) & ((1 << 48) - 1)

    def next(self, bits: int) -> int:
        self.seed = (self.seed * 25214903917 + 11) & ((1 << 48) - 1)
        return self.seed >> (48 - bits)

    def next_int(self, bound: int | None = None) -> int:
        if bound is None:
            return self.next(32)
        if bound <= 0:
            raise ValueError("bound must be positive")
        if (bound & -bound) == bound:
            return (bound * self.next(31)) >> 31
        while True:
            bits = self.next(31)
            value = bits % bound
            if bits - value + (bound - 1) >= 0:
                return value

    def next_boolean(self) -> bool:
        return self.next(1) != 0

    def next_double(self) -> float:
        return ((self.next(26) << 27) + self.next(27)) / float(1 << 53)


def java_shuffle(values: list[str], random: JavaRandom) -> list[str]:
    result = list(values)
    for i in range(len(result), 1, -1):
        j = random.next_int(i)
        result[i - 1], result[j] = result[j], result[i - 1]
    return result


def get_chaos_priority(ability: str, layer: str) -> int:
    if ability == "OPEN_CRAFTING_TABLE":
        return 100
    if ability in {"INTERACT_INSIDE_FORTRESS", "INTERACT_INSIDE_STRONGHOLD", "USE_ENDER_EYE", "ENTER_NETHER", "ENTER_END"}:
        return 90
    if ability in {"OPEN_BEACON", "OPEN_ENDER_CHEST", "OPEN_SMITHING_TABLE", "OPEN_ANVIL", "OPEN_STONECUTTER"}:
        return 80
    if ability in {"USE_GOLDEN_TOOLS", "USE_WOODEN_TOOLS", "USE_STONE_TOOLS", "USE_IRON_TOOLS", "USE_SHIELD", "USE_WATER_BUCKET", "USE_FLINT_AND_STEEL"}:
        return 70
    if ability in {"EQUIP_LEATHER_ARMOR", "EQUIP_CHAINMAIL_ARMOR", "EQUIP_IRON_ARMOR", "EQUIP_ELYTRA"}:
        return 60
    if ability in {"THROW_ENDER_PEARL", "USE_OMINOUS_BOTTLE", "PLACE_END_CRYSTAL"}:
        return 50
    if ability in {"EAT_PUFFERFISH", "EAT_ROTTEN_FLESH", "EAT_SPIDER_EYE", "EAT_SUSPICIOUS_STEW", "EAT_POISONOUS_POTATO"}:
        return 40
    if layer in {"UPGRADE", "BLOCKS", "ACTIONS"}:
        return 30
    if layer in {"TRADING", "LANDMARK", "FOOD"}:
        return 0
    return 100


def generate_chaos(hard_progression: dict[str, int], layers: dict[str, str], seed: int) -> dict[str, int]:
    initially_unlocked_flag = 0
    permanently_locked_flag = -1
    min_advancements = 2
    max_advancements = 1000
    init_chance = 1.0
    locked_chance = 0.2

    random = JavaRandom(seed)
    abilities = list(hard_progression.keys())
    counts = list(hard_progression.values())
    shuffled_abilities = java_shuffle(abilities, random)
    shuffled_counts = java_shuffle(counts, random)
    progression = dict(zip(shuffled_abilities, shuffled_counts))

    shifted: dict[str, int] = {}
    for ability, old_count in progression.items():
        priority = get_chaos_priority(ability, layers[ability])
        if priority == 100:
            new_count = old_count
        else:
            shift_range = round((100 - priority) * 0.5)
            if shift_range < 0:
                shift_range = 0
            shift_magnitude = random.next_int(shift_range) if shift_range > 0 else 0
            shift = shift_magnitude if random.next_boolean() else -shift_magnitude
            new_count = old_count + shift
        if new_count < min_advancements:
            new_count = min_advancements
        elif new_count > max_advancements:
            new_count = max_advancements
        shifted[ability] = new_count

    fixed: dict[str, int] = {}
    for ability, old_count in shifted.items():
        priority = get_chaos_priority(ability, layers[ability])
        base_count = hard_progression[ability]
        if priority == 0:
            new_count = old_count
        elif priority == 100:
            new_count = base_count
        else:
            ratio = priority / 100.0 * random.next_double()
            new_count = round(old_count + ratio * (base_count - old_count))
        fixed[ability] = int(new_count)

    flagged: dict[str, int] = {}
    for ability, old_count in fixed.items():
        if ability == "VISION" or random.next_double() * 100.0 < init_chance:
            new_count = initially_unlocked_flag
        elif get_chaos_priority(ability, layers[ability]) != 100 and random.next_double() * 100.0 < locked_chance:
            new_count = permanently_locked_flag
        else:
            new_count = old_count
        flagged[ability] = new_count
    return flagged


def write_toml(path: Path, version: int, ordered_abilities: list[str], progression: dict[str, int]) -> None:
    lines = [f"version = {version}", "", "[abilities]"]
    lines.extend(f"{ability.lower()} = {progression[ability]}" for ability in ordered_abilities)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> None:
    ability_source = read(ABILITY_PATH)
    landmark_source = read(LANDMARK_PATH)
    progressions_source = read(PROGRESSIONS_PATH)
    internal_pack_source = read(INTERNAL_PACK_PATH)
    constants_source = read(CONSTANTS_PATH)
    mode_source = read(MODE_PATH)

    abilities = enum_constants(ability_source, "AbilityType")
    landmarks = enum_constants(landmark_source, "LandmarkType")
    internal_packs = enum_constants(internal_pack_source, "InternalPack")
    layers = ability_layers(ability_source)

    total_advancements = int(re.search(r"TOTAL_ADVANCEMENTS_COUNT\s*=\s*(\d+)", constants_source).group(1))
    default_mode = re.search(r"return\s+([A-Z_]+);", method_block(mode_source, "public static ProgressionModeType getDefaultMode()")).group(1).lower()
    mode_versions = {
        name.lower(): int(version)
        for name, version in re.findall(r"^\s*([A-Z_]+)\((\d+)\)[,;]?$", mode_source, flags=re.MULTILINE)
    }

    easy = named_progression(progressions_source, "getEasyProgression")
    normal = named_progression(progressions_source, "getNormalProgression")
    hard = named_progression(progressions_source, "getHardProgression")

    seeds = [0, 1, 42, -1, 1234567890123456789]
    chaos = {str(seed): generate_chaos(hard, layers, seed) for seed in seeds}

    write_toml(OUTPUT_ROOT / "easy.toml", mode_versions["easy"], abilities, easy)
    write_toml(OUTPUT_ROOT / "normal.toml", mode_versions["normal"], abilities, normal)
    write_toml(OUTPUT_ROOT / "hard.toml", mode_versions["hard"], abilities, hard)
    for seed_text, progression in chaos.items():
        write_toml(OUTPUT_ROOT / f"chaos_{seed_text}.toml", mode_versions["chaos"], abilities, progression)

    summary = {
        "ability_ids_ordered": [ability.lower() for ability in abilities],
        "landmark_ids_ordered": [landmark.lower() for landmark in landmarks],
        "internal_pack_names_ordered": [pack.lower() for pack in internal_packs],
        "total_advancements_count": total_advancements,
        "config_defaults": {
            "default_mode": default_mode,
            "mode_versions": mode_versions,
        },
        "progression_ranges": {
            "easy": {"min": min(easy.values()), "max": max(easy.values())},
            "normal": {"min": min(normal.values()), "max": max(normal.values())},
            "hard": {"min": min(hard.values()), "max": max(hard.values())},
        },
        "chaos_fixed_seeds": seeds,
    }
    (OUTPUT_ROOT / "snapshot.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
