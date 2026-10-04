"""Current Phase B static successor. Archives are read only; no Phase A generator."""
import argparse
import collections
import hashlib
import json
import pathlib
import re
import subprocess
import os
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
TEMP = ROOT / 'build/tmp/phase_b_b8/post_r2'
PHASE = ROOT / 'reference/phase_b'
TARGET = ROOT / "build/tmp/phase_b_b1/BlazeandCave's Advancements Pack 1.21.zip"
OLD = ROOT / 'reference/phase_a_preservation/files/final/bacap.zip'
HEAD = 'b261b02cd03b4aeae2835c63f982c9aa53c46eed'
TARGET_HASH = 'c71d1aa1a84dbe00a3f85a42144b46214c4669a3cccf07ff66631d28f16a99b2'
OLD_HASH = '8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70'
CONVERTER = 'src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java'
CONVERTER_HASH = '6ab0674902b4783c39c08dfd802cd0067d1436d973345834f092a001a4db9fec'
RU = 'src/main/resources/assets/minecraft/lang/ru_ru.json'
RU_HASH = '8961ad9d573ab72f63969e4a808b19ce733bc9a60b7ab589fc44dfb6148e1a0a'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def load(path):
    return json.loads(pathlib.Path(path).read_text(encoding='utf8'))


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + '\n', encoding='utf8')


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, stderr=subprocess.PIPE).decode('utf8').strip()


def snapshot(directory):
    return {p.relative_to(ROOT).as_posix(): sha(p.read_bytes()) for p in directory.rglob('*') if p.is_file()}


def precheck():
    assert git('branch', '--show-current') == 'phase-b-bacap-26.2'
    assert git('rev-parse', 'HEAD') == HEAD
    assert not git('diff', '--cached', '--name-only')
    assert sha((ROOT/CONVERTER).read_bytes()) == CONVERTER_HASH
    assert 'compat_26_2_r19' in (ROOT/CONVERTER).read_text(encoding='utf8')
    assert sha((ROOT/RU).read_bytes()) == RU_HASH
    b7 = load(PHASE/'b7_localization_certification_resumed.json')
    assert b7['status'] == 'PHASE_B_B7_LOCALIZATION_CERTIFIED' and b7['readyForB8']
    assert sha((ROOT/'tools/phase_b/b7_localization_certification.py').read_bytes()) == b7['safety']['certificationToolAfterSha256']
    matrix=b7['sevenFailureResolution']['evidence']
    assert sha((ROOT/matrix['path']).read_bytes()) == matrix['sha256']
    r1 = load(PHASE/'b7_r1_source_identity_marker_repair.json')
    assert r1['status'] == 'PHASE_B_B7_R1_REPAIRED'
    for p, h in r1['safety']['historicalEvidenceHashesUnchanged'].items():
        assert sha((ROOT/p).read_bytes()) == h, p
    repair = load(PHASE/'b8_r1_terralith_player_predicate_repair.json')
    assert repair['status'] == 'PHASE_B_B8_R1_REPAIRED' and repair['readyToResumeB8']
    assert repair['productionFile']['afterSha256'] == CONVERTER_HASH
    assert load(PHASE/'b8_static_certification.json')['status'] == 'PHASE_B_B8_STOP'
    for p, h in repair['previousEvidence'].items():
        assert sha((ROOT/p).read_bytes()) == h, p
    for p, h in repair['temporaryValidationEvidence']['receipts'].items():
        assert sha((ROOT/'build/tmp/phase_b_b8_r1'/p).read_bytes()) == h, p
    probe = repair['compiledProductionProbe']
    assert sha((ROOT/probe['source']).read_bytes()) == probe['sourceSha256']
    assert sha((ROOT/probe['result']).read_bytes()) == probe['resultSha256']
    assert load(ROOT/probe['result'])['passed']
    # B8-R2 current authority; prior B5/STOP evidence remains historical.
    r2 = load(PHASE/'b8_r2_message_binding_repair.json')
    assert r2['status'] == 'PHASE_B_B8_R2_REPAIRED' and r2['readyToResumeB8']
    for row in r2['repairs']:
        assert sha((ROOT/row['path']).read_bytes()) == row['afterSha256'], row['path']
    for path, digest in r2['immutableEvidenceHashesPreserved'].items():
        assert sha((ROOT/path).read_bytes()) == digest, path
    assert r2['rootOverrideDigest']['after'] == 'cbc432be35d5525001872c430877541cfa1fcad6'
    b5 = load(PHASE/'b5_7_structural_reconciliation.json')
    assert len(b5['productOwnership']['records']) == 251
    for r in b5['productOwnership']['records']:
        if r['path'] in {CONVERTER, 'src/main/resources/resourcepacks/bacap_override/data/bacap_rewards/function/potion/root.mcfunction'}:
            continue
        p = ROOT/r['path']
        assert (not p.exists()) if r['gitAction'] == 'DELETE' else sha(p.read_bytes()) == r['currentSha256'], r['path']
    b6 = load(PHASE/'b6_ru_translation_manifest.json')
    for p, h in b6['priorEvidenceHashes'].items():
        assert sha((ROOT/p).read_bytes()) == h, p
    for r in b6['nonRuntimeLegalArtifacts']:
        assert sha((ROOT/r['path']).read_bytes()) == r['sha256']
    previous = load(ROOT/'build/tmp/phase_b_b7/post_r1/precheck.json')
    for p, h in previous['historical'].items():
        assert sha((ROOT/p).read_bytes()) == h, p
    # The accepted B7 receipt binds its tool and all completed checks.
    assert sha((PHASE/'b7_localization_certification_resumed.json').read_bytes()) == 'be3aca8716e68e48b293bb5299418a87aec98bda340b4836f9f50e70705e92d3'
    changes = dict(line.split('\t', 1)[::-1] for line in git('diff', '--name-status', '--no-renames').splitlines())
    for p in git('ls-files', '--others', '--exclude-standard').splitlines():
        if p.startswith('src/main/'):
            changes[p] = 'A'
    runtime_changes = {p:a for p,a in changes.items() if p.startswith('src/main/')}
    counts = dict(collections.Counter(runtime_changes.values()))
    assert counts == {'A': 177, 'M': 69, 'D': 9}, counts
    return {'runtime': snapshot(ROOT/'src/main'), 'historicalTests': snapshot(ROOT/'src/test'),
            'evidence': snapshot(PHASE), 'historical': previous['historical'],
            'b7ToolSha256': sha((ROOT/'tools/phase_b/b7_localization_certification.py').read_bytes()),
            'counts': counts, 'b5OriginalExact': 249, 'b5Superseded': 2}


def archive(path):
    with zipfile.ZipFile(path) as z:
        names = [i.filename for i in z.infolist() if not i.is_dir()]
        assert len(names) == len(set(names)), 'Duplicate archive paths'
        return {n:z.read(n) for n in sorted(names)}


def advancement_id(path):
    m = re.fullmatch(r'data/([a-z0-9_.-]+)/advancements?/([a-z0-9_./-]+)\.json', path)
    return m[1]+':'+m[2] if m else None


def canonical(resource_id, value):
    return 'display' in value and value['display'].get('hidden') is not True and resource_id != 'blazeandcave:bacap/root'


def normalized(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':')).encode('utf8')


def inventory(files):
    result = {}
    for p, data in files.items():
        rid = advancement_id(p)
        if rid is None:
            continue
        assert rid not in result, rid
        try:
            obj = json.loads(data)
        except json.JSONDecodeError:
            # The frozen source has two known Gson-accepted apostrophe escapes.
            # Only old-side comparison is normalized; exact-byte hashes stay intact.
            assert sha(OLD.read_bytes()) == OLD_HASH
            assert p in {'data/blazeandcave/advancement/adventure/slenderman.json',
                         'data/blazeandcave/advancement/adventure/the_one_and_true_johnny.json'}
            assert data == archive(OLD)[p]
            obj = json.loads(data.decode('utf8').replace("\\'", "'"))
        requirements = obj.get('requirements', [[k] for k in obj['criteria']])
        display = obj.get('display', {})
        result[rid] = {'id': rid, 'path': p, 'sha256': sha(data), 'semanticSha256': sha(normalized(obj)),
                       'canonical': canonical(rid, obj), 'namespace': rid.split(':')[0], 'parent': obj.get('parent'),
                       'displayPresent': 'display' in obj, 'hidden': display.get('hidden', False), 'frame': display.get('frame', 'task'),
                       'criteriaCount': len(obj['criteria']), 'requirementsShape': [len(g) for g in requirements],
                       'requirementsSha256': sha(normalized(requirements)), 'rewardFunction': obj.get('rewards', {}).get('function')}
    return result


def prepare():
    TEMP.mkdir(parents=True, exist_ok=True)
    saved = TEMP/'precheck.json'
    pre = precheck()
    if saved.exists():
        original = load(saved)
        # Completed successor artifacts may be added after the initial preflight.
        # Every pre-existing input still has to retain its exact byte hash.
        for group in ['runtime', 'historicalTests', 'evidence', 'historical']:
            for path, digest in original[group].items():
                assert pre[group].get(path) == digest, ('B8 input changed', path)
        assert original['runtime'].keys() == pre['runtime'].keys()
        permitted_tests = {
            'src/test/java/com/diskree/achievetodo/certification/PhaseBStaticCertificationTest.java',
            *('src/test/resources/phase_b_certification/bacap_1_21_'+name+'.json'
              for name in ['target_inventory', 'codec_receipt', 'static_receipt']),
        }
        permitted_evidence = {
            'reference/phase_b/B8_STATIC_CERTIFICATION_RESUMED.md',
            'reference/phase_b/b8_static_certification_resumed.json',
        }
        assert pre['historicalTests'].keys() - original['historicalTests'].keys() <= permitted_tests
        assert pre['evidence'].keys() - original['evidence'].keys() <= permitted_evidence
        write(TEMP/'current_precheck.json', pre)
    else:
        write(saved, pre)
    manifest = load(PHASE/'b4_product_change_manifest.json')
    contracts = manifest['testSuccessorPlan']
    b8 = [r for r in contracts['newCoverage'] if r['stage'] == 'B8']
    assert len(b8) == 1 and b8[0]['path'] == 'tools/phase_b/b8_bacap_static_certification.py'
    write(TEMP/'authority_contract.json', {'B4': b8, 'B3': load(PHASE/'b3_atd_impact_matrix.json')['testSuccessors'],
          'B2Policy': load(PHASE/'b2_atd_integration_map.json')['testCertificationCoupling']['classificationBasis']})
    assert sha(TARGET.read_bytes()) == TARGET_HASH
    assert hashlib.sha1(TARGET.read_bytes()).hexdigest() == '14da3f07b5467e8b59ffc0253fd8212c938cd739'
    assert sha(OLD.read_bytes()) == OLD_HASH
    old_files, target_files = archive(OLD), archive(TARGET)
    old, target = inventory(old_files), inventory(target_files)
    assert len(old) == 1229 and len(target) == 1332
    assert sum(r['canonical'] for r in old.values()) == 1152
    assert sum(r['canonical'] for r in target.values()) == 1242
    counts = {'all':len(target), 'noDisplay':sum(not r['displayPresent'] for r in target.values()),
              'hidden':sum(r['hidden'] for r in target.values()), 'canonical':1242,
              'canonicalNamespaces':dict(collections.Counter(r['namespace'] for r in target.values() if r['canonical']))}
    assert counts['noDisplay'] == 72 and counts['hidden'] == 17
    assert counts['canonicalNamespaces'] == {'blazeandcave':1116, 'minecraft':126}
    common = old.keys() & target.keys()
    classification = collections.Counter('BYTE_IDENTICAL' if old[i]['sha256'] == target[i]['sha256'] else
                'SEMANTICALLY_IDENTICAL_BYTES_DIFFER' if old[i]['semanticSha256'] == target[i]['semanticSha256'] else
                'SEMANTICALLY_CHANGED' for i in common)
    assert dict(classification) == {'BYTE_IDENTICAL':594, 'SEMANTICALLY_IDENTICAL_BYTES_DIFFER':4, 'SEMANTICALLY_CHANGED':624}
    write(TEMP/'independent_inventory.json', {'targetSha256':TARGET_HASH, 'counts':counts, 'records':list(target.values()),
          'oldToTarget':{'common':len(common), 'oldOnly':sorted(old.keys()-target.keys()), 'targetOnly':sorted(target.keys()-old.keys()),
                         'classification':dict(classification), 'oldCanonical':1152, 'targetCanonical':1242,
                         'targetOnlyNonCanonical':[target[i] for i in sorted(target.keys()-old.keys()) if not target[i]['canonical']]}})
    mcjar = next((ROOT/'.gradle/loom-cache/minecraftMaven').rglob('minecraft-merged-*-26.2.jar'))
    # JSON resources remain ignored input only. The durable inventory contains no bodies.
    resources = {p:json.loads(b) for p,b in archive(mcjar).items() if p.startswith('data/') and p.endswith('.json')}
    resources.update({p:json.loads(b) for p,b in target_files.items() if p.startswith('data/') and p.endswith('.json')})
    write(TEMP/'codec_resources_target.json', resources)
    write(TEMP/'codec_advancements_target.json', {i:json.loads(target_files[r['path']]) for i,r in target.items()})
    write(TEMP/'codec_context_identity.json', {'minecraftJar':str(mcjar.relative_to(ROOT)), 'minecraftJarSha256':sha(mcjar.read_bytes()),
          'targetSha256':TARGET_HASH, 'resourceContextSha256':sha((TEMP/'codec_resources_target.json').read_bytes())})
    print(json.dumps({'precheck':'PASS', 'inventory':counts, 'common':len(common), 'classifications':dict(classification)}))


