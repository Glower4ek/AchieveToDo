"""Independent B7 extraction. Reads archives and source; writes only the requested report."""
import argparse
import collections
import hashlib
import json
import pathlib
import re
import subprocess
import zipfile
import unittest

ROOT = next(p for p in pathlib.Path(__file__).resolve().parents if (p/'.git').exists())
PHASE = ROOT / 'reference/phase_b'
# Current successor runs must not overwrite the historical B7 extraction receipt.
TEMP = ROOT / 'build/tmp/phase_b_b7_m2'
RU_PATH = 'src/main/resources/assets/minecraft/lang/ru_ru.json'
HEAD = 'b261b02cd03b4aeae2835c63f982c9aa53c46eed'
RU_HASH = '8961ad9d573ab72f63969e4a808b19ce733bc9a60b7ab589fc44dfb6148e1a0a'

# Exact downstream authority; historical B7/R1 evidence keeps its original hashes.
CURRENT_MARKER = 'compat_26_2_r19'
CURRENT_ROOT_OVERRIDE_SHA1 = 'cbc432be35d5525001872c430877541cfa1fcad6'
CURRENT_B5_SUPERSESSIONS = {
    'src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java':
        '6ab0674902b4783c39c08dfd802cd0067d1436d973345834f092a001a4db9fec',
    'src/main/resources/resourcepacks/bacap_override/data/bacap_rewards/function/potion/root.mcfunction':
        '4c650f38dcb23f4842a230f3c41e6d79cb05ee66c68a3be49cba5889912c305c',
}
CURRENT_POST_B5_PATHS = {
    RU_PATH: RU_HASH,
    'src/main/resources/resourcepacks/bacap_override/data/bacap_rewards/function/msg/adventure/ive_got_a_bad_feeling_about_this.mcfunction':
        'b33b9a471c612c7ff7bb906fe8fbd541691f5b155a40f258fde8cc544aee5d6c',
    'src/main/resources/resourcepacks/bacap_override/data/bacap_rewards/function/msg/animal/turtle_army.mcfunction':
        '0d0afad93978dd15f5e0e1cdf2a4596f7dff90469be710b42f6d63b56502818f',
    'src/main/resources/resourcepacks/bacap_override/data/bacap_rewards/function/msg/end/dogfight.mcfunction':
        '2ebd20b7c69026e1e6c4d6bfb34b527543fc94276cf853875d764f6d2b011f75',
}

# Current-only oracle refresh: historical B7 receipts remain hash-bound inputs.
HISTORICAL_B7_CERTIFICATION_SHA256 = 'be3aca8716e68e48b293bb5299418a87aec98bda340b4836f9f50e70705e92d3'
RETIRED_CURRENT_CONSUMER_REPLACEMENTS = {
    'Kill a raid captain. Maybe consider staying away from villages for the time being...':
        'Kill a raid captain. I’d warn against drinking that bottle they dropped…',
    'Collect a stack of scutes': 'Collect a stack of Turtle Scutes',
    'Kill a Skeleton or Stray while both you and it have levitation':
        'Kill a Skeleton while both you and it have levitation',
}
CURRENT_EXPECTED_PROVIDER_COUNTS = {
    'ATD_RU_DICTIONARY': 533, 'MINECRAFT_RU_OVERLAY': 3432,
    'VANILLA_26_2_RU': 325, 'INTENTIONALLY_LANGUAGE_NEUTRAL': 11,
}
HISTORICAL_EXPECTED_VIEW_COUNTS = {
    'main': 3702, 'hardcore': 3702, 'amplifiedNether': 3702,
    'terralith': 3756, 'nullscape': 3705,
}
CURRENT_EXPECTED_VIEW_COUNTS = {
    'main': 3699, 'hardcore': 3699, 'amplifiedNether': 3699,
    'terralith': 3753, 'nullscape': 3702,
}

def sha(data):
    return hashlib.sha256(data).hexdigest()

def value_hash(value):
    return sha(value.encode('utf8'))

def load(path):
    return json.loads(pathlib.Path(path).read_text(encoding='utf8'))

def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, stderr=subprocess.PIPE)

def strict_dictionary(data):
    duplicate = []
    def pairs(items):
        result = {}
        for k, v in items:
            if k in result:
                duplicate.append(k)
            result[k] = v
        return result
    result = json.loads(data.decode('utf8'), object_pairs_hook=pairs)
    assert not duplicate, duplicate
    assert all(isinstance(v, str) for v in result.values())
    return result

def write(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2, sort_keys=True) + '\n', encoding='utf8')

