"""E1 (0.4.0 rc.4, Minecraft 26.3, port/AchieveToDo-Purpur): static map from each certified ability to the product source that guards it and to its thresholds.

Usage: campaign_e1.py <out-dir> [configs-dir ...]
For every group in CAMPAIGN_GROUPS.json writes <out-dir>/E1_<group>.json with, per ability:
  - direct guard sites in the Purpur plugin source: "ABILITY" string literals and AbilityType.ABILITY references
    (file, lines, SHA-256 of the file, git blob at HEAD), plus the nativeguard Spec lines that name it;
  - family resolvers that cover it indirectly (food, tools, armor, trades, landmarks, blocks, items);
  - the threshold in every progression config found in the given configs dirs (*.toml, e.g. a campaign
    runtime's plugins/AchieveToDo/progression/configs after the plugin generated easy/normal/hard/chaos).
An ability with neither a direct site nor a family resolver is reported as UNMAPPED.
Also writes E1_SUMMARY.json with the repository HEAD and the counts.
"""
import hashlib, json, re, subprocess, sys
from pathlib import Path

root = Path(__file__).parent
REPO = root.parent.parent / 'repo'
PORT = REPO / 'port' / 'AchieveToDo-Purpur'
SRC = PORT / 'src' / 'main' / 'java'

FAMILIES = [
    (re.compile(r'^eat_'), 'food', ('AbilityResolvers.findFoodAbility', 'AbilityType.findEatFoodAbility')),
    (re.compile(r'^use_(wooden|stone|copper|iron|golden|diamond|netherite)_tools$'), 'tool material', ('AbilityResolvers.findToolMaterialAbility',)),
    (re.compile(r'^equip_'), 'equipment', ('AbilityResolvers.findEquipmentAbility',)),
    (re.compile(r'^trade_with_'), 'trade', ('AbilityResolvers.findTradeAbility',)),
    (re.compile(r'^interact_inside_'), 'landmark', ('AbilityResolvers.findLandmarkAbility',)),
    (re.compile(r'^open_'), 'block', ('AbilityResolvers.findBlockAbility',)),
    (re.compile(r'.'), 'item', ('AbilityResolvers.findItemAbility',)),
]