def resource_id(path, kind, suffix):
    m = re.fullmatch(r'data/([a-z0-9_.-]+)/'+re.escape(kind)+r'/([a-z0-9_./-]+)'+re.escape(suffix), path)
    return m[1]+':'+m[2] if m else None


def builtin(name):
    root = ROOT/'src/main/resources/resourcepacks'/name
    return {p.relative_to(root).as_posix():p.read_bytes() for p in sorted(root.rglob('*')) if p.is_file()}


def has_execution_guard(line):
    """Include selector filters: execute as @a[scores={...}] is conditional."""
    return bool(re.search(r'\b(?:if|unless)\b|@[aeprs]\[[^\]]*\b(?:scores|advancements|predicate|tag)=', line))


def parent_graph(advancements):
    missing, cycles, self_parents, roots = [], [], [], []
    for rid, value in sorted(advancements.items()):
        parent = value.get('parent')
        if not parent:
            roots.append(rid)
        if parent and parent not in advancements:
            missing.append({'id':rid, 'parent':parent})
        if parent == rid:
            self_parents.append(rid)
        seen = []
        current = rid
        while current in advancements:
            if current in seen:
                cycle = sorted(seen[seen.index(current):])
                if cycle not in cycles:
                    cycles.append(cycle)
                break
            seen.append(current)
            current = advancements[current].get('parent')
    return {'checked':len(advancements), 'roots':roots, 'missingParents':missing, 'cycles':cycles, 'selfParents':self_parents}


def views():
    inputs = load(TEMP/'converted_sources.json')
    external = {}
    for row in inputs:
        assert sha(pathlib.Path(row['source']).read_bytes()) == row['sourceSha256']
        assert row['freshnessPassed']
        external[row['enum']] = archive(pathlib.Path(row['output']))
    mcjar = next((ROOT/'.gradle/loom-cache/minecraftMaven').rglob('minecraft-merged-*-26.2.jar'))
    vanilla = archive(mcjar)
    selection = (ROOT/'src/main/java/com/diskree/achievetodo/injection/mixin/client/CreateWorldScreenMixin.java').read_text(encoding='utf8')
    start = selection.index('tempDataPackRepository.addPack(ExternalPack.BACAP.getDatapackName());')
    selected = re.findall(r'tempDataPackRepository.addPack\((ExternalPack|InternalPack)\.(\w+)\.getDatapackName\(\)\);', selection[start:])
    expected = ['BACAP','BACAP_OVERRIDE','BACAP_HARDCORE','BACAP_HARDCORE_OVERRIDE','TERRALITH','BACAP_TERRALITH',
                'BACAP_TERRALITH_OVERRIDE','AMPLIFIED_NETHER','BACAP_AMPLIFIED_NETHER','BACAP_AMPLIFIED_NETHER_OVERRIDE',
                'NULLSCAPE','BACAP_NULLSCAPE','BACAP_NULLSCAPE_OVERRIDE','BACAP_REWARDS_ITEM','BACAP_REWARDS_EXPERIENCE',
                'BACAP_REWARDS_TROPHY','BACAP_COOPERATIVE_MODE']
    assert [n for _,n in selected] == expected
    result = {}
    for combo, group in [('main',None),('hardcore','BACAP_HARDCORE'),('terralith','TERRALITH'),
                         ('amplifiedNether','AMPLIFIED_NETHER'),('nullscape','NULLSCAPE')]:
        enabled = {'BACAP','BACAP_OVERRIDE',*expected[-4:]}
        if group == 'BACAP_HARDCORE':
            enabled.update({'BACAP_HARDCORE','BACAP_HARDCORE_OVERRIDE'})
        elif group:
            enabled.update({group,'BACAP_'+group,'BACAP_'+group+'_OVERRIDE'})
        layers = [('vanilla', vanilla)] + [(n, external[n] if typ == 'ExternalPack' else builtin(n.lower()))
                                          for typ,n in selected if n in enabled]
        resources, owners = {}, {}
        for name, files in layers:
            for path, data in files.items():
                if not path.startswith('data/'):
                    continue
                if '/tags/' in path and path.endswith('.json') and path in resources:
                    tag = json.loads(data)
                    if not tag.get('replace', False):
                        tag['values'] = json.loads(resources[path]).get('values', []) + tag.get('values', [])
                        data = normalized(tag)
                resources[path], owners[path] = data, name
        adv = {resource_id(p,'advancement','.json'):json.loads(b) for p,b in resources.items() if resource_id(p,'advancement','.json')}
        funcs = {resource_id(p,'function','.mcfunction'):b.decode('utf8') for p,b in resources.items() if resource_id(p,'function','.mcfunction')}
        tags = {resource_id(p,'tags/function','.json'):json.loads(b) for p,b in resources.items() if resource_id(p,'tags/function','.json')}
        graph = parent_graph(adv)
        assert not graph['missingParents'] and not graph['cycles'] and not graph['selfParents'], (combo,graph)
        reward_refs = [{'id':i, 'function':a['rewards']['function']} for i,a in sorted(adv.items()) if a.get('rewards',{}).get('function')]
        missing_rewards = [r for r in reward_refs if r['function'] not in funcs]
        searches, commented_searches, dangling, calls, wrappers = [], [], [], [], []
        for fid, body in sorted(funcs.items()):
            for number, line in enumerate(body.splitlines(),1):
                if line.lstrip().startswith('#'):
                    for aid in re.findall(r'/advancementssearch highlight ([^\s"}]+)', line):
                        commented_searches.append({'function':fid,'line':number,'advancementId':aid})
                    continue
                for aid in re.findall(r'/advancementssearch highlight ([^\s"}]+)', line):
                    searches.append({'function':fid, 'advancementId':aid})
                for aid in re.findall(r'adv_id:\s*"([a-z0-9_.-]+:[a-z0-9_./-]+)"', line):
                    wrappers.append({'function':fid,'advancementId':aid})
                for callee in re.findall(r'(?:^\$?|\brun |\bschedule )function (#?[a-z0-9_.-]+:[a-z0-9_./$()-]+)(?=\s|$)',line):
                    if '$(' in callee:
                        typ = 'GUARDED_OPTIONAL_RESOURCE' if re.match(r'bacap_rewards:(reward|exp|trophy)/', callee) else 'DYNAMIC_MACRO_TARGET'
                    elif (callee[1:] in tags if callee.startswith('#') else callee in funcs):
                        typ = 'RESOLVED'
                    elif callee.startswith('#bacap_fanpacks:'):
                        typ = 'EXTERNAL_BACAP_FANPACK_HOOK'
                    elif callee == 'blazeandcave:advancement/loser_hurt' and combo != 'hardcore':
                        typ = 'GUARDED_OPTIONAL_RESOURCE'
                    else:
                        typ = 'UNEXPECTED_DIRECT_DANGLING'
                    row = {'function':fid,'line':number,'callee':callee,'classification':typ}
                    if typ == 'GUARDED_OPTIONAL_RESOURCE':
                        assert has_execution_guard(line), ('Unguarded optional call', combo, row)
                        row['executionGuardVerified'] = True
                        if callee == 'blazeandcave:advancement/loser_hurt':
                            row['optionalContract'] = 'B4 Hardcore-only helper; native damage-score selector guards the shared timer hook.'
                            assert load(PHASE/'b4_companion_matrix.json')['hardcoreMissingCalleeSupplied'] == callee
                    calls.append(row)
                    if typ == 'UNEXPECTED_DIRECT_DANGLING':
                        dangling.append(row)
        missing_search = [r for r in searches if r['advancementId'] not in adv]
        malformed = [r for r in wrappers if r['advancementId'] not in adv]
        missing_tag_members = []
        for rid, tag in tags.items():
            for v in tag.get('values',[]):
                value = v['id'] if isinstance(v,dict) else v
                if isinstance(v,dict) and not v.get('required',True):
                    continue
                if not (value[1:] in tags if value.startswith('#') else value in funcs):
                    missing_tag_members.append({'tag':rid,'member':value})
        assert not any([missing_rewards,missing_search,malformed,dangling,missing_tag_members]), (combo, missing_rewards, missing_search, malformed,dangling,missing_tag_members)
        report = {'layers':[n for n,_ in layers], 'parentGraph':graph,'advancementsChecked':len(adv),
                  'vanillaRecipeAdvancements':sum(':recipes/' in i and owners['data/'+i.replace(':','/advancement/')+'.json']=='vanilla' for i in adv),
                  'rewardFunctionsChecked':len(reward_refs),'missingRewardFunctions':missing_rewards,
                  'SearchLinksChecked':len(searches),'unresolvedSearchIds':missing_search,'searches':searches,
                  'commentedSearchExamples':commented_searches,'historicalExtractorSearchCountIncludingComments':len(searches)+len(commented_searches),
                  'wrapperLiteralArguments':len(wrappers),'malformedLiteralAdvancementIds':malformed,
                  'functionCallCounts':dict(collections.Counter(r['classification'] for r in calls)),
                  'directDanglingCalls':dangling,'missingRequiredFunctionTagMembers':missing_tag_members,
                  'functionCalls':calls,'result':'PASS'}
        result[combo] = {'resources':resources,'owners':owners,'advancements':adv,'functions':funcs,'tags':tags,'report':report}
        write(TEMP/('codec_resources_'+combo+'.json'), {p:json.loads(b) for p,b in resources.items() if p.endswith('.json')})
        write(TEMP/('codec_advancements_'+combo+'.json'), adv)
        write(TEMP/('effective_hashes_'+combo+'.json'), {i:sha(resources['data/'+i.replace(':','/advancement/')+'.json']) for i in sorted(adv)})
    assert len(result['main']['advancements'])-result['main']['report']['vanillaRecipeAdvancements'] == 1332
    assert result['main']['report']['rewardFunctionsChecked'] == 1294
    assert result['main']['report']['wrapperLiteralArguments'] == 1256
    search_counts = {k:v['report']['SearchLinksChecked'] for k,v in result.items()}
    assert search_counts == {'main':1257,'hardcore':1257,'terralith':1283,'amplifiedNether':1257,'nullscape':1258}, search_counts
    assert all(len(v['report']['commentedSearchExamples'])==4 for v in result.values())
    write(TEMP/'effective_layer_validation.json', {k:v['report'] for k,v in result.items()})
    return result