def precheck():
    assert git('branch', '--show-current').decode().strip() == 'phase-b-bacap-26.2'
    assert git('rev-parse', 'HEAD').decode().strip() == HEAD
    assert not git('diff', '--cached', '--name-only').strip()
    assert 'MARKER_VERSION = "'+CURRENT_MARKER+'"' in (ROOT/'src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java').read_text()
    r1 = load(PHASE/'b7_r1_source_identity_marker_repair.json')
    assert r1['status'] == 'PHASE_B_B7_R1_REPAIRED' and r1['readyToResumeB7']
    repaired_path = r1['productionFile']['path']
    assert r1['productionFile']['afterSha256'] == '801634641b388c06b4c564c0bd16b0dd040c48a7a8228d602712bacd4b6b780a'
    current_r1 = load(PHASE/'b8_r1_terralith_player_predicate_repair.json')
    assert current_r1['status'] == 'PHASE_B_B8_R1_REPAIRED'
    assert current_r1['productionFile']['path'] == repaired_path
    assert current_r1['productionFile']['afterSha256'] == CURRENT_B5_SUPERSESSIONS[repaired_path]
    current_r2 = load(PHASE/'b8_r2_message_binding_repair.json')
    assert current_r2['status'] == 'PHASE_B_B8_R2_REPAIRED'
    assert current_r2['rootOverrideDigest']['after'] == CURRENT_ROOT_OVERRIDE_SHA1
    current_resource_hashes = {**CURRENT_B5_SUPERSESSIONS, **CURRENT_POST_B5_PATHS}
    assert len(current_r2['repairs']) == 4
    for repair in current_r2['repairs']:
        assert repair['afterSha256'] == current_resource_hashes[repair['path']], repair['path']
    for path, expected in current_resource_hashes.items():
        assert sha((ROOT/path).read_bytes()) == expected, path
    for p, h in r1['safety']['historicalEvidenceHashesUnchanged'].items():
        assert sha((ROOT/p).read_bytes()) == h, p
    b5 = load(PHASE/'b5_7_structural_reconciliation.json')
    b6 = load(PHASE/'b6_ru_translation_manifest.json')
    assert b5['B5_COMPLETE'] and b5['status'] == 'PHASE_B_B5_7_COMPLETE'
    assert b6['b7Ready'] and b6['status'] == 'PHASE_B_B6_LOCALIZED'
    assert sha((PHASE/'b5_7_structural_reconciliation.json').read_bytes()) == b6['b5_7EvidenceSha256']
    assert len(b5['productOwnership']['records']) == 251
    original_b5_paths = {r['path'] for r in b5['productOwnership']['records']}
    assert len(original_b5_paths) == 251
    assert set(CURRENT_B5_SUPERSESSIONS) <= original_b5_paths
    assert not set(CURRENT_POST_B5_PATHS) & original_b5_paths
    exact = 0
    for record in b5['productOwnership']['records']:
        if record['path'] in CURRENT_B5_SUPERSESSIONS:
            historical_hash = r1['productionFile']['beforeSha256'] if record['path'] == repaired_path else next(
                r['beforeSha256'] for r in current_r2['repairs'] if r['path'] == record['path'])
            assert record['currentSha256'] == historical_hash, record['path']
            continue
        p = ROOT/record['path']
        assert (not p.exists()) if record['gitAction'] == 'DELETE' else sha(p.read_bytes()) == record['currentSha256'], record['path']
        exact += 1
    assert exact == 249
    for p, h in b6['priorEvidenceHashes'].items():
        assert sha((ROOT/p).read_bytes()) == h, p
    for artifact in b6['nonRuntimeLegalArtifacts']:
        assert sha((ROOT/artifact['path']).read_bytes()) == artifact['sha256']
    assert sha((ROOT/RU_PATH).read_bytes()) == RU_HASH
    runtime = {p.relative_to(ROOT).as_posix(): sha(p.read_bytes()) for p in (ROOT/'src/main').rglob('*') if p.is_file()}
    evidence = {p.relative_to(ROOT).as_posix(): sha(p.read_bytes()) for p in PHASE.iterdir() if p.is_file()}
    historical_tests = {p.relative_to(ROOT).as_posix(): sha(p.read_bytes()) for p in (ROOT/'src/test').rglob('*') if p.is_file()}
    changes = dict(x.split('\t', 1)[::-1] for x in git('diff', '--name-status', '--no-renames').decode().splitlines())
    untracked = git('ls-files', '--others', '--exclude-standard', '-z').decode().split('\0')
    for p in untracked:
        if p.startswith('src/main/'):
            changes[p] = 'A'
    runtime_changes = {p:a for p,a in changes.items() if p.startswith('src/main/')}
    assert set(runtime_changes) == original_b5_paths | set(CURRENT_POST_B5_PATHS)
    counts = dict(collections.Counter(runtime_changes.values()))
    assert counts == {'M': 69, 'D': 9, 'A': 177} and len(runtime_changes) == 255, counts
    return {'runtimeHashes': runtime, 'evidenceHashes': evidence, 'historicalTestHashes': historical_tests, 'productDiff': counts}

def strings(obj, location='$'):
    """Walk real JSON objects, never classify a generic quoted string as a key."""
    if isinstance(obj, dict):
        if isinstance(obj.get('translate'), str):
            yield obj['translate'], location+'/translate'
        for k, v in obj.items():
            yield from strings(v, location+'/'+k)
    elif isinstance(obj, list):
        for i, v in enumerate(obj):
            yield from strings(v, location+'/'+str(i))

def tokens(text):
    """Small SNBT/command string lexer, including nested serialized components."""
    i = 0
    while i < len(text):
        c = text[i]
        if c in '\"\'':
            quote = c; i += 1; buf = []
            while i < len(text):
                if text[i] == quote:
                    i += 1; break
                if text[i] == '\\' and i+1 < len(text):
                    nxt = text[i+1]
                    if nxt == 'u' and re.fullmatch('[0-9a-fA-F]{4}', text[i+2:i+6]):
                        buf.append(chr(int(text[i+2:i+6], 16))); i += 6; continue
                    buf.append({'n':'\n', 'r':'\r', 't':'\t', 'b':'\b', 'f':'\f'}.get(nxt, nxt)); i += 2
                else:
                    buf.append(text[i]); i += 1
            yield 'string', ''.join(buf)
        elif c.isalnum() or c in '_.$-':
            j = i+1
            while j < len(text) and (text[j].isalnum() or text[j] in '_.$-'):
                j += 1
            yield 'word', text[i:j]; i = j
        elif c.isspace():
            i += 1
        else:
            yield c, c; i += 1

