from tools.phase_b.b7_localization_certification import *
import os
from tools.build.verify_alternative_r3 import verify
def bind(original,root):
    verify(ROOT,root,os.environ['ACHIEVETODO_ALTERNATIVE_MANIFEST'],os.environ['ACHIEVETODO_INPUT_AUTHORITY'])
    original.provenance_certification=provenance_certification
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
        metadata = load(b6temp/(prefix+'_metadata.s5-r3-v1.json'))
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
        source_identities.append({'projectId':project, 'versionId':version['id'], 'version':version['version_number'], 'license':metadata['project']['license']['id'], 'archiveSha256':sha(archive), 'metadataSha256':sha((b6temp/(prefix+'_metadata.s5-r3-v1.json')).read_bytes()), 'sourceDuplicateKeys':duplicate_keys, 'sourceDuplicatePolicy':'Last declaration wins, matching runtime JSON parsing; final product dictionary separately rejects duplicates.'})
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