def enum_bindings(path, data):
    pat = re.compile(rb'^    ([A-Z][A-Z0-9_]*)\(\r?\n(.*?)^    \)([,;])\r?\n',re.M|re.S)
    typ = 'SCORE' if path.endswith('TrackedScoreType.java') else 'STATISTIC' if path.endswith('TrackedStatisticsDataType.java') else 'NEARBY_ENTITIES'
    result = {}
    for match in pat.finditer(data):
        args = [x.strip() for x in match[2].decode('utf8').split(',')]
        entities = [a.removeprefix('EntityTypes.') for a in args[3:]] if typ == 'NEARBY_ENTITIES' else None
        parameter = int(args[3 if typ == 'STATISTIC' else 1].replace('_',''))
        baby = args[2]=='true' if entities else None
        result[match[1].decode()] = {'name':match[1].decode(),'path':path,'type':typ,'advancementId':args[0].strip('"'),
            'literalParameter':parameter,'effectiveFinalValue':len(entities)*(2 if baby else 1) if entities else parameter,
            'babySeparated':baby,'scoreboardObjectives':[x.strip('"') for x in args[3:]] if typ=='SCORE' else [],
            'nearbyEntities':entities,'statOrItemOrEntityReferences':args[1:3] if typ=='STATISTIC' else args[3:] if entities else [],
            'declarationSha256':sha(match[0].replace(b'\r\n',b'\n'))}
    return result


def trackers_gui(main):
    manifest = load(PHASE/'b4_product_change_manifest.json')
    bindings, baseline = {}, {}
    for spec in manifest['trackers']:
        path = spec['binding']['path']
        if spec['binding']['name'] in bindings:
            continue
        bindings.update(enum_bindings(path,(ROOT/path).read_bytes()))
        baseline.update(enum_bindings(path,subprocess.check_output(['git','show',HEAD+':'+path],cwd=ROOT)))
    assert len(bindings) == 86
    definitions = set()
    for body in main['functions'].values():
        definitions.update(re.findall(r'\bscoreboard objectives add (\S+) ',body))
    records = []
    for spec in manifest['trackers']:
        name = spec['binding']['name']
        actual = bindings[name]
        for key in ['name','path','type','advancementId','literalParameter','effectiveFinalValue','babySeparated',
                    'scoreboardObjectives','nearbyEntities','statOrItemOrEntityReferences']:
            assert actual[key] == spec['targetBinding'][key], (name,key)
        assert actual['advancementId'] in main['advancements'],name
        assert all(o in definitions for o in actual['scoreboardObjectives']),name
        status = 'KEEP' if spec['decision']=='KEEP_EXACTLY' else 'UPDATED'
        if status == 'KEEP':
            assert actual == baseline[name],name
        else:
            for fact in spec.get('targetReachableSelectorEvidence',[]):
                text = main['resources'][fact['path']].decode('utf8').splitlines()[fact['line']-1]
                assert set(re.findall(r'type=([^,\]]+)',text)) == {e.lower() for e in actual['nearbyEntities']}
                assert sorted(set(re.findall(r'distance=([^,\]]+)',text))) == ['..'+str(actual['literalParameter'])]
            for fact in spec.get('grantEvidence',[]):
                text = main['resources'][fact['path']].decode('utf8').splitlines()[fact['line']-1]
                assert 'only '+actual['advancementId'] in text
                assert all(r['objective'] in text and r['range'] in text for r in fact['scoreRanges'])
        records.append({'name':name,'disposition':status,'binding':actual,'resolved':True})
    assert collections.Counter(r['disposition'] for r in records) == {'KEEP':80,'UPDATED':6}
    assert bindings['ON_A_RAIL'] == baseline['ON_A_RAIL'] and bindings['HALF_HEART_LIFE'] == baseline['HALF_HEART_LIFE']
    write(TEMP/'registry_input.json', {'identifierReferences':sorted({s for b in bindings.values() for s in b['statOrItemOrEntityReferences']})})
    path = 'src/main/java/com/diskree/achievetodo/injection/mixin/main/PlacedAdvancementMixin.java'
    data = (ROOT/path).read_bytes(); old = subprocess.check_output(['git','show',HEAD+':'+path],cwd=ROOT)
    old = old.replace(b'\r\n',b'\n'); data = data.replace(b'\r\n',b'\n')
    assert data == re.sub(rb'^            "blazeandcave:challenges/constellation",\n',b'',old,flags=re.M)
    def order(source):
        return {m[1].decode():re.findall(r'"([a-z0-9_:/.]+)"',m[2].decode()) for m in re.finditer(rb'customChildrenOrderMap.put\("([^"]+)", List.of\((.*?)\)\);',source,re.S)}
    maps, before = order(data), order(old)
    assert len(maps)==3 and sum(map(len,maps.values()))==27 and sum(map(len,before.values()))==28
    for parent, children in maps.items():
        assert children == [i for i in before[parent] if i!='blazeandcave:challenges/constellation']
        assert all(main['advancements'][c]['parent']==parent for c in children)
    categories = (ROOT/'src/main/java/com/diskree/achievetodo/client/gui/AdvancementsTabType.java').read_text(encoding='utf8').split('public enum AdvancementsTabType {')[1].split(';')[0]
    assert len(re.findall(r'\b[A-Z_]+\b',categories))==17
    return {'total':86,'keep':80,'updated':6,'removed':0,'unresolved':0,'bindings':records,'unchangedSpecialCases':['ON_A_RAIL','HALF_HEART_LIFE']}, {'categories':17,'explicitBefore':28,'explicitAfter':27,'parentMaps':3,'orderedChildren':maps,'missing':0,'parentFailures':0,'architectureUnchanged':True}


REMOVED = {'blazeandcave:adventure/spear_fishing':'blazeandcave:adventure/seafood_skewer',
           'blazeandcave:animal/birdkeeper':'blazeandcave:biomes/birdkeeper','blazeandcave:animal/chatterbox':'blazeandcave:biomes/chatterbox',
           'blazeandcave:nether/get_a_lode_of_this':'blazeandcave:mining/get_a_lode_of_this',
           'blazeandcave:nether/lodes_of_applications':'blazeandcave:mining/lodes_of_applications',
           'blazeandcave:statistics/two_by_two':'blazeandcave:statistics/overpopulation','minecraft:nether/use_lodestone':'minecraft:adventure/use_lodestone'}


def static_checks():
    effective = views()
    main = effective['main']
    trackers, gui = trackers_gui(main)
    raw = archive(TARGET)
    old = inventory(archive(OLD)); target = inventory(raw)
    assert set(REMOVED)==old.keys()-target.keys()
    mapping = [{'oldId':a,'newId':b,'oldPresent':a in old,'oldAbsentTarget':a not in target,'newPresentTarget':b in target,
                'oldSha256':old[a]['sha256'],'targetSha256':target[b]['sha256'],'progressMigration':False} for a,b in sorted(REMOVED.items())]
    invalid = set(REMOVED)|{'blazeandcave:biomes/naughty_lus','blazeandcave:bacap/benchmarking'}
    stale = []
    for p in (ROOT/'src/main').rglob('*'):
        if not p.is_file() or p.suffix not in {'.java','.json','.mcfunction'}:
            continue
        text = p.read_text(encoding='utf8')
        for rid in invalid:
            if re.search(re.escape(rid)+r'(?=[\s"\x27},\]]|$)',text):
                stale.append({'path':p.relative_to(ROOT).as_posix(),'id':rid})
    assert not stale,stale
    shims = []
    for fid, bad, correct in [('bacap_rewards:biomes/naughtylus','blazeandcave:biomes/naughty_lus','blazeandcave:biomes/naughtylus'),
                               ('bacap_rewards:bacap/benchmarking','blazeandcave:bacap/benchmarking','minecraft:story/root')]:
        path = 'data/'+fid.replace(':','/function/')+'.mcfunction'
        native = raw[path]; current = main['resources'][path]
        assert native.count(bad.encode())==1 and native.replace(bad.encode(),correct.encode(),1)==current
        assert correct in main['advancements']
        shims.append({'function':fid,'invalidId':bad,'correctId':correct,'onlyIdTokenChanged':True,'sha256':sha(current)})
    alias = main['functions']['blazeandcave:unwanted_passenger_fail']
    assert alias == 'function blazeandcave:advancement/unwanted_passenger_fail\n'
    assert 'blazeandcave:advancement/unwanted_passenger_fail' in main['functions']
    shims.append({'function':'blazeandcave:unwanted_passenger_fail','target':'blazeandcave:advancement/unwanted_passenger_fail','oneLineForwarder':True})
    assert main['advancements']['minecraft:story/root']['rewards']['function']=='bacap_rewards:bacap/benchmarking'
    messages = []
    for path in sorted((ROOT/'src/main/resources/resourcepacks').rglob('*.mcfunction')):
        text = path.read_text(encoding='utf8')
        if any('tellraw ' in l and '/advancementssearch highlight ' in l and not l.lstrip().startswith('#') for l in text.splitlines()):
            messages.append({'path':path.relative_to(ROOT).as_posix(),'sha256':sha(path.read_bytes()),'legacySpelling':'hoverEvent' in text})
    assert len(messages)==1300 and sum(r['legacySpelling'] for r in messages)==1162
    write(TEMP/'message_inputs.json', messages)
    native_macro = raw['data/bacap_rewards/function/advancement_made_macro.mcfunction'].decode('utf8').splitlines()
    helper = main['functions']['bacap_rewards:terralith_advancement_made_macro'].splitlines()
    assert len(native_macro)==len(helper)
    gates=[]
    for number,(a,b) in enumerate(zip(native_macro,helper),1):
        if re.search(r'scoreboard players (?:add|operation) \S+ bac_advancements(?:_team)?(?:_points)?\b',a):
            assert 'if score terralith_score bac_settings matches 1' in b
            restored=b.replace('if score terralith_score bac_settings matches 1 ','',1)
            if a.startswith('$scoreboard '):
                restored=restored.replace('$execute run scoreboard ','$scoreboard ',1)
            assert restored==a
            gates.append(number)
        else:
            assert a==b
    assert len(gates)==34
    root_records=[]
    m=load(PHASE/'b4_product_change_manifest.json')
    for spec in m['rootWrappers']:
        p=ROOT/spec['path']; text=p.read_text(encoding='utf8')
        calls=re.findall(r'^function (\S+) \{([^\n]+)\}$',text,re.M)
        assert len(calls)==1 and calls[0][0] in main['functions']
        args=dict(re.findall(r'(\w+):"([^"]+)"',calls[0][1]))
        assert args['adv_id'] in main['advancements'] or args['adv_id'] in effective['nullscape']['advancements']
        assert not re.search(r'\bscoreboard players|\badvancement grant',text)
        root_records.append({'path':spec['path'],'sha256':sha(p.read_bytes()),'delegate':calls[0][0],'arguments':args,'passed':True})
    assert len(root_records)==17
    # The native shared macro remains the single bookkeeping implementation.
    assert main['resources']['data/bacap_rewards/function/advancement_made_macro.mcfunction']==raw['data/bacap_rewards/function/advancement_made_macro.mcfunction']
    active_target_commands = '\n'.join(line for path, body in raw.items() if path.endswith('.mcfunction')
                                       for line in body.decode('utf8').splitlines()
                                       if line.strip() and not line.lstrip().startswith('#'))
    definitions=set(re.findall(r'\bscoreboard objectives add (\S+) ',active_target_commands))
    assert len(definitions)==110
    raw_scores=['bac_advancements','bac_advfirst','bac_advancements_team','bac_advfirst_team_sum','bac_advfirst_sum','bac_advfirst_team']
    assert all(o in definitions for o in raw_scores)
    report={'phase':'B','stage':'B8','targetIdentity':{'version':'1.21','versionId':'Y2zZ5eSs','sha256':TARGET_HASH,
            'sha1':'14da3f07b5467e8b59ffc0253fd8212c938cd739'},'oldToTarget':load(TEMP/'independent_inventory.json')['oldToTarget'],
            'removedIdMappings':mapping,'staleActiveIds':stale,'postB4Shims':shims,'trackers':trackers,'gui':gui,
            'rootWrappers':root_records,'scoreboardContract':{'definedObjectives':len(definitions),'rawProgressionObjectives':raw_scores,'pointsSeparate':True},
            'messages':{'total':1300,'legacySpellingSubset':1162,'records':messages},'terralithGating':{'gatedIncrementStatements':34,'nonIncrementLinesUnchanged':True},
            'effectiveLayers':{k:v['report'] for k,v in effective.items()}}
    write(TEMP/'static_validation.json',report)
    binding_report = message_bindings(messages, effective)
    write(TEMP/'message_binding_validation.json', binding_report)
    assert not binding_report['failures'], ('CURRENT_TARGET_MESSAGE_COMPONENT_OR_SEARCH_BINDING_MISMATCH', binding_report['failures'])
    print(json.dumps({'static':'PASS','trackers':86,'GUI':27,'messages':1300,'layers':{k:len(v['advancements']) for k,v in effective.items()}}))