def command_keys(text, depth=0):
    ts = list(tokens(text))
    for i in range(len(ts)-2):
        if ts[i][1] == 'translate' and ts[i+1][0] == ':' and ts[i+2][0] == 'string':
            yield ts[i+2][1]
    if depth < 3:
        for kind, value in ts:
            if kind == 'string' and 'translate' in value and ':' in value:
                yield from command_keys(value, depth+1)

def pack_zip(path, owner):
    with zipfile.ZipFile(path) as z:
        return {n: (owner, z.read(n)) for n in sorted(z.namelist()) if n.startswith('data/') and not n.endswith('/')}

def builtin(name):
    base = ROOT/'src/main/resources/resourcepacks'/name
    return {p.relative_to(base).as_posix(): (name, p.read_bytes()) for p in sorted((base/'data').rglob('*')) if p.is_file()}

def extract(resources):
    result = collections.defaultdict(list)
    for p, (owner, data) in sorted(resources.items()):
        if owner == 'VANILLA':
            continue
        if p.endswith('.json'):
            obj = json.loads(data.decode('utf8'))
            for key, pointer in strings(obj):
                result[key].append({'owner':owner, 'path':p, 'location':pointer})
        elif '/function/' in p and p.endswith('.mcfunction'):
            for line, text in enumerate(data.decode('utf8').splitlines(), 1):
                if not text.strip() or text.lstrip().startswith('#'):
                    continue
                for key in sorted(set(command_keys(text))):
                    result[key].append({'owner':owner, 'path':p, 'line':line})
    return dict(result)

NEUTRAL = {
    "'s Advancements Pack!": 'Pack brand suffix',
    '/team join bac_team_<color> <player>': 'Executable command syntax',
    '2013 - 2020': 'Dates', '>:)': 'Emoticon', '[ »» ]': 'Navigation glyphs',
    'Blaze': 'Named pack coauthor', 'Cave': 'Named pack coauthor',
    'Discord': 'Service name', 'Flex Tape': 'Brand name', 'Patreon': 'Service name',
    '': 'Empty spacing component',
}

FORMAT = re.compile(r'%(?:(\d+)\$)?([-#+ 0,(<]*)(\d+)?(?:\.(\d+))?([tT])?([a-zA-Z%])')

def formats(text):
    implicit = 0; last = None; numbered = []; unnumbered = []; literal = 0; newline = 0
    for m in FORMAT.finditer(text):
        number, flags, width, precision, date, conversion = m.groups()
        if conversion == '%':
            literal += 1; continue
        if conversion == 'n' and not date:
            newline += 1; continue
        if number:
            index = int(number); explicit = True
        elif '<' in flags:
            index = last; explicit = True
        else:
            implicit += 1; index = implicit; explicit = False
        last = index
        token = (index, date or '', conversion)
        numbered.append(token)
        if not explicit:
            unnumbered.append(token)
    return {'arguments':sorted(numbered, key=str), 'unnumberedOrder':unnumbered, 'literalPercentEscapes':literal, 'newlineTokens':newline}

