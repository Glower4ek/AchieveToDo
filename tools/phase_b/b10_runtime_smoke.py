"""B10 preparation and audit. Production Java owns every conversion and runtime load."""
import argparse
import hashlib
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools/phase_b'))
import b9_regression_certification as b9
import b8_bacap_static_certification as b8

TEMP = ROOT / 'build/tmp/phase_b_b10'
SELECTOR = 'achievetodo-test:phase_bruntime_smoke_production_smoke'
GROUPS = {
    'MAIN': [],
    'HARDCORE': ['BACAP_HARDCORE', 'BACAP_HARDCORE_OVERRIDE'],
    'TERRALITH': ['TERRALITH', 'BACAP_TERRALITH', 'BACAP_TERRALITH_OVERRIDE'],
    'AMPLIFIED_NETHER': ['AMPLIFIED_NETHER', 'BACAP_AMPLIFIED_NETHER', 'BACAP_AMPLIFIED_NETHER_OVERRIDE'],
    'NULLSCAPE': ['NULLSCAPE', 'BACAP_NULLSCAPE', 'BACAP_NULLSCAPE_OVERRIDE'],
}
COUNTS = {'MAIN': 2894, 'HARDCORE': 2894, 'TERRALITH': 2922, 'AMPLIFIED_NETHER': 2894, 'NULLSCAPE': 2895}
ATD_ABILITY_ADVANCEMENTS = 152
B8_MODES = {'MAIN': 'main', 'HARDCORE': 'hardcore', 'TERRALITH': 'terralith',
            'AMPLIFIED_NETHER': 'amplifiedNether', 'NULLSCAPE': 'nullscape'}

def id_set_sha256(ids):
    """Sorted UTF-8 IDs, one per LF-terminated line; identical to the Java gate."""
    return b9.sha(('\n'.join(sorted(ids))+'\n').encode('utf8'))

def expected_sets(mode):
    """Use hash-bound accepted B8 output and checked-in production resources only."""
    import re
    authority_path = ROOT/'reference/phase_b/b8_static_certification_resumed.json'
    authority = b9.load(authority_path)
    assert authority['status'] == 'PHASE_B_B8_STATIC_CERTIFIED'
    b8_mode = B8_MODES[mode]
    core_path = ROOT/'build/tmp/phase_b_b8/post_r2'/('codec_advancements_'+b8_mode+'.json')
    expected_hash = authority['effectiveViews'][b8_mode]['codec']['advancementInputsSha256']
    assert b9.sha(core_path.read_bytes()) == expected_hash, 'B8 core output provenance: '+mode
    core_ids = set(b9.load(core_path))
    assert len(core_ids) == COUNTS[mode] == authority['effectiveViews'][b8_mode]['advancements']
    directory = ROOT/'src/main/generated/data/achievetodo/advancement/abilities'
    paths = sorted(directory.glob('*.json'))
    assert len(paths) == ATD_ABILITY_ADVANCEMENTS
    resources = {}
    for path in paths:
        assert re.fullmatch(r'[a-z0-9_]+', path.stem)
        assert isinstance(b9.load(path).get('criteria'), dict), path
        identifier = 'achievetodo:abilities/'+path.stem
        assert identifier not in resources
        resources[identifier] = {'path':path.relative_to(ROOT).as_posix(), 'sha256':b9.sha(path.read_bytes())}
    tracked = set(b9.b7.git('ls-files', '--', directory.relative_to(ROOT).as_posix()).decode().splitlines())
    assert tracked == {r['path'] for r in resources.values()}, 'Production resources must be checked in'
    ability_path = ROOT/'src/main/java/com/diskree/achievetodo/ability/AbilityType.java'
    source = ability_path.read_text(encoding='utf8')
    constants = source.split('public enum AbilityType {', 1)[1].split('    private final ', 1)[0]
    names = re.findall(r'^    ([A-Z][A-Z0-9_]*)\(', constants, re.M)
    assert len(names) == len(set(names)) == 151
    assert 'return name().toLowerCase(Locale.ROOT);' in source
    enum_ids = {'achievetodo:abilities/'+name.lower() for name in names}
    atd_ids = set(resources)
    assert atd_ids == enum_ids | {'achievetodo:abilities/root'}
    assert not core_ids & atd_ids, 'Core/ATD sets must be disjoint'
    return {
        'b8CoreIds': sorted(core_ids), 'atdAbilityIds': sorted(atd_ids),
        'expectedFullIds': sorted(core_ids | atd_ids),
        'b8CoreExpectedCount': len(core_ids), 'atdAbilityExpectedCount': len(atd_ids),
        'expectedFullLiveCount': len(core_ids | atd_ids),
        'b8CoreIdsSha256': id_set_sha256(core_ids), 'atdAbilityIdsSha256': id_set_sha256(atd_ids),
        'expectedFullIdsSha256': id_set_sha256(core_ids | atd_ids),
        'coreAuthority': {'evidencePath':authority_path.relative_to(ROOT).as_posix(),
            'evidenceSha256':b9.sha(authority_path.read_bytes()),
            'outputPath':core_path.relative_to(ROOT).as_posix(), 'outputSha256':expected_hash},
        'atdAuthority': {'origin':'ALWAYS_ENABLED_ATD_PRODUCTION_BASE_PACK',
            'resourceDirectory':directory.relative_to(ROOT).as_posix(), 'resources':resources,
            'abilityTypeSourcePath':ability_path.relative_to(ROOT).as_posix(),
            'abilityTypeSourceSha256':b9.sha(ability_path.read_bytes()),
            'abilityTypeCount':len(names), 'rootCount':1, 'nonRootGenerated':len(enum_ids),
            'missingAbilityTypes':[], 'extraGenerated':[], 'duplicates':[]},
    }