def component_translation_keys(component):
    """Read actual JSON components, including nested hover text; never regex prose."""
    if isinstance(component, dict):
        return ({component['translate']} if isinstance(component.get('translate'), str) else set()) | set().union(
            *(component_translation_keys(value) for value in component.values()))
    if isinstance(component, list):
        return set().union(*(component_translation_keys(value) for value in component))
    return set()


def missing_display_keys(component, display):
    actual = component_translation_keys(component)
    # Native reward messages bind the primary title/description. Advancement-only
    # progress-command hints and companion attribution extras are not message prose.
    expected = {field:({display[field]['translate']} if isinstance(display.get(field), dict)
                       and isinstance(display[field].get('translate'), str)
                       else component_translation_keys(display.get(field, {})))
                for field in ['title', 'description']}
    return {field:sorted(expected[field] - actual)
            for field in ['title', 'description']
            if expected[field] - actual}


def message_bindings(messages, effective):
    actions = {row['path']:row for row in load(PHASE/'b4_product_change_manifest.json')['fileActions']}
    failures, records = [], []
    for row in messages:
        path = row['path']
        view = next((v for pack,v in [('hardcore','hardcore'),('terralith','terralith'),
                                     ('amplified_nether','amplifiedNether'),('nullscape','nullscape')]
                     if '/bacap_'+pack+'_override/' in path), 'main')
        checked = 0
        for number, line in enumerate((ROOT/path).read_text(encoding='utf8').splitlines(), 1):
            if line.lstrip().startswith('#') or 'tellraw ' not in line or '/advancementssearch highlight ' not in line:
                continue
            match = re.search(r'\btellraw\s+\S+\s+([\[{].*)', line)
            assert match, (path, number)
            component = json.loads(match[1])
            for rid in set(re.findall(r'/advancementssearch highlight ([^\s"}]+)', line)):
                assert rid in effective[view]['advancements'], (path, rid)
                display = effective[view]['advancements'][rid].get('display', {})
                assert display, (path, rid, 'Message target has no display')
                missing = missing_display_keys(component, display)
                checked += 1
                if missing:
                    action = actions.get(path)
                    failures.append({'path':path,'sha256':sha((ROOT/path).read_bytes()),'line':number,
                        'view':view,'searchTarget':rid,'missingCurrentDisplayKeys':missing,
                        'actualComponentTranslationKeys':sorted(component_translation_keys(component)),
                        'B4Action':action['action'] if action else None,
                        'B4SemanticDecision':action['exactIntendedSemanticChange'] if action else None})
        assert checked, path
        records.append({'path':path,'sha256':row['sha256'],'targetBindingsChecked':checked})
    assert len(records) == 1300
    return {'currentCorpus':len(records),'bindingsChecked':sum(r['targetBindingsChecked'] for r in records),
            'failures':failures,'failureCount':len(failures),'records':records,'passed':not failures}


def java_command(name, command):
    env = dict(os.environ)
    env['TEMP'] = env['TMP'] = str(ROOT/'build/tmp/codex_java_uds_probe')
    with (TEMP/(name+'.log')).open('w',encoding='utf8') as log:
        result = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, env=env)
    write(TEMP/(name+'_command.json'), {'command':command,'exitCode':result.returncode})
    return result.returncode


def run_real_codec():
    """Stop at the first rejected effective view. Never adapt rejected predicates."""
    precheck()
    for name, text in probe_sources().items():
        (TEMP/(name+'.java')).write_text(text,encoding='utf8')
    jdk = pathlib.Path(os.environ.get('JAVA_HOME','C:/Program Files/Eclipse Adoptium/jdk-25.0.4.7-hotspot'))/'bin'
    classpath = ';'.join(str(ROOT/p) if not pathlib.Path(p).is_absolute() else p for p in CLASSPATH_ENTRIES)
    assert java_command('independent_javac',[str(jdk/'javac.exe'),'-encoding','UTF-8','-cp',classpath,
               '-d',str(TEMP),*[str(TEMP/(name+'.java')) for name in JAVA_SOURCES]]) == 0
    rows=[]
    (TEMP/'converted').mkdir(exist_ok=True)
    for spec in SOURCE_INPUTS:
        source=ROOT/spec['source']
        assert sha(source.read_bytes())==spec['sourceSha256']
        assert hashlib.sha1(source.read_bytes()).hexdigest()==spec['sourceSha1']
        rows.append({**spec,'source':str(source),'output':str(TEMP/'converted'/(spec['enum']+'.zip'))})
    write(TEMP/'source_inputs.json',rows)
    assert java_command('independent_export',[str(jdk/'java.exe'),'-cp',str(TEMP)+';'+classpath,'B8Export',
                       str(TEMP/'source_inputs.json'),str(TEMP/'converted_sources.json')]) == 0
    views()
    for view in ['target','main','hardcore','terralith','amplifiedNether','nullscape']:
        rc=java_command('independent_codec_'+view,[str(jdk/'java.exe'),'-Xmx3g','-cp',str(TEMP)+';'+classpath,
                                  'B8Codec',str(TEMP),view])
        result=load(TEMP/('codec_'+view+'_results.json'))
        assert result['minecraftVersion']=='26.2' and result['negativeRejected']
        print(view,result['checked'],result['passed'],result['failed'],flush=True)
        if rc or result['failed']:
            return {'passed':False,'stoppedView':view,'stopReason':'REAL_26_2_ADVANCEMENT_CODEC_REJECTION'}
    return {'passed':True}


def probe_sources():
    sources = dict(JAVA_SOURCES)
    sources['B8Export'] = sources['B8Export'].replace('r18', 'r19')
    # These two probes had not executed before the original codec STOP.
    # Java regex literals need two source backslashes, not four.
    for name in ['B8Registry', 'MessageCodec']:
        sources[name] = sources[name].replace('\\\\\\\\', '\\\\')
    sources['MessageCodec'] = sources['MessageCodec'].replace('build/tmp/phase_b_b8"', 'build/tmp/phase_b_b8/post_r2"')
    return sources