# Stage 4 review (r6): text search alone misses the sites that really decide some abilities (lookup tables, family
# resolvers, client mixins) and cannot tell whether a site is live. The certified composition is gameplay mode
# CERTIFICATION: AchieveToDoPlugin.onEnable starts bridge + progression + BukkitGameplayRuntime (native agents,
# expanded listener) and returns before the legacy restriction runtime is built.
P = 'port/AchieveToDo-Purpur/src/main/java/com/diskree/achievetodo/'
C = 'src/main/java/com/diskree/achievetodo/'
INACTIVE = [(P + 'landmark/ExactLandmarkGate.java', 'deprecated gate; the live path uses only its Result enum via CanonicalLandmarkMembership')]  # 0.3.0: legacy path removed
# Hand-verified enforcement sites: (ability regex, repo-relative file, first line, last line, text that must be there, role).
CURATED = [
    (r'^vision$', C + 'injection/mixin/client/GameRendererMixin.java', 38, 38, 'isAbilityLocked(AbilityType.VISION)',
     'client mod: blackout overlay while VISION is locked (client-local ability)'),
    (r'^vision$|^open_inventory$', P + 'bridge/BridgeWire.java', 95, 100, 'entry.getValue().unlocked()',
     'server -> client bridge state: threshold and unlocked flag per ability that the client-local guards read'),
    (r'^open_inventory$', C + 'injection/mixin/client/MinecraftClientMixin.java', 39, 39, 'isAbilityLocked(AbilityType.OPEN_INVENTORY)',
     'client mod: inventory key ignored while OPEN_INVENTORY is locked'),
    (r'^open_inventory$', P + 'bridge/BukkitBridgeRuntime.java', 300, 309, 'getAbilityState(ability).isUnlocked()',
     'server: client denial intents are honoured only for abilities the server holds locked'),
    (r'^equip_', P + 'gameplay/NativeEquipmentAbilities.java', 8, 16, 'EQUIP_" + material + "_ARMOR',
     'native identity map item -> EQUIP_* ability (copper included) used by NativeTransactionAgent'),
    (r'^equip_', P + 'gameplay/nativeguard/NativeTransactionAgent.java', 76, 101, 'denied("equipment", actor, stack)',
     'native equipment guard: Equippable / ArmorSlot / dispenser routes'),
    (r'^trade_with_(?!wandering)', P + 'gameplay/nativeguard/NativeLandmarkOperations.java', 215, 238, 'ability="TRADE_WITH_"+name.toUpperCase',
     'native villager trade guard: profession -> TRADE_WITH_*; 0.4.0 offersGated checks before Villager.getOffers() (known issue 5)'),
    (r'^trade_with_|^open_beacon$', P + 'gameplay/BukkitExpandedGameplayListener.java', 76, 82, 'e.setCancelled(true)',
     'expanded listener: InventoryOpenEvent cancelled for a locked container/menu ability'),
    (r'^ignite_tnt$', P + 'gameplay/nativeguard/NativeComplexSpecs.java', 31, 32, '"tnt-manual"',
     'native TNT hooks: manual ignite and projectile ignite'),
    (r'^unlock_vault$', P + 'gameplay/nativeguard/NativeComplexSpecs.java', 44, 44, '"vault-membership"',
     'native vault membership filter (silent, as in the Fabric original)'),
    (r'^interact_inside_', P + 'landmark/CanonicalLandmarkMembership.java', 105, 124, 'findLandmarkAbility',
     'live landmark decision: matched landmark record -> INTERACT_INSIDE_* lock state'),
    (r'^interact_inside_abandoned_camp$', P + 'gameplay/nativeguard/NativeLandmarkMappings.java', 30, 47, '"abandoned_camp"',
     '0.4.0: native landmark capture admits the 18 abandoned camp structures (rc.4 smoke fix)'),
]


def curated_sites(ability, files):
    out = []
    for rx, rel, a, b, expect, role in CURATED:
        if not re.search(rx, ability):
            continue
        path = REPO / rel
        data = path.read_bytes()
        # 0.3.0: line numbers moved after the legacy removal; locate the hand-verified text, it must be unique-ish.
        hits = [i + 1 for i, l in enumerate(data.decode('utf8').splitlines()) if expect in l]
        if not hits:
            raise SystemExit(f'E1 curated site gone: {rel} lacks {expect!r}')
        a, b = hits[0], hits[-1]
        out.append(dict(file=rel, lines=[a, b], sha256=sha(data), role=role, expect=expect, active=True))
    return out


def activity(rel):
    return next(((False, why) for prefix, why in INACTIVE if rel.startswith(prefix)), (True, None))


DEFINITION = re.compile(r'/achievetodo/(ability|progression)/')


def sha(data: bytes):
    return hashlib.sha256(data).hexdigest()


def git(*args):
    return subprocess.run(['git', '-C', str(REPO), *args], capture_output=True, text=True, check=True).stdout.strip()


def read_configs(dirs):
    configs = {}
    for d in dirs:
        for f in sorted(Path(d).glob('*.toml')):
            values, table = {}, None
            for line in f.read_text(encoding='utf8').splitlines():
                line = line.split('#', 1)[0].strip()
                if line.startswith('['):
                    table = line.strip('[]').strip()
                elif table == 'abilities' and '=' in line:
                    k, v = (x.strip() for x in line.split('=', 1))
                    values[k.strip('"')] = int(v)
            entry = {'file': str(f), 'sha256': sha(f.read_bytes()), 'values': values}
            key = f.stem
            if key in configs and configs[key]['sha256'] != entry['sha256']:
                # Same file name, different content (CHAOS: one chaos_<seed>.toml per runtime, and the generator's
                # order is not stable across JVM starts): keep both, labelled by the runtime that generated it.
                runtime = next((p for p in Path(d).parts if p.startswith('campaign')), str(d))
                first = configs.pop(key)
                configs[f'{key}@' + next((p for p in Path(first['file']).parts if p.startswith('campaign')), 'first')] = first
                key = f'{key}@{runtime}'
            elif any(k.startswith(key + '@') for k in configs):
                key = f'{key}@' + next((p for p in Path(d).parts if p.startswith('campaign')), str(d))
            if key not in configs or configs[key]['sha256'] != entry['sha256']:
                configs[key] = entry
    return configs