def provenance_certification(ru, changed, requirements, input_root=None):
    manifest = load(PHASE/'b6_ru_translation_manifest.json')
    records = manifest['perChangedKeyProvenance']
    keys = [r['key'] for r in records]
    assert len(keys) == len(set(keys)) == 296
    assert set(keys) == set(changed)
    source_cache = {}; imports = collections.Counter(); checks = []
    source_files = {
        'YX5bAAJN':'bacap-language-pack', '47BuT3SL':'bacap-rus-translate',
        'yiLJxtqH':'bacap-better-ru', 'DlwNJUii':'ru-blaze-and-caves-advancements-pack',
        'CGFOvwcX':'bacaped-language-pack',
    }
    # Explicit current input root; historical CLI callers must supply it too.
    import os
    b6temp = pathlib.Path(input_root or os.environ['ACHIEVETODO_TEST_INPUTS_DIR'])
    source_identities = []
    for project, prefix in source_files.items():
        acquisition = load(b6temp/(prefix+'_acquisition.json'))
        metadata = load(b6temp/(prefix+'_metadata.json'))
        assert acquisition['projectId'] == metadata['project']['id'] == project
        version = next(v for v in metadata['versions'] if v['id']==acquisition['versionId'])
        assert version['version_number'] == acquisition['version']
        archive = (b6temp/(prefix+'.zip')).read_bytes()
        assert len(archive) == acquisition['size']
        for algorithm, expected in acquisition['hashes'].items():
            assert hashlib.new(algorithm, archive).hexdigest() == expected
        with zipfile.ZipFile(b6temp/(prefix+'.zip')) as z:
            text = z.read('assets/minecraft/lang/ru_ru.json').decode('utf-8-sig')
            # Source comment headers are not dictionary declarations.
            text = '\n'.join(l for l in text.splitlines() if not l.lstrip().startswith(('#','//')))
            # Upstream dictionaries can repeat declarations. Match the game's
            # last-declaration-wins parse; the ATD runtime remains strict.
            source_pairs = json.loads(text, object_pairs_hook=lambda pairs:pairs)
            duplicate_keys = [k for k,n in collections.Counter(k for k,v in source_pairs).items() if n>1]
            dictionary = dict(source_pairs)
        source_cache[project] = (acquisition, dictionary)
        source_identities.append({'projectId':project, 'versionId':version['id'], 'version':version['version_number'], 'license':metadata['project']['license']['id'], 'archiveSha256':sha(archive), 'metadataSha256':sha((b6temp/(prefix+'_metadata.json')).read_bytes()), 'sourceDuplicateKeys':duplicate_keys, 'sourceDuplicatePolicy':'Last declaration wins, matching runtime JSON parsing; final product dictionary separately rejects duplicates.'})
    permission_path = ROOT/'reference/localization/phase_a_ru_full_reaudit_summary.json'
    permission = load(permission_path)
    lp = permission['sources']['official_lp_1_21']
    assert lp['project_id']=='YX5bAAJN' and lp['version_id']=='hsqY3G3V' and lp['version_number']=='1.21' and lp['game_versions']==['26.2']
    assert 'direct permission' in permission['provenance'] and 'Allowed for this project via direct permission' in lp['usage']
    by_key = {r['key']:r for r in requirements}
    for r in records:
        key = r['key']; assert key in by_key
        assert value_hash(ru[key]) == r['finalValueSha256'], key
        kind = r['sourceType']; imports[kind] += 1
        if kind in ('OFFICIAL_LP_1_21_PERMISSION','OTHER_PERMITTED_SOURCE'):
            identity = r['sourceIdentity']; project = identity['projectId']
            assert project == ('YX5bAAJN' if kind=='OFFICIAL_LP_1_21_PERMISSION' else '47BuT3SL')
            acq, dictionary = source_cache[project]
            assert identity['versionId'] == acq['versionId'] and identity['archiveSha256'] == acq['hashes']['sha256']
            assert dictionary[identity['sourceDictionaryKey']] == ru[key], key
        else:
            assert kind == 'MANUAL_B6'
            assert (ru[key] != key or key == 'M.O.A.B.') and ru[key].strip(), key
        checks.append({'key':key,'owner':kind,'finalValueSha256':value_hash(ru[key]),'consumerCount':len(by_key[key]['consumers']),'sourceValueVerified':kind!='MANUAL_B6'})
    assert dict(imports) == {'OFFICIAL_LP_1_21_PERMISSION':211,'OTHER_PERMITTED_SOURCE':21,'MANUAL_B6':64}, imports
    notice_path = ROOT/'licenses/bacap-rus-translate/LICENSE'
    notice = notice_path.read_text(encoding='utf8')
    with zipfile.ZipFile(b6temp/'bacap-rus-translate.zip') as z:
        files = z.namelist(); disclaimer = z.read('DISCLAIMER.txt').decode('utf8').replace('\r\n','\n')
        readme = z.read('README.md').decode('utf8')
        assert not [f for f in files if pathlib.PurePosixPath(f).name.upper() in ('LICENSE','COPYING','COPYRIGHT','LICENSE.TXT')]
        assert disclaimer in notice.replace('\r\n','\n')
        assert 'BSD-2-Clause' in readme
        assert all(name in readme and name in notice for name in ('SHTUKA','lialimoro','Perchila27','Limoro'))
    canonical = (b6temp/'BSD-2-Clause.txt').read_text(encoding='utf8').replace('\r\n','\n')
    assert canonical in notice.replace('\r\n','\n')
    assert '<year>' in notice and '<owner>' in notice
    assert 'licenses' in (ROOT/'build.gradle').read_text(encoding='utf8')
    latin = []; leaks = []
    accepted = {'Craftmine','Bounce','M.O.A.B.','II','III','Advancement','Info','Reloaded'}
    for key in sorted(changed):
        value = re.sub(r'§[0-9a-fk-or]', '', ru[key], flags=re.I)
        value = FORMAT.sub('',value)
        words = re.findall('[A-Za-z]+(?:\\.[A-Za-z]+)*\\.?',value)
        bad = [w for w in words if w.rstrip('.') not in {x.rstrip('.') for x in accepted}]
        if bad or (ru[key] == key and key != 'M.O.A.B.'):
            leaks.append({'key':key, 'unapprovedLatinTokens':bad})
        if words:
            latin.append({'key':key,'tokens':words,'reason':'Exact mod/track/game names, acronym, or Roman numeral; remaining prose is Russian.'})
    assert not leaks, leaks
    return {
        'partition':{'RETAINED_PHASE_A':3186,**dict(imports)}, 'changedValueRecords':checks,
        'missing':0,'duplicates':0,'hashMismatch':0,'sourceIdentities':source_identities,
        'officialPermission':{'path':permission_path.relative_to(ROOT).as_posix(),'sha256':sha(permission_path.read_bytes()),'projectSpecific':True,'passed':True},
        'bsdNotice':{'path':notice_path.relative_to(ROOT).as_posix(),'sha256':sha(notice_path.read_bytes()),'result':'PASS_WITH_DOCUMENTED_LIMITATION','limitation':'NOTICE_SOURCE_INCOMPLETE_UPSTREAM','availableFiles':files,'attributionPreserved':True,'disclaimerExact':True,'canonicalTemplateExact':True,'inventedHolderOrYear':False},
        'englishLeaks':{'checked':len(changed),'genuineLeaks':leaks,'acceptedExceptions':latin},
        'evidenceQuirk':{'label':'BETTER_RU_BSD_2_CLAUSE','actualLicense':'CC0-1.0','bucketCount':manifest['provenanceSummary']['countsBySourceType']['BETTER_RU_BSD_2_CLAUSE'],'classification':'EVIDENCE_LABEL_ONLY','runtimeImpact':'NONE','licenseUseImpact':'NONE'},
    }