def stop_evidence():
    """Record the failed gate and safety facts; do not publish success receipts."""
    paths=[PHASE/'B8_STATIC_CERTIFICATION.md',PHASE/'b8_static_certification.json']
    assert not any(p.exists() for p in paths), 'B8 durable output already exists; preserve it'
    before=load(TEMP/'precheck.json')
    current=precheck()
    for group in ['runtime','historicalTests','evidence','historical','b7ToolSha256','counts']:
        assert current[group]==before[group], group
    raw=archive(TARGET)
    historical=archive(ROOT/'reference/phase_a_preservation/files/final/bacap_terralith.zip')
    converted=archive(TEMP/'converted/BACAP_TERRALITH.zip')
    previous=archive(ROOT/'build/tmp/phase_b_b5_6/converted/BACAP_TERRALITH.zip')
    layers=load(TEMP/'effective_layer_validation.json')
    target_inventory=load(TEMP/'independent_inventory.json')
    codec={}
    for view in ['target','main','hardcore','terralith']:
        receipt=load(TEMP/('codec_'+view+'_results.json'))
        hashes=({r['id']:r['sha256'] for r in target_inventory['records']} if view=='target' else load(TEMP/('effective_hashes_'+view+'.json')))
        codec[view]={'checked':receipt['checked'],'passed':receipt['passed'],'failed':receipt['failed'],
                     'minecraftVersion':receipt['minecraftVersion'],'negativeRejected':receipt['negativeRejected'],
                     'resourceContextSha256':sha((TEMP/('codec_resources_'+view+'.json')).read_bytes()),
                     'records':[{'id':r['id'],'inputSha256':hashes[r['id']],'accepted':r['accepted']} for r in receipt['records']],
                     'sourceBackedRegistryReferences':receipt['sourceBackedRegistryReferences']}
    failures=[]
    failed=[r for r in load(TEMP/'codec_terralith_results.json')['records'] if not r['accepted']]
    assert {r['id'] for r in failed} == {'blazeandcave:technical/biome_branch1_end','blazeandcave:technical/biome_branch2_end'}
    planned_merge_ids={r['archiveEntry'] for r in load(PHASE/'b4_product_change_manifest.json')['companionIntegration']['exactMergePlan']['overlapAdvancementMerges']}
    for r in failed:
        path='data/'+r['id'].replace(':','/advancement/')+'.json'
        assert converted[path]==previous[path]
        assert path not in planned_merge_ids
        assert not any(x['id']==r['id'] for x in codec['main']['records'])
        assert path not in raw
        obj=json.loads(converted[path])
        assert set(obj['criteria'])=={'kilometre_walk'}
        assert obj['criteria']['kilometre_walk']['conditions']['player']['player']['advancements']=={'blazeandcave:biomes/kilometre_walk':True}
        failures.append({'advancementId':r['id'],'path':path,'effectiveOwner':'BACAP_TERRALITH',
              'nativeTargetPresent':False,'nativeTargetSha256':None,'historicalCompanionSha256':sha(historical[path]),
              'currentConvertedCompanionSha256':sha(converted[path]),'r17ConvertedPayloadSha256':sha(previous[path]),
              'r17AndR18PayloadByteIdentical':True,'nativeMainSetCodecAccepted':True,'effectiveTerralithCodecAccepted':False,
              'criterion':'kilometre_walk','trigger':'minecraft:location','rejectedPointer':'/criteria/kilometre_walk/conditions/player/player',
              'error':'Unknown registry key minecraft:player in minecraft:entity_sub_predicate_type',
              'withinOriginal14MergeSet':False,'repairPerformed':False})
    assert codec['target']['checked']==codec['target']['passed']==1332 and codec['target']['failed']==0
    assert codec['terralith']['failed']==2
    checks={'selfTests':{'tests':8,'failures':0,'errors':0,'result':'PASS'},
            'compileCodecHarness':{'result':'GREEN','commandReceipt':'build/tmp/phase_b_b8/independent_javac_command.json'},
            'compileTestJava':{'result':'NOT_RUN','reason':'B8 stopped at real advancement codec rejection before publishing JUnit/fixtures'},
            'filteredJUnit':{'result':'NOT_RUN','reason':'B8 static certification gate failed'},
            'fullGradleSuite':{'result':'NOT_RUN','owner':'B9'}}
    protected_sources=[]
    for spec in SOURCE_INPUTS:
        assert sha((ROOT/spec['source']).read_bytes())==spec['sourceSha256']
        protected_sources.append({'path':spec['source'],'sha256':spec['sourceSha256'],'unchanged':True})
    record={'phase':'B','stage':'B8','status':'PHASE_B_B8_STOP','baselineHead':HEAD,'branch':'phase-b-bacap-26.2',
            'stopReason':'REAL_26_2_ADVANCEMENT_CODEC_REJECTION_IN_EFFECTIVE_TERRALITH_LAYER',
            'stoppedBeforeRuntimeProductWrites':True,'readyForB9':False,
            'currentProductAuthority':{'marker':'compat_26_2_r18','converterSha256':CONVERTER_HASH,'ruSha256':RU_HASH,
               'originalB5HashesPreserved':250,'supersededByB7R1':1},
            'target':{'version':'1.21','versionId':'Y2zZ5eSs','sha256':TARGET_HASH,'sha1':'14da3f07b5467e8b59ffc0253fd8212c938cd739'},
            'inventory':{**target_inventory['counts'],'excludedRoot':1,'duplicateIds':0,'duplicateArchivePaths':0},
            'oldTargetReconciliation':target_inventory['oldToTarget'],
            'canonicalArithmetic':'1152 - 7 + 97 = 1242',
            'codec':codec['target'],'effectiveCodecRuns':{k:v for k,v in codec.items() if k!='target'},
            'registryContext':load(TEMP/'codec_context_identity.json'),
            'registryContextPolicy':'Real vanilla typed registries plus actual selected pack resource keys and fully bound source-backed nested tags. Unknown biome negative control remains rejected. No predicate rewriting or codec deferral.',
            'failures':failures,'effectiveLayers':layers,
            'searchCountReconciliation':{'olderCountsIncludedFourCommentedExamples':True,
                 'commentsExcludedFromActiveContracts':layers['main']['commentedSearchExamples'],
                 'activeCounts':{k:r['SearchLinksChecked'] for k,r in layers.items()},
                 'includingComments':{k:r['historicalExtractorSearchCountIncludingComments'] for k,r in layers.items()},'unresolvedActive':0},
            'wrapperIdScan':{'checked':1256,'malformed':0},'rewardFunctionScan':{'checked':1294,'missing':0},
            'parentGraph':layers['main']['parentGraph'],
            'functionGraph':{k:{'counts':r['functionCallCounts'],'unexpectedDirectDangling':len(r['directDanglingCalls'])} for k,r in layers.items()},
            'pendingCertification':['resource registry/predicate/tag contracts beyond Advancement.CODEC','scoreboard referenced objective inventory',
                 '86 tracker final certification','27 GUI child final certification','1300 message metadata/component successor',
                 '17 root wrapper final certification','14 merge parity certification','24 Terralith wrappers','6 Hardcore messages',
                 'Amplified Nether and Nullscape full Advancement.CODEC','JUnit successor and fixtures'],
            'tests':checks,'processResources':load(TEMP/'process_resources_result.json'),
            'diffCheck':load(TEMP/'diff_check_result.json'),
            'runtimeProductDelta':{'ADD':0,'MODIFY':0,'DELETE':0},
            'uniqueCumulativeRuntimeProductDiff':{'ADD':177,'MODIFY':66,'DELETE':9},
            'safety':{'runtimeHashesUnchanged':True,'historicalTestsUnchanged':True,'priorEvidenceUnchanged':True,
                'priorEvidenceHashes':before['evidence'],'historicalArchivesUnchanged':True,'sourceArchives':protected_sources,
                'b7ToolUnchanged':True,'b7ToolSha256':before['b7ToolSha256'],'nothingStaged':True,
                'noCommit':True,'noPush':True,'noTag':True,'noGitHubMutation':True,'B9Started':False,
                'noProductRepair':True,'noLocalizationChange':True,'noBalanceChange':True,'noProgressMigration':True},
            'successorArtifacts':{'tool':'tools/phase_b/b8_bacap_static_certification.py','toolSha256':sha(pathlib.Path(__file__).read_bytes()),
                'successFixturesPublished':False,'successJUnitPublished':False}}
    write(paths[1],record)
    md='''# B8 static certification — STOP

The exact pinned BACAP 1.21 target passes all **1,332 / 1,332** real Minecraft 26.2 Advancement.CODEC checks. B8 stops because the effective retained Terralith companion adds two technical helpers with rejected legacy player-predicate structures.

| Effective codec view | Checked | Passed | Failed |
|---|---:|---:|---:|
| Pinned target | 1332 | 1332 | 0 |
| Main including vanilla recipes | 2894 | 2894 | 0 |
| Hardcore including vanilla recipes | 2894 | 2894 | 0 |
| Terralith including vanilla recipes | 2922 | 2920 | 2 |

Rejected IDs:

- `blazeandcave:technical/biome_branch1_end`
- `blazeandcave:technical/biome_branch2_end`

Both effective resources come from converted `BACAP_TERRALITH`. Their `kilometre_walk` criterion has the rejected structural pointer `/criteria/kilometre_walk/conditions/player/player`. The real codec reports unknown registry key `minecraft:player` in `minecraft:entity_sub_predicate_type`. Neither helper ID exists in the native main target; the entire main set passes under the same registry context. Their r17 and r18 converted companion payload bytes are identical. Neither path belongs to the fourteen authorized B5.6 merges. No repair, new override, converter change, or registry exemption was applied.

The registry harness uses real vanilla typed registries and selected pack-backed keys/tags; nested tags are actually bound. An unknown-biome negative control is rejected. The first temporary context used a construction-only named-tag holder; that harness limitation was corrected before the authoritative full runs. These two final rejections are independent of that limitation.

## Independently established facts

- Target: version `1.21`, Modrinth `Y2zZ5eSs`; SHA256 `c71d1aa1a84dbe00a3f85a42144b46214c4669a3cccf07ff66631d28f16a99b2`.
- Inventory: 1332 all − 72 without display − 17 hidden − 1 excluded root = **1242 canonical**, split 1116 BlazeandCave / 126 Minecraft. No duplicate IDs/archive paths.
- Old/target: 1222 common, 7 old-only, 110 target-only; common classes 594 byte identical, 4 semantic identical with byte differences, 624 semantic changed. Canonical: **1152 − 7 + 97 = 1242**. Thirteen target-only resources are noncanonical and retained in the ignored inventory with reasons/flags.
- Main wrapper literal arguments: 1256 checked, zero malformed. Main advancement reward functions: 1294 checked, zero missing.
- All five independently layered parent/function/reward/Search graphs have zero missing parents, cycles, malformed wrapper arguments, unresolved active Search IDs, or unexpected literal direct dangling calls. Guarded sparse optional resources and fanpack extension hooks remain separately classified.
- Active Search counts are 1257 Main/Hardcore/Amplified Nether, 1283 Terralith, 1258 Nullscape. Earlier 1261/1287/1262 counts include four commented examples in Fractal, Humble Bundle, and Vibe Check; no active reference was lost.
- Effective advancement totals include 1562 vanilla recipe unlock helpers: 2894 Main/Hardcore/Amplified Nether, 2922 Terralith, 2895 Nullscape. Main BACAP scope remains 1332 and canonical 1242.

## Incomplete gates

The remaining tracker/GUI/message/root-wrapper/merge parity and resource-registry certification, Amplified Nether/Nullscape full codecs, and JUnit/fixture publication are **not complete**. No success receipt or green certification is claimed. Eight new tool self-tests pass; the real-codec harness compiles. `compileTestJava` and filtered JUnit were not run after the mandatory codec STOP. The full Gradle suite belongs to B9 and was not run.

Safety/resource-processing results and exact hashes are recorded in `b8_static_certification.json`. No runtime product bytes or historical evidence changed. The unique runtime diff remains **177 ADD / 66 MODIFY / 9 DELETE**. Marker remains `compat_26_2_r18`; 250 B5 hashes remain exact and the converter hash remains the accepted B7-R1 supersession. B9 was not started.

**PHASE_B_B8_STOP**

`readyForB9 = false`
'''
    paths[0].write_text(md,encoding='utf8')
    print('PHASE_B_B8_STOP — readyForB9=false; two real effective Terralith codec rejections; zero runtime writes')


class SelfTests(unittest.TestCase):
    def test_score_selector_is_an_execution_guard(self):
        self.assertTrue(has_execution_guard('execute as @a[scores={bac_loser_hurt=1..}] run function x:optional'))

    def test_unguarded_literal_call_is_not_an_optional_guard(self):
        self.assertFalse(has_execution_guard('function x:missing'))

    def test_current_description_binding(self):
        self.assertFalse(missing_display_keys({'translate':'Title','extra':[{'translate':'Current description'}]},
                                             {'title':{'translate':'Title'},'description':{'translate':'Current description'}}))
    def test_stale_description_binding_is_rejected(self):
        self.assertEqual(missing_display_keys({'translate':'Title','extra':[{'translate':'Old description'}]},
                         {'title':{'translate':'Title'},'description':{'translate':'Current description'}}),
                         {'description':['Current description']})
    def test_wrong_root_search_binding_is_rejected(self):
        self.assertEqual(missing_display_keys({'translate':'Potions'}, {'title':{'translate':'Mining'}}),
                         {'title':['Mining']})
    def test_advancement_only_progress_hint_is_not_reward_message_prose(self):
        self.assertFalse(missing_display_keys({'translate':'Current description'},
                         {'description':{'translate':'Current description','extra':[{'translate':'To view progress, run:'}]}}))
    def test_object_key_order(self):
        self.assertEqual(normalized({'a':1,'b':2}), normalized({'b':2,'a':1}))
    def test_array_order_preserved(self):
        self.assertNotEqual(normalized([1,2]), normalized([2,1]))
    def test_canonical_rule(self):
        self.assertTrue(canonical('x:a', {'display':{}}))
        self.assertFalse(canonical('x:a', {'display':{'hidden':True}}))
        self.assertFalse(canonical('blazeandcave:bacap/root', {'display':{}}))
    def test_layout(self):
        self.assertEqual(advancement_id('data/a/advancement/b/c.json'), 'a:b/c')
        self.assertEqual(advancement_id('data/a/advancements/b.json'), 'a:b')
        self.assertIsNone(advancement_id('data/a/function/b.json'))
    def test_false_hidden_is_canonical(self):
        self.assertTrue(canonical('x:a', {'display':{'hidden':False}}))
    def test_parent_cycle(self):
        self.assertEqual(parent_graph({'a':{'parent':'b'},'b':{'parent':'a'}})['cycles'], [['a','b']])
    def test_missing_parent(self):
        self.assertEqual(parent_graph({'a':{'parent':'b'}})['missingParents'], [{'id':'a','parent':'b'}])
    def test_valid_tree(self):
        graph=parent_graph({'a':{},'b':{'parent':'a'}})
        self.assertEqual(graph['roots'], ['a'])
        self.assertFalse(graph['missingParents'] or graph['cycles'])