def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf8')

def deep_main_contract(contract):
    """D1 candidates come from accepted MAIN definitions and current reward resources."""
    import re
    c1 = b9.load(ROOT/'reference/phase_b/b10_c1_live_advancement_contract_refresh.json')
    assert c1['status'] == 'PHASE_B_B10_C1_LIVE_CONTRACT_REFRESHED' and c1['readyToResumeB10']
    copy = c1['mainFreshRuntime']['bacapWorldCopyIdentity']
    source = ROOT/'build/tmp/phase_b_b10_c1/MAIN_FRESH/runtime/world/datapacks/bacap.zip'
    assert b9.sha(source.read_bytes()) == copy['worldCopySha256']
    files = b8.archive(source)
    for name in ['bacap_override', 'bacap_rewards_item', 'bacap_rewards_experience',
                 'bacap_rewards_trophy', 'bacap_cooperative_mode']:
        files.update(b8.builtin(name))
    definitions = b9.load(ROOT/contract['coreAuthority']['outputPath'])
    removed = sorted(r['oldId'] for r in b9.load(ROOT/'reference/phase_b/b5_4_root_reward_messages_resumed.json')['removedIdReferenceResolution'])
    assert len(removed) == 7
    function_path = lambda identifier: 'data/'+identifier.replace(':', '/function/')+'.mcfunction'
    macro_id = 'bacap_rewards:advancement_made_macro'
    macro = files[function_path(macro_id)].decode('utf8')
    raw_line = '$execute unless score $(tier) bac_dont_count matches 1 run scoreboard players add @s bac_advancements 1'
    points_line = '$scoreboard players operation @s bac_advancements_points += $(tier) bac_points'
    first_line = '$execute unless score $(adv_id) bac_obtained matches 1.. run function bacap_rewards:first_score_add'
    assert all(line in macro.splitlines() for line in [raw_line, points_line, first_line])
    first = files[function_path('bacap_rewards:first_score_add')].decode('utf8')
    assert 'scoreboard players add @s bac_advfirst 1' in first.splitlines()
    rows = []
    for identifier, definition in sorted(definitions.items()):
        display = definition.get('display', {})
        if (not identifier.startswith('blazeandcave:') or identifier in removed or
            identifier.endswith('/root') or not display or display.get('hidden', False) or
            len(definition.get('criteria', {})) != 1):
            continue
        reward = definition.get('rewards', {}).get('function')
        path = function_path(reward) if reward else ''
        if path not in files:
            continue
        commands = [s.strip() for s in files[path].decode('utf8').splitlines() if s.strip() and not s.lstrip().startswith('#')]
        if len(commands) != 1:
            continue
        match = re.fullmatch(r'function bacap_rewards:advancement_made_macro \{adv_id:"([^"]+)",reward_id:"([^"]+)",tier:"([^"]+)"\}', commands[0])
        if not match or match[1] != identifier:
            continue
        message = 'bacap_rewards:msg/'+match[2]
        exp = 'bacap_rewards:exp/'+match[2]
        if function_path(message) not in files or function_path(exp) not in files:
            continue
        exp_commands = [s.strip() for s in files[function_path(exp)].decode('utf8').splitlines() if s.strip() and not s.lstrip().startswith('#')]
        xp = re.fullmatch(r'xp add @s (\d+)', exp_commands[0]) if exp_commands else None
        if not xp or any(not s.startswith('tellraw @s ') for s in exp_commands[1:]):
            continue
        criterion = next(iter(definition['criteria']))
        assert definition.get('requirements', [[criterion]]) == [[criterion]]
        rows.append({'advancementId':identifier, 'criteria':[criterion], 'rewardFunction':reward,
            'messageFunction':message, 'tier':match[3], 'experienceFunction':exp, 'experienceAmount':int(xp[1]),
            'advancementSha256':b9.sha(json.dumps(definition, sort_keys=True, separators=(',', ':')).encode()),
            'wrapperSha256':b9.sha(files[path]), 'messageSha256':b9.sha(files[function_path(message)]),
            'experienceSha256':b9.sha(files[function_path(exp)])})
    assert rows, 'No current one-criterion reward candidate'
    return {'rewardCandidates':rows, 'orderedCandidateIdsSha256':id_set_sha256(r['advancementId'] for r in rows),
        'candidatePlanSha256':b9.sha(json.dumps(rows, sort_keys=True, separators=(',', ':')).encode()),
        'coreAuthority':contract['coreAuthority'], 'removedOldIds':removed,
        'rewardGraph':{'macroId':macro_id, 'macroSha256':b9.sha(files[function_path(macro_id)]),
            'firstScoreFunctionSha256':b9.sha(files[function_path('bacap_rewards:first_score_add')]),
            'rawIncrement':1, 'rawGuardObjective':'bac_dont_count', 'pointsObjective':'bac_advancements_points',
            'tierPointsObjective':'bac_points', 'firstIncrement':1, 'firstGuardObjective':'bac_obtained',
            'rawLine':raw_line, 'pointsLine':points_line, 'firstLine':first_line}}