def main(argv):
    out = Path(argv[0])
    out.mkdir(parents=True, exist_ok=True)
    configs = read_configs(argv[1:])
    head = git('rev-parse', 'HEAD')
    groups = json.loads((root / 'CAMPAIGN_GROUPS.json').read_text())
    files = {}
    for f in sorted(SRC.rglob('*.java')):
        rel = f.relative_to(REPO).as_posix()
        data = f.read_bytes()
        files[rel] = dict(text=data.decode('utf8'), sha256=sha(data))
    blobs = {}
    for line in git('ls-tree', '-r', 'HEAD', '--', PORT.relative_to(REPO).as_posix()).splitlines():
        meta, path = line.split('\t', 1)   # "<mode> blob <sha>\t<path>"
        blobs[path] = meta.split()[2]
    resolver_sites = {}
    for rel, f in files.items():
        for name in {n for _, _, ns in FAMILIES for n in ns}:
            if name in f['text']:
                resolver_sites.setdefault(name, []).append(rel)

    summary = dict(repoHead=head, groups=len(groups), abilities=0, unmapped=[], configs={k: v['sha256'] for k, v in configs.items()})
    for group, abilities in groups.items():
        entries = []
        for ability in abilities:
            upper = ability.upper()
            pat = re.compile(r'"' + upper + r'"|AbilityType\.' + upper + r'\b')
            sites = []
            for rel, f in files.items():
                lines = [i + 1 for i, l in enumerate(f['text'].splitlines()) if pat.search(l)]
                if lines:
                    specs = [l.strip()[:220] for l in f['text'].splitlines() if pat.search(l) and 'new Spec(' in l]
                    live, why = activity(rel)
                    sites.append(dict(file=rel, lines=lines, sha256=f['sha256'], gitBlob=blobs.get(rel), specs=specs,
                                      active=live, inactiveReason=why))
            family = next(((label, names) for rx, label, names in FAMILIES if rx.search(ability)), None)
            family_sites = {n: [dict(file=r, active=activity(r)[0], inactiveReason=activity(r)[1]) for r in resolver_sites.get(n, [])]
                            for n in family[1]} if family else {}
            enforcement = curated_sites(ability, files)
            # Definition files (enum, presets, config codec) name every ability; they are not guard sites.
            definitions = [x for x in sites if DEFINITION.search(x['file'])]
            sites = [x for x in sites if not DEFINITION.search(x['file'])]
            live_direct = [x for x in sites if x['active']]
            live_family = any(x['active'] for v in family_sites.values() for x in v)
            mapped = bool(live_direct) or live_family or bool(enforcement)
            entries.append(dict(ability=ability, enforcementSites=enforcement, directSites=sites, definitionSites=[x['file'] for x in definitions],
                                family=family[0] if family and any(family_sites.values()) else None, familyResolverSites=family_sites,
                                thresholds={k: v['values'].get(ability) for k, v in configs.items()},
                                mapping='CURATED' if enforcement else 'DIRECT' if live_direct else ('FAMILY' if mapped else 'UNMAPPED')))
            summary['abilities'] += 1
            if not mapped:
                summary['unmapped'].append(ability)
        (out / f'E1_{group}.json').write_text(json.dumps(dict(group=group, repoHead=head, abilities=entries,
                                                              configs={k: {'file': v['file'], 'sha256': v['sha256']} for k, v in configs.items()}),
                                                         indent=2, ensure_ascii=False), encoding='utf8')
    (out / 'E1_SUMMARY.json').write_text(json.dumps(summary, indent=2), encoding='utf8')
    print(f"E1: {summary['groups']} groups, {summary['abilities']} abilities, unmapped={summary['unmapped']}, configs={list(configs)}")


if __name__ == '__main__':
    main(sys.argv[1:])