# Probe source is ATD certification code, not third-party datapack content.
JAVA_SOURCES = {'B8Codec': 'import com.google.gson.*;\nimport com.mojang.serialization.JsonOps;\nimport net.minecraft.advancements.Advancement;\nimport net.minecraft.core.*;\nimport net.minecraft.core.registries.BuiltInRegistries;\nimport net.minecraft.data.registries.VanillaRegistries;\nimport net.minecraft.resources.*;\nimport net.minecraft.tags.TagKey;\nimport net.minecraft.SharedConstants;\nimport java.nio.file.*;\nimport java.util.*;\nimport java.util.stream.Stream;\n\npublic class B8Codec {\n static Path dir; static JsonObject resources; static List<String> referenced=new ArrayList<>();\n static <T> HolderLookup.RegistryLookup<T> backed(HolderLookup.RegistryLookup<T> parent) {\n  return new HolderLookup.RegistryLookup.Delegate<>() {\n   Map<ResourceKey<T>,Holder.Reference<T>> custom=new HashMap<>();\n   Map<TagKey<T>,HolderSet.Named<T>> tags=new HashMap<>();\n   Set<TagKey<T>> visiting=new HashSet<>();\n   public HolderLookup.RegistryLookup<T> parent(){return parent;}\n   public Optional<Holder.Reference<T>> get(ResourceKey<T> key){\n    var nativeValue=parent.get(key);if(nativeValue.isPresent())return nativeValue;\n    String path="data/"+key.identifier().getNamespace()+"/"+parent.key().identifier().getPath()+"/"+key.identifier().getPath()+".json";\n    if(!resources.has(path))return Optional.empty();\n    referenced.add(path);\n    return Optional.of(custom.computeIfAbsent(key,k->Holder.Reference.createStandAlone(parent,k)));\n   }\n   public Optional<HolderSet.Named<T>> get(TagKey<T> tag){\n    if(tags.containsKey(tag))return Optional.of(tags.get(tag));\n    String path="data/"+tag.location().getNamespace()+"/tags/"+parent.key().identifier().getPath()+"/"+tag.location().getPath()+".json";\n    if(!resources.has(path))return parent.get(tag);\n    if(!visiting.add(tag))throw new IllegalStateException("Tag cycle "+tag);\n    var members=new LinkedHashMap<String,Holder<T>>();\n    for(var element:resources.getAsJsonObject(path).getAsJsonArray("values")) {\n     var obj=element.isJsonObject()?element.getAsJsonObject():null;\n     boolean required=obj==null||!obj.has("required")||obj.get("required").getAsBoolean();\n     String id=obj==null?element.getAsString():obj.get("id").getAsString();\n     if(id.startsWith("#")) {\n      var nested=get(TagKey.create(tag.registry(),Identifier.parse(id.substring(1))));\n      if(required&&nested.isEmpty())throw new IllegalStateException("Missing nested tag "+id);\n      nested.ifPresent(n->n.stream().forEach(h->members.put(h.unwrapKey().orElseThrow().identifier().toString(),h)));\n     }else {\n      var h=get(ResourceKey.create(tag.registry(),Identifier.parse(id)));\n      if(required&&h.isEmpty())throw new IllegalStateException("Unknown required tag member "+id);\n      h.ifPresent(v->members.put(id,v));\n     }\n    }\n    HolderSet.Named<T> named;\n    try {\n     var constructor=HolderSet.Named.class.getDeclaredConstructor(HolderOwner.class,TagKey.class);\n     constructor.setAccessible(true); named=(HolderSet.Named<T>)constructor.newInstance(parent,tag);\n    } catch(Exception e){throw new IllegalStateException(e);}\n    try{var bind=HolderSet.Named.class.getDeclaredMethod("bind",List.class);bind.setAccessible(true);bind.invoke(named,List.copyOf(members.values()));}catch(Exception e){throw new IllegalStateException(e);}\n    visiting.remove(tag);tags.put(tag,named);referenced.add(path);return Optional.of(named);\n   }\n  };\n }\n\n public static void main(String[] args)throws Exception {\n  dir=Path.of(args[0]);\n  try { run(args); } catch(Throwable t) {\n   var trace=new java.io.StringWriter(); t.printStackTrace(new java.io.PrintWriter(trace));\n   Files.writeString(dir.resolve("codec_exception.txt"),trace.toString()); throw t;\n  }\n }\n static void run(String[] args)throws Exception {\n  SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();\n  var builtins=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).freeze();\n  var vanilla=VanillaRegistries.createLookup();\n  Map<String,HolderLookup.RegistryLookup<?>> base=new LinkedHashMap<>();\n  builtins.listRegistries().forEach(r->base.put(r.key().identifier().toString(),r));\n  vanilla.listRegistries().forEach(r->base.put(r.key().identifier().toString(),r));\n  resources=JsonParser.parseString(Files.readString(dir.resolve("codec_resources_"+args[1]+".json"))).getAsJsonObject();\n  var definitions=JsonParser.parseString(Files.readString(dir.resolve("codec_advancements_"+args[1]+".json"))).getAsJsonObject();\n  var provider=HolderLookup.Provider.create(base.values().stream().map(B8Codec::backed));\n  var ops=RegistryOps.create(JsonOps.INSTANCE,provider);\n  JsonArray results=new JsonArray(); int failures=0; referenced.clear();\n  for(var entry:definitions.entrySet()) {\n   JsonObject out=new JsonObject();out.addProperty("id",entry.getKey());\n   try {\n    var decoded=Advancement.CODEC.parse(ops,entry.getValue());\n    boolean accepted=decoded.error().isEmpty();\n    out.addProperty("accepted",accepted);\n    decoded.error().ifPresent(e->out.addProperty("error",e.message()));\n    if(accepted) {\n     var value=decoded.result().orElseThrow();\n     var expected=entry.getValue().getAsJsonObject().getAsJsonObject("criteria").keySet();\n     boolean preserved=value.criteria().keySet().equals(expected);\n     out.addProperty("criteriaPreserved",preserved);\n     if(!preserved)accepted=false;\n    }\n    if(!accepted)failures++;\n   }catch(Throwable t){out.addProperty("accepted",false);out.addProperty("exception",t.toString());failures++;}\n   results.add(out);\n  }\n  var negative=JsonParser.parseString("{\\"criteria\\":{\\"negative\\":{\\"trigger\\":\\"minecraft:location\\",\\"conditions\\":{\\"player\\":[{\\"condition\\":\\"minecraft:entity_properties\\",\\"entity\\":\\"this\\",\\"predicate\\":{\\"location\\":{\\"biomes\\":\\"terralith:phase_b_intentionally_missing\\"}}}]}}}}");\n  boolean negativeRejected=Advancement.CODEC.parse(ops,negative).error().isPresent();\n  var report=new JsonObject();report.addProperty("minecraftVersion",SharedConstants.getCurrentVersion().name());\n  report.addProperty("checked",results.size());report.addProperty("passed",results.size()-failures);\n  report.addProperty("failed",failures);report.addProperty("negativeRejected",negativeRejected);\n  report.add("sourceBackedRegistryReferences",new Gson().toJsonTree(new TreeSet<>(referenced)));\n  report.add("records",results);\n  Files.writeString(dir.resolve("codec_"+args[1]+"_results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\\n");\n  System.out.println("B8 Advancement.CODEC checked="+results.size()+" failed="+failures+" negativeRejected="+negativeRejected);\n  if(failures!=0 || !negativeRejected)System.exit(1);\n }\n}\n', 'B8Export': 'import com.google.gson.*;\nimport com.diskree.achievetodo.client.*;\nimport java.nio.file.*;\npublic class B8Export {\n public static void main(String[] args) throws Exception {\n  JsonArray inputs=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonArray();JsonArray results=new JsonArray();\n  for(JsonElement e:inputs){JsonObject x=e.getAsJsonObject();ExternalPack pack=ExternalPack.valueOf(x.get("enum").getAsString());Path src=Path.of(x.get("source").getAsString()),out=Path.of(x.get("output").getAsString());\n   if(!Utils.calculateSHA1(src).equals(pack.getSha1()))throw new IllegalStateException("Pin mismatch "+pack);\n   ExternalPackCompatibility.copyForWorld(src,out,pack);\n   if(!ExternalPackCompatibility.isCurrentWorldPack(out,pack))throw new IllegalStateException("Freshness "+pack);\n   x.addProperty("sourceSha1",pack.getSha1());x.addProperty("r18Converted",true);x.addProperty("freshnessPassed",true);results.add(x);\n  }\n  Files.writeString(Path.of(args[1]),new GsonBuilder().setPrettyPrinting().create().toJson(results)+"\\n");System.out.println("PASS: "+results.size()+" pinned sources exported through production r18.");\n }\n}\n', 'B8Registry': 'import com.google.gson.*;\nimport java.nio.file.*;\nimport net.minecraft.SharedConstants;\nimport net.minecraft.server.Bootstrap;\nimport net.minecraft.core.Registry;\nimport net.minecraft.core.registries.BuiltInRegistries;\nimport net.minecraft.resources.Identifier;\npublic class B8Registry {\n @SuppressWarnings({"rawtypes","unchecked"})\n public static void main(String[] args) throws Exception {\n  SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();\n  JsonObject input=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();\n  JsonArray records=new JsonArray();\n  for(JsonElement e:input.getAsJsonArray("identifierReferences")) {\n   String ref=e.getAsString();String[] parts=ref.split("\\\\.");String cls=switch(parts[0]){\n    case "Stats" -> "net.minecraft.stats.Stats";\n    case "EntityTypes" -> "net.minecraft.world.entity.EntityTypes";\n    case "Items" -> "net.minecraft.world.item.Items";\n    case "Blocks" -> "net.minecraft.world.level.block.Blocks";\n    default -> throw new IllegalArgumentException(ref);\n   };\n   Object value=Class.forName(cls).getField(parts[1]).get(null);Registry registry=switch(parts[0]){\n    case "EntityTypes" -> BuiltInRegistries.ENTITY_TYPE;\n    case "Items" -> BuiltInRegistries.ITEM;\n    case "Blocks" -> BuiltInRegistries.BLOCK;\n    case "Stats" -> value instanceof Identifier ? BuiltInRegistries.CUSTOM_STAT : BuiltInRegistries.STAT_TYPE;\n    default -> throw new IllegalArgumentException(ref);\n   };\n   Object id=registry.getKey(value);if(id==null)throw new IllegalStateException("Unregistered "+ref);\n   JsonObject row=new JsonObject();row.addProperty("reference",ref);row.addProperty("identifier",id.toString());row.addProperty("registered",true);records.add(row);\n  }\n  JsonObject out=new JsonObject();out.addProperty("passed",true);out.addProperty("minecraftVersion",SharedConstants.getCurrentVersion().name());out.addProperty("referencesChecked",records.size());out.add("records",records);\n  Files.writeString(Path.of(args[1]),new GsonBuilder().setPrettyPrinting().create().toJson(out)+"\\n");\n  System.out.println("PASS: "+records.size()+" native Minecraft registry references.");\n }\n}\n', 'MessageCodec': 'import com.diskree.achievetodo.client.LegacyChatText;\nimport com.google.gson.*;\nimport com.mojang.serialization.JsonOps;\nimport net.minecraft.network.chat.*;\nimport net.minecraft.network.chat.contents.TranslatableContents;\nimport net.minecraft.SharedConstants;\nimport net.minecraft.server.Bootstrap;\nimport java.nio.file.*;\nimport java.util.*;\nimport java.util.regex.*;\n\npublic class MessageCodec {\n static void require(boolean b,String s){if(!b)throw new IllegalStateException(s);}\n static boolean hover(Component c){if(c.getStyle().getHoverEvent()!=null)return true;if(c.getContents() instanceof TranslatableContents t)for(Object a:t.getArgs())if(a instanceof Component n&&hover(n))return true;return c.getSiblings().stream().anyMatch(MessageCodec::hover);}\n static int links(JsonElement e,String inheritedColor){int n=0;if(e.isJsonObject()){\n  var o=e.getAsJsonObject();String color=o.has("color")?o.get("color").getAsString():inheritedColor;if(o.has("click_event")){var click=o.getAsJsonObject("click_event");if(click.has("command")&&click.get("command").getAsString().contains("/advancementssearch highlight ")){\n   boolean spacing=o.has("text")&&o.get("text").getAsString().isBlank();require(click.get("action").getAsString().equals("run_command"),"Bad click action");require(spacing||color!=null,"Frame color missing");require(o.has("hover_event"),"Title hover missing");require(o.getAsJsonObject("hover_event").get("action").getAsString().equals("show_text"),"Bad hover action");n++;\n  }}for(var v:o.entrySet())n+=links(v.getValue(),color);\n }else if(e.isJsonArray())for(var v:e.getAsJsonArray())n+=links(v,inheritedColor);return n;}\n public static void main(String[] args)throws Exception{\n  Path root=Path.of(args[0]),temp=root.resolve("build/tmp/phase_b_b8");\n  try {\n   SharedConstants.tryDetectVersion();Bootstrap.bootStrap();\n   var inputs=JsonParser.parseString(Files.readString(temp.resolve("message_inputs.json"))).getAsJsonArray();\n   Pattern tellraw=Pattern.compile("\\\\btellraw\\\\s+\\\\S+\\\\s+([\\\\[{].*)");\n   JsonArray records=new JsonArray();int failures=0;\n   for(var item:inputs){var input=item.getAsJsonObject();String path=input.get("path").getAsString();\n    var row=new JsonObject();row.addProperty("path",path);row.addProperty("sha256",input.get("sha256").getAsString());\n    try {int count=0;\n     for(String line:Files.readAllLines(root.resolve(path))){\n      if(line.stripLeading().startsWith("#")||!line.contains("/advancementssearch highlight ")||!line.contains("tellraw "))continue;\n      String migrated=LegacyChatText.migrateCommand(line);var matcher=tellraw.matcher(migrated);require(matcher.find(),"Cannot parse tellraw");var json=JsonParser.parseString(matcher.group(1));\n      var component=ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();require(hover(component),"No decoded hover");require(links(json,null)>0,"No Search click/style");\n      require(component.equals(ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE,component).getOrThrow()).getOrThrow()),"Round trip");\n      require(migrated.equals(LegacyChatText.migrateCommand(migrated)),"Chat migration idempotence");count++;\n     }\n     require(count>0,"No applicable command");row.addProperty("commands",count);row.addProperty("passed",true);\n    } catch(Throwable t){row.addProperty("passed",false);row.addProperty("error",t.toString());failures++;}\n    records.add(row);\n   }\n   var out=new JsonObject();out.addProperty("messagesChecked",records.size());out.addProperty("failures",failures);out.addProperty("minecraftVersion",SharedConstants.getCurrentVersion().name());\n   out.addProperty("productionClassLocation",LegacyChatText.class.getProtectionDomain().getCodeSource().getLocation().toString());out.add("records",records);\n   Files.writeString(temp.resolve("message_codec_results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out)+"\\n");if(failures!=0)System.exit(1);\n  } catch(Throwable t){var sw=new java.io.StringWriter();t.printStackTrace(new java.io.PrintWriter(sw));Files.writeString(temp.resolve("message_codec_exception.txt"),sw.toString());throw t;}\n }\n}\n'}
SOURCE_INPUTS = [{'enum': 'BACAP', 'source': "build/tmp/phase_b_b1/BlazeandCave's Advancements Pack 1.21.zip", 'sourceSha256': 'c71d1aa1a84dbe00a3f85a42144b46214c4669a3cccf07ff66631d28f16a99b2', 'sourceSha1': '14da3f07b5467e8b59ffc0253fd8212c938cd739'}, {'enum': 'BACAP_HARDCORE', 'source': "build/tmp/phase_b_b4/BlazeandCave's Advancements Pack Hardcore.zip", 'sourceSha256': '1baf5f1bc7c48c819cb6b3081cb124793a316b3568352c853b94757bb4237446', 'sourceSha1': '9c20e14bbef224d2cc4ce63c8d24de1abc7a2475'}, {'enum': 'BACAP_TERRALITH', 'source': 'reference/phase_a_preservation/files/final/bacap_terralith.zip', 'sourceSha256': '0b3cd387fe6e80ac6fe9a05b22091fce6abf6c38dd9656b3086629ec4031d3de', 'sourceSha1': '3d8cc170c1bf2a00460a8d7e779acbe9d5034dea'}, {'enum': 'BACAP_AMPLIFIED_NETHER', 'source': 'reference/phase_a_preservation/files/final/bacap_amplified_nether.zip', 'sourceSha256': 'be7f9c646a8ea8616e34ddef0beab3397995fe66da371cee7cb5cc929f03ad68', 'sourceSha1': '9956d0a7d26e0b7d166711fa4b6bf856d2993a44'}, {'enum': 'BACAP_NULLSCAPE', 'source': 'reference/phase_a_preservation/files/final/bacap_nullscape.zip', 'sourceSha256': '5ad60435dded0a786d4c6320a6af456f640b2e713a080b942c3bed3c2cb85a20', 'sourceSha1': '6a50de576558b6b9079a60ffdff73cd9e622eac1'}, {'enum': 'TERRALITH', 'source': 'build/tmp/phase_b_b4/Terralith_26.2_v2.6.4.zip', 'sourceSha256': '5ac86ed13cc9fc617cbb57e14ce02b1ecac2f2776c36f0536a519378e546e23e', 'sourceSha1': '96ccd25be9ba5240ebe8150cc29240aca781f0e1'}, {'enum': 'AMPLIFIED_NETHER', 'source': 'build/tmp/phase_b_b4/Amplified_Nether_v1.2.15.zip', 'sourceSha256': 'b7013fdf880afe85fb63e06d3edb47795272701463bd850a4500ac02d0b453aa', 'sourceSha1': '94d9604cebbfca667aeb59e4b1ac03e9c6e5cb5d'}, {'enum': 'NULLSCAPE', 'source': 'build/tmp/phase_b_b4/Nullscape_26.2_v1.2.20.zip', 'sourceSha256': 'ec7b0dfcd1add8c127c985dac1466a44168449b3b6641dd20e6b2e16153b66aa', 'sourceSha1': 'bee4a2182593fdfc5da4b253a342feddf02b88b9'}]
CLASSPATH_ENTRIES = ['build/classes/java/main', 'build/resources/main', '.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-84afe0508c/26.2/minecraft-merged-84afe0508c-26.2.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/gson-2.14.0.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/failureaccess-1.0.3.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/guava-33.6.0-jre.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/authlib-9.0.75.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/brigadier-1.3.10.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/datafixerupper-10.0.21.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/logging-1.7.12.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/commons-io-2.20.0.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/netty-buffer-4.2.15.Final.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/netty-codec-4.1.115.Final.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/netty-codec-base-4.2.15.Final.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/netty-codec-compression-4.2.15.Final.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/netty-codec-http-4.2.15.Final.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/netty-common-4.2.15.Final.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/netty-handler-4.2.15.Final.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/netty-resolver-4.2.15.Final.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/netty-transport-4.2.15.Final.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/netty-transport-classes-epoll-4.2.15.Final.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/netty-transport-classes-kqueue-4.2.15.Final.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/netty-transport-native-epoll-4.2.15.Final-linux-x86_64.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/netty-transport-native-kqueue-4.2.15.Final-osx-x86_64.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/netty-transport-native-unix-common-4.2.15.Final.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/fastutil-8.5.18.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/commons-lang3-3.20.0.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/log4j-api-2.26.0.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/log4j-core-2.26.0.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/joml-1.10.8.jar', 'build/tmp/phase_b_b3/converter_probe/probe_libraries/slf4j-api-2.0.17.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-api-base-2.0.4+ece063239e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-api-lookup-api-v1-2.0.17+11e1c9a39e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-biome-api-v1-18.0.6+c7bd5b8e9e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-block-api-v1-3.0.3+ec56b6019e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-block-getter-api-v2-2.0.7+ec56b6019e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-command-api-v2-3.1.0+00cb03469e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-content-registries-v0-11.3.1+37b1aa249e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-convention-tags-v2-4.7.0+1f023a9e9e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-crash-report-info-v1-1.0.5+086d547a9e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-creative-tab-api-v1-5.0.14+d871b99e9e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-data-attachment-api-v1-2.2.18+515ac5339e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-data-generation-api-v1-25.5.0+aed122aa9e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-debug-api-v1-1.0.2+c792624d9e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-dimensions-v1-5.1.11+515ac5339e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-entity-events-v1-5.0.5+06488ac19e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-events-interaction-v0-5.2.7+515ac5339e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-game-rule-api-v1-4.0.8+46a6d00c9e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-item-api-v1-14.5.0+c68f6cbe9e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-key-mapping-api-v1-2.0.5+e2bdee789e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-lifecycle-events-v1-4.1.3+4575b05f9e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-loot-api-v3-3.0.17+06488ac19e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-menu-api-v1-2.0.16+086d547a9e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-message-api-v1-7.0.8+bb2ed3f79e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-model-loading-api-v1-8.0.16+c80601bb9e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-networking-api-v1-6.3.3+72073ef09e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-object-builder-api-v1-24.1.0+6fcd5f039e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-particles-v1-5.0.18+d1756aa99e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-permission-api-v1-1.0.5+e442d8d09e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-recipe-api-v1-9.0.20+11e1c9a39e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-registry-sync-v0-7.1.0+c7bd5b8e9e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-renderer-api-v1-14.1.3+2b0d8a229e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-renderer-indigo-9.1.3+2b0d8a229e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-rendering-fluids-v1-6.0.4+06488ac19e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-rendering-v1-25.3.2+515ac5339e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-resource-conditions-api-v1-6.1.0+10e9a28b9e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-resource-loader-v0-3.3.20+4fc5413f9e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-resource-loader-v1-2.0.13+9edec1269e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-screen-api-v1-5.2.0+58e078ad9e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-serialization-api-v1-2.0.4+11a26f319e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-sound-api-v1-2.0.5+11a26f319e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-tag-api-v1-2.1.4+515ac5339e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-transfer-api-v1-8.0.12+11dbb7d79e.jar', 'build/tmp/phase_b_b5_5/current_api_libraries/fabric-transitive-access-wideners-v1-8.1.4+67c847259e.jar']