def companion_contract(mode, contract):
    """Exact B5.6-owned resources; no companion identity guessed by the harness."""
    assert mode in GROUPS and mode != 'MAIN'
    path = ROOT/'reference/phase_b/b5_6_companions_resumed.json'
    evidence = b9.load(path)
    assert evidence['status'] == 'PHASE_B_B5_6_COMPLETE'
    result = {'authorityPath':path.relative_to(ROOT).as_posix(), 'authoritySha256':b9.sha(path.read_bytes()),
              'mode':mode, 'messages':[], 'merges':[], 'wrappers':[]}
    core = b9.load(ROOT/contract['coreAuthority']['outputPath'])
    def checked(row):
        assert b9.sha((ROOT/row['path']).read_bytes()) == row['resultingSha256'], row['path']
        return row
    def function_id(row):
        identifier = b8.resource_id('data/'+row['path'].split('/data/', 1)[1], 'function', '.mcfunction')
        assert identifier
        return identifier
    if mode == 'HARDCORE':
        rows = evidence['hardcoreMessages']['records']
        assert len(rows) == 6
        for row in sorted(rows, key=lambda r:r['path']):
            checked(row)
            result['messages'].append({'functionId':function_id(row), 'targetId':row['SearchTarget'],
                'translationKeys':row['translationKeys'], 'sourcePath':row['path'], 'sha256':row['resultingSha256']})
    else:
        companion = 'BACAP_'+mode
        rows = sorted((checked(r) for r in evidence['advancementMerges']['records'] if r['companion'] == companion),
                      key=lambda r:r['advancementId'])
        assert len(rows) == {'TERRALITH':11,'AMPLIFIED_NETHER':2,'NULLSCAPE':1}[mode]
        for row in rows:
            identifier = row['advancementId']
            assert identifier in core and core[identifier].get('rewards', {}).get('function') == row['rewardFunction']
            result['merges'].append({'advancementId':identifier, 'rewardFunction':row['rewardFunction'],
                'sourcePath':row['path'], 'sha256':row['resultingSha256']})
        representative = result['merges'][0]
        assert representative['rewardFunction'].startswith('bacap_rewards:')
        result['representativeMerge'] = {**representative,
            'messageFunction':'bacap_rewards:msg/'+representative['rewardFunction'].split(':', 1)[1],
            'selectionReason':'first sorted B5.6 merged ID with an accepted current reward wrapper/message path'}
        if mode == 'TERRALITH':
            wrappers = evidence['terralithWrappers']['records']
            assert len(wrappers) == 24
            for row in sorted(wrappers, key=lambda r:r['path']):
                checked(row)
                assert row['advancementId'] in core
                result['wrappers'].append({'functionId':function_id(row), 'advancementId':row['advancementId'],
                    'macroTarget':row['macroTarget'], 'arguments':row['arguments'], 'sha256':row['resultingSha256']})
            assert len({r['macroTarget'] for r in result['wrappers']}) == 1
            macro_id = result['wrappers'][0]['macroTarget']
            macro_path = ROOT/'src/main/resources/resourcepacks/bacap_override'/('data/'+macro_id.replace(':','/function/')+'.mcfunction')
            macro = macro_path.read_text(encoding='utf8')
            assert '$execute if score terralith_score bac_settings matches 1 unless score $(tier) bac_dont_count matches 1 run scoreboard players add @s bac_advancements 1' in macro.splitlines()
            assert '$execute if score terralith_score bac_settings matches 1 run scoreboard players operation @s bac_advancements_points += $(tier) bac_points' in macro.splitlines()
            result['macroGate'] = {'functionId':macro_id, 'sha256':b9.sha(macro_path.read_bytes()),
                'scoreHolder':'terralith_score', 'objective':'bac_settings', 'enabledValue':1}
            result['repairedHelpers'] = ['blazeandcave:technical/biome_branch1_end', 'blazeandcave:technical/biome_branch2_end']
            assert set(result['repairedHelpers']) <= set(core)
        elif mode == 'NULLSCAPE':
            row = checked(evidence['modifiedExistingCompanionResource'])
            assert row['advancementId'] == 'nullscape:root' and row['advancementId'] in core
            result['messages'].append({'functionId':function_id(row), 'targetId':row['advancementId'],
                'translationKeys':[], 'sourcePath':row['path'], 'sha256':row['resultingSha256']})
    result['forbiddenSearchTargets'] = sorted({r['oldId'] for r in b9.load(ROOT/'reference/phase_b/b5_4_root_reward_messages_resumed.json')['removedIdReferenceResolution']} | {'data:advancement/root'})
    return result