def run():
    before = precheck()
    TEMP.mkdir(parents=True, exist_ok=True)
    if not (TEMP/'precheck.json').exists():
        write(TEMP/'precheck.json', before)
    atd_en = strict_dictionary((ROOT/'src/main/resources/assets/achievetodo/lang/en_us.json').read_bytes())
    atd_ru = strict_dictionary((ROOT/'src/main/resources/assets/achievetodo/lang/ru_ru.json').read_bytes())
    ru = strict_dictionary((ROOT/RU_PATH).read_bytes())
    old = strict_dictionary(git('show', 'HEAD:'+RU_PATH))
    assert set(atd_en) == set(atd_ru) and len(atd_en) == 533
    assert not [k for k,v in ru.items() if not v.strip()]
    index = load(ROOT/'.gradle-user-home/caches/fabric-loom/assets/indexes/26.2-32.json')
    vanilla_id = index['objects']['minecraft/lang/ru_ru.json']['hash']
    vanilla_path = ROOT/'.gradle-user-home/caches/fabric-loom/assets/objects'/vanilla_id[:2]/vanilla_id
    vanilla_bytes = vanilla_path.read_bytes()
    assert hashlib.sha1(vanilla_bytes).hexdigest() == vanilla_id
    vanilla_ru = strict_dictionary(vanilla_bytes)
    mc = ROOT/'.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-84afe0508c/26.2/minecraft-merged-84afe0508c-26.2.jar'
    vanilla_pack = pack_zip(mc, 'VANILLA')
    with zipfile.ZipFile(mc) as z:
        vanilla_en = strict_dictionary(z.read('assets/minecraft/lang/en_us.json'))
    converted = {}
    inputs = []
    identities = load(PHASE/'b5_7_structural_reconciliation.json')['acquisition']['archiveIdentityValidation']
    prior_inputs = {r['enum']:r for r in load(ROOT/'build/tmp/phase_b_b7/independent_extraction.json')['inputs']}
    for source in load(ROOT/'build/tmp/phase_b_b5_6/converted_sources.json'):
        source_path = pathlib.Path(source['source']); output_path = pathlib.Path(source['output'])
        r18_outputs = {'BACAP':'main-r18.zip', 'BACAP_HARDCORE':'hardcore-r18.zip',
                       'BACAP_TERRALITH':'BACAP_TERRALITH-r18.zip',
                       'BACAP_AMPLIFIED_NETHER':'BACAP_AMPLIFIED_NETHER-r18.zip',
                       'BACAP_NULLSCAPE':'BACAP_NULLSCAPE-r18.zip'}
        if source['enum'] in r18_outputs:
            output_path = ROOT/'build/tmp/phase_b_b7_r1'/r18_outputs[source['enum']]
        source_bytes = source_path.read_bytes()
        assert sha(source_bytes) == source['sourceSha256']
        rel = source_path.relative_to(ROOT).as_posix()
        assert identities[rel]['sha256'] == source['sourceSha256']
        with zipfile.ZipFile(output_path) as z:
            marker_name = 'achievetodo_compatibility/compat_26_2.properties'
            if marker_name in z.namelist():
                marker = z.read(marker_name).decode('utf8')
                expected_revision = 'compat_26_2_r18' if source['enum'] in r18_outputs else 'compat_26_2_r17'
                assert 'version='+expected_revision in marker and 'sourceSha1='+source['sourceSha1'] in marker
                if source['enum'] not in r18_outputs:
                    # These accepted worldgen payload artifacts are historical
                    # extraction inputs. R1 changes marker metadata only.
                    assert source['enum'] in ('TERRALITH','AMPLIFIED_NETHER','NULLSCAPE')
                    assert sha(output_path.read_bytes()) == prior_inputs[source['enum']]['convertedSha256']
            else:
                assert source['enum'] in ('TERRALITH','AMPLIFIED_NETHER','NULLSCAPE')
                assert sha(output_path.read_bytes()) == sha(source_bytes)
        converted[source['enum']] = pack_zip(output_path, source['enum'])
        inputs.append({'enum':source['enum'], 'sourcePath':rel, 'sourceSha256':sha(source_bytes), 'convertedSha256':sha(output_path.read_bytes()), 'markerVerified':marker_name in z.namelist(), 'byteIdenticalWorldgenPassthrough':source['enum'] in ('TERRALITH','AMPLIFIED_NETHER','NULLSCAPE')})
    optional = ['bacap_rewards_item','bacap_rewards_experience','bacap_rewards_trophy','bacap_cooperative_mode']
    definitions = {
        'main':['BACAP','bacap_override'],
        'hardcore':['BACAP','bacap_override','BACAP_HARDCORE','bacap_hardcore_override'],
        'terralith':['BACAP','bacap_override','TERRALITH','BACAP_TERRALITH','bacap_terralith_override'],
        'amplifiedNether':['BACAP','bacap_override','AMPLIFIED_NETHER','BACAP_AMPLIFIED_NETHER','bacap_amplified_nether_override'],
        'nullscape':['BACAP','bacap_override','NULLSCAPE','BACAP_NULLSCAPE','bacap_nullscape_override'],
    }
    combinations = {}; union = collections.defaultdict(list); companion_sets = {}
    for combination, layers in definitions.items():
        effective = dict(vanilla_pack)
        for layer in layers+optional:
            effective.update(converted[layer] if layer in converted else builtin(layer))
        keys = extract(effective)
        combinations[combination] = keys
        for key, contexts in keys.items():
            union[key].extend(dict(c, combination=combination) for c in contexts)
        if combination != 'main':
            companion_sets[combination] = sorted(extract(builtin(layers[-1])))
    for key in atd_en:
        union[key].append({'owner':'ATD_DICTIONARY_DECLARATION','path':'src/main/resources/assets/achievetodo/lang/en_us.json'})
    def provider(k):
        if k in atd_ru: return 'ATD_RU_DICTIONARY'
        if k in ru: return 'MINECRAFT_RU_OVERLAY'
        if k in vanilla_ru: return 'VANILLA_26_2_RU'
        if k in NEUTRAL: return 'INTENTIONALLY_LANGUAGE_NEUTRAL'
        return 'MISSING'
    providers = {k:provider(k) for k in sorted(union)}
    counts = dict(collections.Counter(providers.values()))
    cover = {c:{'required':len(keys), 'resolved':sum(provider(k)!='MISSING' for k in keys), 'missing':sorted(k for k in keys if provider(k)=='MISSING')} for c,keys in combinations.items()}
    mismatches = []; placeholder_records = []
    for key, prov in providers.items():
        if prov in ('MISSING','INTENTIONALLY_LANGUAGE_NEUTRAL'): continue
        if prov == 'ATD_RU_DICTIONARY': en, value = atd_en[key], atd_ru[key]
        elif prov == 'VANILLA_26_2_RU': en, value = vanilla_en.get(key,key), vanilla_ru[key]
        else: en, value = vanilla_en.get(key,key), ru[key]
        left, right = formats(en), formats(value)
        record = {'key':key, 'provider':prov, 'englishContract':left, 'russianContract':right, 'passed':left==right}
        placeholder_records.append(record)
        if left != right:
            mismatches.append(record)
    added = sorted(set(ru)-set(old)); removed = sorted(set(old)-set(ru))
    updated = sorted(k for k in set(old)&set(ru) if old[k]!=ru[k])
    retained = sorted(k for k in set(old)&set(ru) if old[k]==ru[k])
    handoff = load(PHASE/'b4_localization_handoff.json')
    report = {
        'implementationSha256':sha(pathlib.Path(__file__).read_bytes()),
        'inputs':inputs, 'layerOrder':definitions, 'optionalLayersConservativelyIncluded':optional,
        'vanillaRuSource':{'path':vanilla_path.relative_to(ROOT).as_posix(),'sha1':vanilla_id,'sha256':sha(vanilla_bytes),'keyCount':len(vanilla_ru)},
        'totalRequirements':len(union),'providerCounts':counts,'effectiveCoverage':cover,
        'requirements':[{ 'key':k, 'provider':providers[k], 'valueSha256': value_hash(atd_ru[k] if providers[k]=='ATD_RU_DICTIONARY' else ru[k] if providers[k]=='MINECRAFT_RU_OVERLAY' else vanilla_ru[k]) if providers[k] not in ('MISSING','INTENTIONALLY_LANGUAGE_NEUTRAL') else None, 'consumers':union[k]} for k in sorted(union)],
        'placeholderCertification':{'checked':len(placeholder_records), 'mismatches':mismatches, 'records':placeholder_records},
        'dictionaryDiff':{'preB6':len(old),'final':len(ru),'retained':len(retained),'added':added,'updated':updated,'removed':[{ 'key':k,'currentConsumers':union.get(k,[]),'vanillaKey':k in vanilla_en,'nativeAtdKey':k in atd_en} for k in removed]},
        'handoff':{'added':len(handoff['addedKeys']),'removed':len(handoff['removedKeys']),'changedReview':len(handoff['changedKeys']),'addedConsumed':sorted(set(handoff['addedKeys'])&set(union)),'changedConsumed':sorted(set(handoff['changedKeys'])&set(union)),'removedConsumed':sorted(set(handoff['removedKeys'])&set(union))},
        'companionOverlayKeys':companion_sets,
        'selectedCompanionCoverage':{k:providers.get(k,'MISSING') for k in handoff['selectedCompanionKeys']},
        'threeFormerEmptyValues':[{'key':k,'currentConsumerCount':len(union.get(k,[])), 'finalValueSha256':value_hash(ru[k]),'nonEmpty':bool(ru[k])} for k,v in old.items() if not v],
    }
    report['provenanceCertification'] = provenance_certification(ru, added+updated, report['requirements'])
    historical_path = PHASE/'b7_localization_certification_resumed.json'
    assert sha(historical_path.read_bytes()) == HISTORICAL_B7_CERTIFICATION_SHA256
    historical_certification = load(historical_path)
    historical_ref = historical_certification['independentCoverage']['extraction']
    assert sha((ROOT/historical_ref['path']).read_bytes()) == historical_ref['sha256']
    historical = load(ROOT/historical_ref['path'])
    historical_requirements = {r['key']:r for r in historical['requirements']}
    historical_keys = set(historical_requirements)
    current_keys = set(union)
    retired = set(RETIRED_CURRENT_CONSUMER_REPLACEMENTS)
    assert len(historical_requirements) == len(historical['requirements']) == historical['totalRequirements'] == 4304
    assert historical_keys-current_keys == retired
    assert current_keys-historical_keys == set()
    assert len(union) == sum(CURRENT_EXPECTED_PROVIDER_COUNTS.values()) == 4301
    assert counts == CURRENT_EXPECTED_PROVIDER_COUNTS
    historical_counts = {'ATD_RU_DICTIONARY':533,'MINECRAFT_RU_OVERLAY':3435,'VANILLA_26_2_RU':325,'INTENTIONALLY_LANGUAGE_NEUTRAL':11}
    assert historical['providerCounts'] == historical_counts
    assert {k:counts[k]-historical_counts[k] for k in counts} == {
        'ATD_RU_DICTIONARY':0,'MINECRAFT_RU_OVERLAY':-3,'VANILLA_26_2_RU':0,'INTENTIONALLY_LANGUAGE_NEUTRAL':0}
    assert all(providers[k] == historical_requirements[k]['provider'] for k in current_keys)
    assert set(combinations) == set(HISTORICAL_EXPECTED_VIEW_COUNTS) == set(CURRENT_EXPECTED_VIEW_COUNTS)
    view_deltas = {}
    for view, keys in combinations.items():
        historical_view = {k for k,r in historical_requirements.items()
                           if any(c.get('combination') == view for c in r['consumers'])}
        current_view = set(keys)
        assert len(historical_view) == historical['effectiveCoverage'][view]['required'] == HISTORICAL_EXPECTED_VIEW_COUNTS[view]
        assert historical_view-current_view == retired and not current_view-historical_view
        assert cover[view] == {'required':CURRENT_EXPECTED_VIEW_COUNTS[view],
                               'resolved':CURRENT_EXPECTED_VIEW_COUNTS[view], 'missing':[]}
        view_deltas[view] = {'historicalOnly':sorted(retired), 'currentOnly':[], 'delta':-3}
    historical_placeholders = {r['key'] for r in historical['placeholderCertification']['records']}
    current_placeholders = {r['key'] for r in placeholder_records}
    assert len(historical_placeholders) == historical['placeholderCertification']['checked'] == 4293
    assert historical_placeholders-current_placeholders == retired and not current_placeholders-historical_placeholders
    assert len(current_placeholders) == len(placeholder_records) == 4301-11 == 4290
    r2 = load(PHASE/'b8_r2_message_binding_repair.json')
    description_repairs = [r for r in r2['repairs'] if r['field'] == 'description.translate']
    assert {r['old']:r['new'] for r in description_repairs} == RETIRED_CURRENT_CONSUMER_REPLACEMENTS
    ownership = {r['key']:r for r in report['provenanceCertification']['changedValueRecords']}
    assert not set(ownership) & set(retained)
    ownership.update({k:{'owner':'RETAINED_PHASE_A','finalValueSha256':value_hash(old[k])} for k in retained})
    assert set(ownership) == set(ru) and len(ownership) == 3482
    placeholder_by_key = {r['key']:r for r in placeholder_records}
    replacements = []
    for repair in description_repairs:
        old_key, new_key = repair['old'], repair['new']
        assert historical_requirements[old_key]['provider'] == 'MINECRAFT_RU_OVERLAY'
        assert old_key in ru and old_key not in union
        assert new_key in historical_keys and new_key in current_keys
        assert providers[new_key] == 'MINECRAFT_RU_OVERLAY' and ru[new_key].strip()
        assert ownership[new_key]['finalValueSha256'] == value_hash(ru[new_key])
        assert placeholder_by_key[new_key]['passed']
        current_message_keys = set(command_keys((ROOT/repair['path']).read_text(encoding='utf8')))
        assert new_key in current_message_keys and old_key not in current_message_keys
        replacements.append({'old':old_key,'new':new_key,'oldAvailableInRu':True,'oldCurrentConsumers':0,
                             'newCurrentConsumers':len(union[new_key]),'newProvider':providers[new_key],
                             'newPlaceholderPassed':True,'newProvenanceOwner':ownership[new_key]['owner']})
    potion = next(r for r in r2['repairs'] if r['path'].endswith('/potion/root.mcfunction'))
    potion_before = ROOT/'build/tmp/phase_b_b8_r2/before/root.mcfunction'
    assert sha(potion_before.read_bytes()) == potion['beforeSha256']
    assert set(command_keys(potion_before.read_text(encoding='utf8'))) == set(
        command_keys((ROOT/potion['path']).read_text(encoding='utf8')))
    assert len(report['selectedCompanionCoverage']) == 53
    report['currentConsumerSetReconciliation'] = {
        'historicalCertifiedExtraction':historical_ref,'historicalRequirements':4304,'currentRequirements':4301,
        'historicalOnly':sorted(retired),'currentOnly':[],'unexpectedRemoved':[],'unexpectedAdded':[],
        'providerDelta':{'ATD_RU_DICTIONARY':0,'MINECRAFT_RU_OVERLAY':-3,'VANILLA_26_2_RU':0,'INTENTIONALLY_LANGUAGE_NEUTRAL':0},
        'effectiveViewDeltas':view_deltas,'placeholderRemoved':sorted(retired),'placeholderAdded':[],
        'replacements':replacements,'potionTranslationKeySetUnchanged':True,
    }
    assert len(ru) == 3482 and (len(retained),len(added),len(updated),len(removed))==(3186,286,10,32)
    assert not mismatches
    assert all(not r['currentConsumers'] and not r['vanillaKey'] and not r['nativeAtdKey'] for r in report['dictionaryDiff']['removed'])
    assert all(not c['missing'] for c in cover.values())
    assert set(report['selectedCompanionCoverage'].values()).isdisjoint({'MISSING'})
    write(TEMP/'independent_extraction.json', report)
    print(json.dumps({k:report[k] for k in ['totalRequirements','providerCounts','effectiveCoverage']},ensure_ascii=False))
    print('Placeholder mismatches:',json.dumps(mismatches,ensure_ascii=False))
    print('Dictionary diff:',len(retained),len(added),len(updated),len(removed))
    return report