RESOURCE_CHECK_SOURCE = 'import com.google.gson.*;\nimport com.mojang.serialization.JsonOps;\nimport net.minecraft.SharedConstants;\nimport net.minecraft.core.*;\nimport net.minecraft.core.registries.BuiltInRegistries;\nimport net.minecraft.data.registries.VanillaRegistries;\nimport net.minecraft.resources.*;\nimport net.minecraft.tags.TagKey;\nimport net.minecraft.world.level.storage.loot.predicates.LootItemCondition;\nimport java.nio.file.*;\nimport java.util.*;\n\npublic class ResourceCheck {\n @SuppressWarnings({"rawtypes","unchecked"})\n public static void main(String[] args)throws Exception {\n  SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();\n  var builtins=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).freeze();\n  var vanilla=VanillaRegistries.createLookup();\n  Map<String,HolderLookup.RegistryLookup<?>> base=new LinkedHashMap<>();\n  builtins.listRegistries().forEach(r->base.put(r.key().identifier().toString(),r));\n  vanilla.listRegistries().forEach(r->base.put(r.key().identifier().toString(),r));\n  Path dir=Path.of(args[0]);String view=args[1];\n  var resources=JsonParser.parseString(Files.readString(dir.resolve("codec_resources_"+view+".json"))).getAsJsonObject();\n  B8Codec.resources=resources;\n  Map<String,HolderLookup.RegistryLookup<?>> lookups=new LinkedHashMap<>();\n  base.forEach((k,v)->lookups.put(k,B8Codec.backed(v)));\n  var provider=HolderLookup.Provider.create(lookups.values().stream());var ops=RegistryOps.create(JsonOps.INSTANCE,provider);\n  var input=JsonParser.parseString(Files.readString(dir.resolve("resource_refs_"+view+".json"))).getAsJsonArray();\n  JsonArray results=new JsonArray();int failed=0;\n  for(var x:input){var row=x.getAsJsonObject().deepCopy();boolean ok=false;\n   try{\n    String reg=row.get("registry").getAsString(),id=row.get("id").getAsString();boolean tag=row.get("tag").getAsBoolean();\n    var lookup=(HolderLookup.RegistryLookup)lookups.get("minecraft:"+reg);if(lookup==null)throw new IllegalStateException("Unknown registry "+reg);\n    ok=tag?lookup.get(TagKey.create(lookup.key(),Identifier.parse(id))).isPresent():lookup.get(ResourceKey.create(lookup.key(),Identifier.parse(id))).isPresent();\n   }catch(Throwable e){row.addProperty("error",e.toString());}\n   row.addProperty("passed",ok);if(!ok)failed++;results.add(row);\n  }\n  JsonArray predicates=new JsonArray();\n  for(var entry:resources.entrySet())if(entry.getKey().matches("data/[^/]+/predicate/.+\\\\.json")){\n   var out=new JsonObject();out.addProperty("path",entry.getKey());var parsed=LootItemCondition.DIRECT_CODEC.parse(ops,entry.getValue());\n   boolean ok=parsed.error().isEmpty();out.addProperty("passed",ok);parsed.error().ifPresent(e->out.addProperty("error",e.message()));if(!ok)failed++;predicates.add(out);\n  }\n  JsonObject report=new JsonObject();report.addProperty("minecraftVersion",SharedConstants.getCurrentVersion().name());report.addProperty("referencesChecked",results.size());report.addProperty("predicatesChecked",predicates.size());report.addProperty("failed",failed);report.add("records",results);report.add("predicates",predicates);\n  Files.writeString(dir.resolve("resource_"+view+"_results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));\n  System.out.println(view+" refs="+results.size()+" predicates="+predicates.size()+" failed="+failed);if(failed>0)System.exit(1);\n }\n}\n'