def prepare(mode, attempt=None, deep=False, companion=False):
    b9.precheck()
    accepted = b9.load(ROOT / 'reference/phase_b/b9_regression_certification_resumed.json')
    assert accepted['status'] == 'PHASE_B_B9_REGRESSION_CERTIFIED' and accepted['readyForB10']
    for path, expected in accepted['certificationArtifacts'].items():
        assert b9.sha((ROOT/path).read_bytes()) == expected, path
    source = (ROOT/'src/main/java/com/diskree/achievetodo/injection/mixin/client/CreateWorldScreenMixin.java').read_text(encoding='utf8')
    # Match the production addPack ordering; reward and cooperative options are explicitly ON.
    import re
    order = re.findall(r'tempDataPackRepository.addPack\((ExternalPack|InternalPack)\.(\w+)\.getDatapackName\(\)\);', source)
    wanted = ['BACAP', 'BACAP_OVERRIDE', *GROUPS[mode], 'BACAP_REWARDS_ITEM',
              'BACAP_REWARDS_EXPERIENCE', 'BACAP_REWARDS_TROPHY', 'BACAP_COOPERATIVE_MODE']
    selection = [(kind, name) for kind, name in order if name in wanted]
    assert [name for kind, name in selection] == wanted
    specs = {s['enum']: s for s in b8.SOURCE_INPUTS}
    external = []
    for kind, name in selection:
        if kind == 'ExternalPack':
            spec = dict(specs[name])
            spec['source'] = str(ROOT/spec['source'])
            assert b9.sha(pathlib.Path(spec['source']).read_bytes()) == spec['sourceSha256']
            external.append(spec)
    assert not deep or mode == 'MAIN', 'D1 authorizes MAIN only'
    assert not companion or mode != 'MAIN', 'D2 excludes MAIN'
    assert not (deep and companion), 'One explicit runtime surface per process'
    contract = expected_sets(mode)
    deep_contract = deep_main_contract(contract) if deep else None
    selector = 'achievetodo-test:phase_bruntime_smoke_main_deep_smoke' if deep else SELECTOR
    companion_plan = companion_contract(mode, contract) if companion else None
    if companion: selector = 'achievetodo-test:phase_bruntime_smoke_companion_smoke'
    work = TEMP/(attempt or mode)
    assert not work.exists(), 'Fresh per-mode directory required'
    work.mkdir(parents=True)
    ids = [('file/'+name.lower()+'.zip') if kind == 'ExternalPack' else ('achievetodo:'+name.lower())
           for kind, name in selection]
    write(work/'plan.json', {'mode': mode, 'external': external, 'productionPackIds': ids,
        'expectedAdvancementCount': COUNTS[mode], 'advancementSetContract': contract,
        'deepContract': deep_contract, 'companionContract': companion_plan, 'selector': selector,
        'rewardOptions': {'item': True, 'experience': True, 'trophy': True, 'cooperative': True},
        'selectionSource': 'CreateWorldScreenMixin.prepareDatapacks', 'productionOrder': selection})
    # Ignored, explicitly B10-scoped Gradle configuration; historical build.gradle is untouched.
    init = "allprojects { afterEvaluate { loom.runs.gameTest.runDir("+json.dumps(str(work.relative_to(ROOT)/'runtime').replace('\\','/'))+")\n tasks.named('runGameTest', JavaExec) {\n"
    for key, value in {'achievetodo.b10.mode': mode, 'achievetodo.b10.output': str(work),
                       'achievetodo.b10.plan': str(work/'plan.json')}.items():
        init += "systemProperty("+json.dumps(key)+", "+json.dumps(value.replace('\\','/'))+")\n"
    init += "workingDir("+json.dumps(str(work).replace('\\','/'))+")\n} } }\n"
    (work/'b10.init.gradle').write_text(init, encoding='utf8')
    print(json.dumps({'mode': mode, 'plan': str(work/'plan.json'), 'selector': selector,
                      'productionPackIds': ids}, ensure_ascii=False))