class ExtractionContractTests(unittest.TestCase):
    def test_historical_source_and_current_pin_are_distinct(self):
        historical = (ROOT/'reference/phase_a_preservation/files/final/bacap.zip').read_bytes()
        self.assertEqual('8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70', sha(historical))
        self.assertEqual('45b8bb0076bbf5b92fde7dc9590c6686937abbc0', hashlib.sha1(historical).hexdigest())
        current = (ROOT/"build/tmp/phase_b_b1/BlazeandCave's Advancements Pack 1.21.zip").read_bytes()
        self.assertEqual('14da3f07b5467e8b59ffc0253fd8212c938cd739', hashlib.sha1(current).hexdigest())
        enum_source = (ROOT/'src/main/java/com/diskree/achievetodo/client/ExternalPack.java').read_text()
        block = enum_source.split('BACAP(',1)[1].split('),',1)[0]
        self.assertIn(hashlib.sha1(current).hexdigest(), block)
        self.assertNotEqual(hashlib.sha1(historical).hexdigest(), hashlib.sha1(current).hexdigest())

    def test_current_and_conjunction_has_live_consumer_and_provenance(self):
        effective = pack_zip(ROOT/"build/tmp/phase_b_b1/BlazeandCave's Advancements Pack 1.21.zip", 'BACAP')
        effective.update(builtin('bacap_override'))
        requirements = extract(effective)
        self.assertIn('and', requirements)
        self.assertEqual({'data/blazeandcave/function/config.mcfunction',
                          'data/blazeandcave/function/msg/intro.mcfunction',
                          'data/blazeandcave/function/msg/welcome.mcfunction'},
                         {c['path'] for c in requirements['and']})
        ru = strict_dictionary((ROOT/RU_PATH).read_bytes())
        self.assertEqual('и', ru['and'])
        self.assertEqual(formats('and'), formats(ru['and']))
        record = next(r for r in load(PHASE/'b6_ru_translation_manifest.json')['perChangedKeyProvenance'] if r['key']=='and')
        self.assertEqual(value_hash(ru['and']), record['finalValueSha256'])
        self.assertEqual('MANUAL_B6', record['sourceType'])

    def test_nested_snbt_component_and_escaped_apostrophe(self):
        self.assertEqual({'Wolf\'s reward'},set(command_keys('give @s stone[custom_name=\'{translate:"Wolf\\\'s reward"}\']')))

    def test_comments_and_generic_strings_are_not_consumers(self):
        pack = {'data/test/function/a.mcfunction':('test', b'# tellraw @s {"translate":"disabled"}\ntellraw @s {"text":"a command"}\ntellraw @s {"translate":"live"}\n')}
        self.assertEqual({'live'},set(extract(pack)))

    def test_json_dialogs_are_consumers(self):
        self.assertEqual({'dialog.prompt'},set(extract({'data/test/dialog/a.json':('test',b'{"title":{"translate":"dialog.prompt"}}')})))

    def test_placeholder_contract_detects_lost_repeated_argument(self):
        self.assertNotEqual(formats('%1$s %1$s'),formats('%1$s'))
        self.assertNotEqual(formats('%1$s %2$d'),formats('%2$s %1$d'))
        self.assertNotEqual(formats('%s %d'),formats('%d %s'))
        self.assertNotEqual(formats('%% %1$s'),formats('%1$s'))
        self.assertEqual(formats('%1$s %2$d'),formats('%2$d %1$s'))

    def test_duplicate_dictionary_rejected(self):
        with self.assertRaises(AssertionError):
            strict_dictionary(b'{"key":"one","key":"two"}')

    def test_current_effective_coverage_and_provenance(self):
        result = run()
        self.assertEqual(4290,result['placeholderCertification']['checked'])
        self.assertEqual(53,len(result['selectedCompanionCoverage']))

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--self-test', action='store_true')
    args = parser.parse_args()
    if args.self_test:
        unittest.main(argv=['b7'], verbosity=2)
    else:
        run()