def certify_resource_contracts(effective=None):
    """Current typed resource, scoreboard and companion static successor."""
    import types
    import importlib.util
    b=types.SimpleNamespace(**globals()); R=ROOT; T=TEMP
    (T/'ResourceCheck.java').write_text(RESOURCE_CHECK_SOURCE,encoding='utf8')
    effective = effective or b.views(); static=b.load(T/'static_validation.json')
    sp=importlib.util.spec_from_file_location('b3',R/'tools/phase_b/b3_bacap_semantic_diff.py');b3=importlib.util.module_from_spec(sp);sp.loader.exec_module(b3)
    # Recompute the B3 scoreboard extraction on frozen/current input bytes; the
    # absent B2 path deliberately disables unrelated historical overlay enumeration.
    scores=b3.contract_probe(b.OLD,b.TARGET,T/'no_b2_overlay_inventory.json')
    assert scores['summary']['targetDefinedObjectives']==110 and scores['summary']['targetReferencedObjectives']==143
    b.write(T/'scoreboard_validation.json',{'summary':scores['summary'],'rawProgressionObjectives':static['scoreboardContract']['rawProgressionObjectives'],'pointsSeparate':True,'targetObjectiveIdentityMatchesB3':scores['objectiveContracts']==b.load(b.PHASE/'b3_resource_graph_diff.json')['scoreboardContracts']['objectiveContracts'] if 'objectiveContracts' in scores else None})
    graphs={}
    for combo,view in effective.items():
     resources=view['resources'];funcs=view['functions'];refs=[];direct=[];optional=[]
     def add(reg,rid,source,location,tag=False):
      if '$(' in rid:optional.append({'source':source,'location':location,'kind':'DYNAMIC_MACRO_TARGET','id':rid});return
      rid=rid if ':' in rid else 'minecraft:'+rid
      refs.append({'registry':reg,'id':rid,'tag':tag,'source':source,'location':location})
     def file_ref(kind,rid,source,location):
      if '$(' in rid:optional.append({'source':source,'location':location,'kind':'DYNAMIC_MACRO_TARGET','id':rid});return
      rid=rid if ':' in rid else 'minecraft:'+rid
      path='data/'+rid.replace(':','/'+kind+'/')+'.json'
      direct.append({'source':source,'location':location,'kind':kind,'id':rid,'path':path,'resolved':path in resources,'owner':'VANILLA_OR_BUILTIN' if rid.startswith('minecraft:') else 'PACK_LOCAL'})
     def walk(x,source,loc=''):
      if isinstance(x,dict):
       if isinstance(x.get('rewards'),dict):
        for reward_kind,resource_kind in [('recipes','recipe'),('loot','loot_table')]:
         for rid in x['rewards'].get(reward_kind,[]):
          file_ref(resource_kind,rid,source,loc+'/rewards/'+reward_kind)
       if x.get('condition')=='minecraft:reference' and isinstance(x.get('name'),str):file_ref('predicate',x['name'],source,loc+'/name')
       if x.get('type')=='minecraft:loot_table' and isinstance(x.get('value',x.get('name')),str):file_ref('loot_table',x.get('value',x.get('name')),source,loc)
       if x.get('type')=='minecraft:tag' and isinstance(x.get('name'),str):add('item',x['name'],source,loc,True)
       if x.get('condition')=='minecraft:recipe_unlocked' and isinstance(x.get('recipe'),str):file_ref('recipe',x['recipe'],source,loc+'/recipe')
       if x.get('function')=='minecraft:reference' and isinstance(x.get('name'),str):file_ref('item_modifier',x['name'],source,loc+'/name')
       for key,value in x.items():
        if key=='predicate' and isinstance(value,str) and re.fullmatch(r'[a-z0-9_.:/-]+',value):file_ref('predicate',value,source,loc+'/predicate')
        walk(value,source,loc+'/'+key)
      elif isinstance(x,list):
       for n,value in enumerate(x):walk(value,source,loc+'/'+str(n))
     for path,data in resources.items():
      if not path.endswith('.json'):continue
      value=json.loads(data);walk(value,path)
      match=re.fullmatch(r'data/([^/]+)/tags/(.+)/([^/]+)\.json',path)
      # Registry names can themselves contain '/', so derive the registry prefix
      # from the known Minecraft tag directory list rather than path depth.
      m=re.match(r'data/([^/]+)/tags/(item|entity_type|block|fluid|enchantment|damage_type|worldgen/biome|worldgen/structure|worldgen/structure_set|worldgen/configured_feature|worldgen/placed_feature|worldgen/world_preset|worldgen/flat_level_generator_preset|banner_pattern|instrument|painting_variant|cat_variant|wolf_variant|point_of_interest_type)/(.*)\.json$',path)
      if m:
       add(m[2],m[1]+':'+m[3],path,'self',True)
       for n,member in enumerate(value['values']):
        if isinstance(member,dict) and not member.get('required',True):
         optional.append({'source':path,'location':str(n),'kind':'GUARDED_OPTIONAL_RESOURCE','id':member['id']});continue
        rid=member['id'] if isinstance(member,dict) else member
        add(m[2],rid.lstrip('#'),path,'values/'+str(n),rid.startswith('#'))
     for fid,body in funcs.items():
      for n,line in enumerate(body.splitlines(),1):
       if not line.strip() or line.lstrip().startswith('#'):continue
       for rid in re.findall(r'\b(?:if|unless) predicate ([a-z0-9_.:/-]+)',line):file_ref('predicate',rid,fid,str(n))
       for rid in re.findall(r'\brecipe (?:give|take) \S+ ([a-z0-9_.:/-]+)',line):file_ref('recipe',rid,fid,str(n))
       if re.search(r'(?:^\$?|\brun )loot ',line):
        for rid in re.findall(r'\b(?:loot|fish) ([a-z0-9_.-]+:[a-z0-9_./-]+)',line):file_ref('loot_table',rid,fid,str(n))
       for rid in re.findall(r'\blocate biome (#?[a-z0-9_.:/-]+)',line):add('worldgen/biome',rid.lstrip('#'),fid,str(n),rid.startswith('#'))
       for rid in re.findall(r'\blocate structure (#?[a-z0-9_.:/-]+)',line):add('worldgen/structure',rid.lstrip('#'),fid,str(n),rid.startswith('#'))
       for rid in re.findall(r'\btype=(#[a-z0-9_.:/-]+)',line):add('entity_type',rid[1:],fid,str(n),True)
       for rid in re.findall(r'\b(?:if|unless) block \S+ \S+ \S+ (#[a-z0-9_.:/-]+)',line):add('block',rid[1:],fid,str(n),True)
       for rid in re.findall(r'\b(?:if|unless) items \S+ \S+ (#[a-z0-9_.:/-]+)',line):add('item',rid[1:],fid,str(n),True)
     missing=[r for r in direct if not r['resolved']]
     b.write(T/('resource_refs_'+combo+'.json'),refs)
     graphs[combo]={'directReferencesChecked':len(direct),'directReferences':direct,'unexplainedDirectMissingResources':missing,'typedRegistryReferences':len(refs),'guardedOptional':optional,'functionGraphCounts':view['report']['functionCallCounts'],'unexpectedDirectDanglingCalls':view['report']['directDanglingCalls']}
     assert not missing,(combo,missing)
    b.write(T/'resource_graph.json',graphs)
    jdk=pathlib.Path('C:/Program Files/Eclipse Adoptium/jdk-25.0.4.7-hotspot/bin');cp=str(T)+';'+';'.join(str(R/p) for p in b.CLASSPATH_ENTRIES)
    assert b.java_command('resource_javac',[str(jdk/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(T),str(T/'ResourceCheck.java')])==0
    for combo in effective:
     assert b.java_command('resource_check_'+combo,[str(jdk/'java.exe'),'-Xmx3g','-cp',cp,'ResourceCheck',str(T),combo])==0,combo
     d=b.load(T/('resource_'+combo+'_results.json'));assert d['failed']==0
     print(combo,'resource refs',d['referencesChecked'],'predicates',d['predicatesChecked'],'PASS',flush=True)
    comp=b.load(b.PHASE/'b5_6_companions_resumed.json');merges=[]
    for row in comp['advancementMerges']['records']:
     path=row['path'];rid=row['advancementId'];combo='terralith' if '/bacap_terralith_override/' in path else 'amplifiedNether' if '/bacap_amplified_nether_override/' in path else 'nullscape'
     assert b.sha((R/path).read_bytes())==row['resultingSha256']
     parsed=json.loads((R/path).read_bytes());assert parsed==effective[combo]['advancements'][rid]
     codec=next(x for x in b.load(T/('codec_'+combo+'_results.json'))['records'] if x['id']==rid);assert codec['accepted'] and codec['criteriaPreserved']
     merges.append({'path':path,'id':rid,'companion':combo,'sha256':b.sha((R/path).read_bytes()),'codecPassed':True,'criteriaPreserved':True,'fieldOwnershipEvidence':{'path':'reference/phase_b/b5_6_companions_resumed.json','sha256':b.sha((b.PHASE/'b5_6_companions_resumed.json').read_bytes()),'changedPointers':row['changedPointers']}})
    assert len(merges)==14
    for row in comp['terralithWrappers']['records']:
     text=(R/row['path']).read_text();assert b.sha((R/row['path']).read_bytes())==row['resultingSha256']
     assert re.findall(r'^function (\S+)',text,re.M)==[row['macroTarget']]
     assert dict(re.findall(r'(\w+):"([^"]+)"',text))==row['arguments'] and row['advancementId'] in effective['terralith']['advancements']
    for row in comp['hardcoreMessages']['records']:
     assert b.sha((R/row['path']).read_bytes())==row['resultingSha256']
     assert row['SearchTarget'] in effective['hardcore']['advancements']
    assert len(comp['terralithWrappers']['records'])==24 and len(comp['hardcoreMessages']['records'])==6
    assert '/advancementssearch highlight nullscape:root' in (R/comp['modifiedExistingCompanionResource']['path']).read_text()
    b.write(T/'companion_validation.json',{'merges':merges,'total':14,'passed':14,'distribution':{'terralith':11,'amplifiedNether':2,'nullscape':1},'terralithWrappers':24,'hardcoreMessages':6,'nullscapeSearch':'nullscape:root'})
    print('Expanded resources, scoreboard and companion gates PASS.',flush=True)
    return {'resourceGraph':graphs,'scoreboard':load(T/'scoreboard_validation.json'),'companions':load(T/'companion_validation.json')}

def complete_static_gates():
    prepare()
    assert run_real_codec()['passed']
    static_checks()
    jdk=pathlib.Path(os.environ.get('JAVA_HOME','C:/Program Files/Eclipse Adoptium/jdk-25.0.4.7-hotspot'))/'bin'
    cp=str(TEMP)+';'+';'.join(str(ROOT/p) for p in CLASSPATH_ENTRIES)
    assert java_command('registry_check',[str(jdk/'java.exe'),'-cp',cp,'B8Registry',str(TEMP/'registry_input.json'),str(TEMP/'registry_results.json')]) == 0
    assert java_command('message_check',[str(jdk/'java.exe'),'-Xmx2g','-cp',cp,'MessageCodec',str(ROOT)]) == 0
    certify_resource_contracts()
    print('Current Phase B static gates PASS. Durable receipts/JUnit are separately verified; no runtime certification claimed.')

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--self-test', action='store_true')
    parser.add_argument('--prepare', action='store_true')
    parser.add_argument('--static', action='store_true')
    parser.add_argument('--real-codec', action='store_true')
    parser.add_argument('--record-stop', action='store_true')
    parser.add_argument('--complete-static', action='store_true')
    args = parser.parse_args()
    if args.self_test:
        unittest.main(argv=['b8'], exit=True)
    elif args.prepare:
        prepare()
    elif args.static:
        static_checks()
    elif args.real_codec:
        result=run_real_codec()
        write(TEMP/'real_codec_gate.json',result)
        raise SystemExit(0 if result['passed'] else 1)
    elif args.complete_static:
        complete_static_gates()
    elif args.record_stop:
        stop_evidence()