def audit(mode, attempt=None):
    work = TEMP/(attempt or mode)
    installed = b9.load(work/'installed.json')
    live = b9.load(work/'live.json') if (work/'live.json').exists() else None
    log = (work/'gradle.log').read_text(encoding='utf8', errors='replace')
    import re
    errors = [line for line in log.splitlines() if re.search(r'/ERROR\]|/FATAL\]|CommandSyntaxException|Couldn.t load|Failed to (load|parse)|Unknown function|Unknown advancement', line)]
    result = {'mode': mode, 'installed': installed, 'live': live, 'errorLines': errors,
              'logSha256': b9.sha((work/'gradle.log').read_bytes()),
              'certified': False, 'deepSmokeCompleted': bool(live and live.get('deepSmokeCompleted'))}
    write(work/'audit.json', result)
    print(json.dumps({'mode': mode, 'live': None if live is None else live.get('liveAdvancementCount'),
                      'errorCount': len(errors), 'errors': errors[:20]}, ensure_ascii=False))

def reconcile(mode, attempt=None):
    """Identify additional live production resources; never alter the runtime count gate."""
    work = TEMP/(attempt or mode)
    installed = b9.load(work/'installed.json')
    live = b9.load(work/'live.json')
    plan = b9.load(work/'plan.json')
    minecraft = ROOT/'.gradle-user-home/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.2/minecraft-merged-deobf-26.2.jar'
    core = b8.archive(minecraft)
    for kind, name in plan['productionOrder']:
        if kind == 'ExternalPack':
            files = b8.archive(pathlib.Path(installed['worldDatapacks'])/(name.lower()+'.zip'))
        else:
            files = b8.builtin(name.lower())
        core.update(files)
    core_ids = {i for path in core if (i := b8.resource_id(path, 'advancement', '.json'))}
    live_ids = set(live['liveAdvancementIds'])
    extra = sorted(live_ids-core_ids)
    generated = {}
    for path in (ROOT/'src/main/generated').rglob('*.json'):
        rel = path.relative_to(ROOT/'src/main/generated').as_posix()
        identifier = b8.resource_id(rel, 'advancement', '.json')
        if identifier:
            generated[identifier] = {'path':path.relative_to(ROOT).as_posix(), 'sha256':b9.sha(path.read_bytes())}
    assert len(core_ids) == plan['expectedAdvancementCount']
    assert not core_ids-live_ids
    assert set(extra) == set(generated)
    result = {'expectedB8CoreCount':len(core_ids), 'actualLiveCount':len(live_ids),
        'coreIdsSha256':b9.sha(('\n'.join(sorted(core_ids))+'\n').encode()),
        'missingCoreIds':[], 'additionalLiveIds':extra, 'additionalResources':generated,
        'additionalCount':len(extra), 'additionalOrigin':'ALWAYS_ENABLED_ATD_PRODUCTION_BASE_PACK',
        'testModAdvancementCount':0, 'unexplainedAdditionalIds':[],
        'countGatePassed':False,
        'b8LayerOrderOmitsBaseModPack':True,
        'productionSelectionFaithful':live['selectedPackIds']==installed['requestedSelectedIds']}
    write(work/'count_reconciliation.json', result)
    print(json.dumps({k:v for k,v in result.items() if k not in ['additionalLiveIds','additionalResources']},ensure_ascii=False))

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--prepare', choices=GROUPS)
    parser.add_argument('--audit', choices=GROUPS)
    parser.add_argument('--reconcile', choices=GROUPS)
    parser.add_argument('--attempt')
    parser.add_argument('--deep-main', action='store_true')
    parser.add_argument('--companion-smoke', action='store_true')
    args = parser.parse_args()
    if args.prepare: prepare(args.prepare, args.attempt, args.deep_main, args.companion_smoke)
    elif args.audit: audit(args.audit, args.attempt)
    elif args.reconcile: reconcile(args.reconcile, args.attempt)
    else: parser.error('Choose --prepare or --audit; no implicit stage continuation')
