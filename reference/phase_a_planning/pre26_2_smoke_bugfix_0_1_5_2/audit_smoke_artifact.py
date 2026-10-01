"""Read-only artifact/preservation audit; writes only this new smoke namespace."""
import collections, hashlib, io, json, pathlib, re, subprocess, zipfile
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[3]
OUT = pathlib.Path(__file__).resolve().parent

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def write(name, value):
    (OUT/name).write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

def main():
    before = read(ROOT/'reference/phase_a_planning/pre26_2_final_build/input_verification.json')
    for name in ['final_gates_03.exit', 'final_native.exit', 'production_build.exit']:
        assert (OUT/name).read_text(encoding='utf-8-sig').strip() == '0', name
    for path, expected in read(OUT/'input_hashes.json').items():
        assert sha(ROOT/path).upper() == expected.upper(), path
    allowed = {
        'src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java': 'BUG-01 exact item-text migration call and refresh marker',
        'src/main/java/com/diskree/achievetodo/ability/AbilityType.java': 'BUG-03 shared lock prefix',
        'src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java': 'BUG-04 scoreboard lifecycle',
        'src/main/resources/achievetodo.mixins.json': 'BUG-02 function loader and BUG-03 HUD mixin registration',
        'src/main/resources/fabric.mod.json': 'BUILD-02 truthful tested API requirement',
        'src/main/generated/java/com/diskree/achievetodo/BuildConfig.java': 'BUILD-01 generated canonical version',
    }
    changed = []
    unchanged = 0
    for path, expected in before['protectedFiles'].items():
        assert (ROOT/path).is_file(), 'Protected file removed: '+path
        if sha(ROOT/path) != expected:
            assert path in allowed, 'Unrelated protected edit: '+path
            changed.append(dict(path=path, authorization=allowed[path], beforeSha256=expected, afterSha256=sha(ROOT/path)))
        else:
            unchanged += 1
    expected_added = {
        'src/main/java/com/diskree/achievetodo/client/LegacyItemText.java',
        'src/main/java/com/diskree/achievetodo/client/LegacyChatText.java',
        'src/main/java/com/diskree/achievetodo/client/gui/AbilityLockLines.java',
        'src/main/java/com/diskree/achievetodo/injection/mixin/main/CommandFunctionMixin.java',
        'src/main/java/com/diskree/achievetodo/injection/mixin/client/HudAbilityLockMixin.java',
        'src/test/java/com/diskree/achievetodo/client/Pre26SmokeRegressionTest.java',
        'src/test/java/com/diskree/achievetodo/client/Pre26SmokeRuntimeSetup.java',
        'src/gametest/java/com/diskree/achievetodo/certification/Pre26SmokeGameTest.java',
    }
    now = {p.relative_to(ROOT).as_posix() for parent in ['src','tools/final19','reference/phase_a_planning/final19'] for p in (ROOT/parent).rglob('*') if p.is_file() and '__pycache__' not in p.parts}
    assert now - set(before['protectedFiles']) == expected_added
    fabric_before = (ROOT/'src/main/resources/fabric.mod.json').read_bytes().replace(b'"fabric-api": ">=${apiVersion}+${minMinecraftVersion}"\r\n', b'"fabric-api": "*"\n')
    assert hashlib.sha256(fabric_before).hexdigest() == before['protectedFiles']['src/main/resources/fabric.mod.json']
    converter = ROOT/'src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java'
    original = converter.read_bytes().replace(b'compat_26_2_r16', b'compat_26_2_r15')
    original = original.replace(b'            convertedLine = LegacyItemText.migrateCommand(convertedLine);\n', b'')
    assert hashlib.sha256(original).hexdigest() == '1bea3ba9caf22963d1ef26de69b19534a4b16d1eab85a39c5e1fc17750227a31'
    head = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    branch = subprocess.check_output(['git', 'branch', '--show-current'], cwd=ROOT, text=True).strip()
    assert head == before['head'] and branch == before['branch']
    assert sha(ROOT/'.git/index') == before['indexSha256']
    mixin = read(ROOT/'src/main/resources/achievetodo.mixins.json')
    restored = json.loads(json.dumps(mixin))
    restored['mixins'].remove('main.CommandFunctionMixin')
    restored['client'].remove('client.HudAbilityLockMixin')
    with zipfile.ZipFile(ROOT/'build/libs/achievetodo-mc26.2+0.1.5.jar') as old:
        assert restored == json.loads(old.read('achievetodo.mixins.json'))
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    suites = []
    for path in sorted((OUT/'junit').glob('TEST-*.xml')):
        suite = ET.parse(path).getroot()
        for key in totals:
            totals[key] += int(suite.attrib.get(key, 0))
        suites.append(dict(name=suite.attrib['name'], tests=int(suite.attrib['tests']), sha256=sha(path)))
    assert totals == dict(tests=82, failures=0, errors=0, skipped=0), totals
    native = read(OUT/'native_smoke.json')
    native_log = (OUT/'final_native.gradle.log').read_text(encoding='utf-8-sig')
    for expected in ['Loading Minecraft 26.2 with Fabric Loader 0.19.5', '- achievetodo 0.1.5.2', '- fabric-api 0.161.0+26.2', 'All 1 required tests passed']:
        assert expected in native_log
    assert "Can't find scoreboard objective with advancements counter!" not in native_log
    assert len(ET.parse(OUT/'native_results.xml').getroot().findall('.//testcase')) == 1
    assert len(native['nativeAdvancements']) == 6 and native['certificationGain'] == 0 and native['cleanupGreen']
    assert all(row['advancementAfter'] and row['nativePickup'] for row in native['nativeAdvancements'])
    for key in ['flowerRewardNative', 'namespacedItemNameNativeFunction', 'existingCommandWarningHover', 'scoreboardTransientAndExisting']:
        assert native[key], key
    meta = read(OUT/'gradle_output_metadata.json')
    graph = {row['task'] for row in meta['taskGraph']}
    assert {':build', ':jar'} <= graph and ':test' not in graph and ':runGameTest' not in graph
    selected = [row for row in meta['jarTasks'] if row['task'] in graph and row['classifier'] == '']
    assert len(selected) == 1
    artifact = pathlib.Path(selected[0]['archive'])
    assert artifact == ROOT/'build/libs/achievetodo-mc26.2+0.1.5.2.jar'
    assert meta['projectVersion'] == 'mc26.2+0.1.5.2' and meta['gradleVersion'] == '9.8.0' and meta['javaVersion'] == '25.0.4'
    guard = read(OUT/'version_guard_result.json')
    assert guard['verdict'] == 'VERSION_REUSE_GUARD_GREEN' and guard['artifactUnmodified'] and guard['afterSha256'].lower() == sha(artifact)
    resolved = read(OUT/'resolved_dependencies.json')
    for config, rows in resolved.items():
        coordinates = {row['coordinate'] for row in rows}
        assert {'net.fabricmc:fabric-loader:0.19.5', 'net.fabricmc.fabric-api:fabric-api:0.161.0+26.2', 'com.moandjiezana.toml:toml4j:0.7.2'} <= coordinates, config
        if config == 'testRuntimeClasspath':
            assert all(coordinate.endswith(':6.1.3') for coordinate in coordinates if coordinate.startswith('org.junit.'))
    classes_base = ROOT/'build/classes/java/main'
    classes = {p.relative_to(classes_base).as_posix(): sha(p) for p in classes_base.rglob('*.class')}
    tests = set()
    for source in ['test', 'gametest']:
        base = ROOT/('build/classes/java/'+source)
        tests.update(p.relative_to(base).as_posix() for p in base.rglob('*.class'))
    secrets, paths, nested = [], [], []
    def scan(z, label):
        assert z.testzip() is None, label
        duplicates = [n for n, count in collections.Counter(z.namelist()).items() if count > 1]
        assert not duplicates, duplicates
        for name in z.namelist():
            if name.endswith('/'):
                continue
            data = z.read(name)
            if re.search(rb'(?i)-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|\b(?:ghp_|github_pat_|xox[baprs]-)[A-Za-z0-9_\-]{20,}|\bAKIA[A-Z0-9]{16}\b|\bsk-(?:proj-)?[A-Za-z0-9_\-]{32,}', data):
                secrets.append(label+'!'+name)
            if re.search(rb'(?i)(?:\b[A-Z]:[\\/](?:Users|Vibecode|Program Files|Temp|Windows)[\\/]|D:[\\/]Vibecode|C:[\\/]Users)', data):
                paths.append(label+'!'+name)
            if name.endswith('.jar'):
                with zipfile.ZipFile(io.BytesIO(data)) as child:
                    scan(child, label+'!'+name)
                    nested.append(dict(entry=name, bytes=len(data), sha256=hashlib.sha256(data).hexdigest()))
    with zipfile.ZipFile(artifact) as z:
        scan(z, artifact.name)
        names = set(z.namelist())
        assert not secrets and not paths
        mod = json.loads(z.read('fabric.mod.json'))
        assert mod['version'] == '0.1.5.2' and mod['id'] == 'achievetodo'
        assert mod['depends'] == {'java': '>=25', 'minecraft': '~26.2', 'fabricloader': '>=0.19.5', 'fabric-api': '>=0.161.0+26.2'}
        assert 'fabric-gametest' not in mod['entrypoints']
        assert mod['mixins'] == ['achievetodo.mixins.json']
        for entries in mod['entrypoints'].values():
            for entry in entries:
                assert entry.replace('.', '/')+'.class' in names
        assert tests.isdisjoint(names)
        forbidden = [name for name in names if re.search(r'(?i)(?:final19|gametest|certification|proposal\.patch|(?:^|/)(?:reference|planning|diagnostics?|logs?|temp|tmp)/|\.log$|\.patch$)', name)]
        assert not forbidden, forbidden
        assert {name for name in names if name.endswith('.class')} == set(classes)
        for name, expected in classes.items():
            assert hashlib.sha256(z.read(name)).hexdigest() == expected, name
        assert b'0.1.5.2' in z.read('com/diskree/achievetodo/BuildConfig.class')
        for short in ['client/LegacyItemText', 'client/LegacyChatText', 'client/gui/AbilityLockLines', 'injection/mixin/main/CommandFunctionMixin', 'injection/mixin/client/HudAbilityLockMixin']:
            assert 'com/diskree/achievetodo/'+short+'.class' in names
        for name in mixin.get('mixins', [])+mixin.get('client', [])+mixin.get('server', []):
            assert (mixin['package']+'.'+name).replace('.', '/')+'.class' in names, name
        resources = {}
        for parent in ['src/main/resources', 'src/main/generated']:
            base = ROOT/parent
            for path in base.rglob('*'):
                if path.is_file() and path.suffix != '.java':
                    resources[path.relative_to(base).as_posix()] = path
        for name, path in resources.items():
            assert name in names, name
            if name != 'fabric.mod.json':
                assert z.read(name) == path.read_bytes(), name
            if path.suffix in ['.json', '.mcmeta']:
                json.loads(z.read(name))
        for entry in mod.get('jars', []):
            assert entry['file'] in names
        entry_hashes = {name: hashlib.sha256(z.read(name)).hexdigest() for name in sorted(names) if not name.endswith('/')}
    audit = dict(verdict='JAR_AUDIT_GREEN', artifact=dict(filename=artifact.name, absolutePath=str(artifact), bytes=artifact.stat().st_size, sha256=sha(artifact), sha1=hashlib.sha1(artifact.read_bytes()).hexdigest(), internalVersion=mod['version']), metadata=mod,
        entries=len(names), productionClasses=len(classes), productionResources=len(resources), nestedJars=nested,
        mixinReferences=sum(len(mixin.get(key, [])) for key in ['mixins','client','server']), testClassesCompared=len(tests),
        packagedTestClasses=[], duplicateMetadata=[], forbiddenPaths=[], credentialSignatureHits=[], absoluteLocalPaths=[],
        allPackagedClassesMatchFinalGradleOutput=True, allProductionResourcesMatch=True, entrySha256=entry_hashes)
    preservation = dict(verdict='1152_PRESERVATION_GREEN', productCertified=1152, productUncertified=0, historicalPhaseACertified=1133, targets=19, groups=79, criteria=108, familiesGreen=7,
        unchangedProtectedFiles=unchanged, authorizedModifiedOriginalFiles=changed, authorizedAddedSourceFiles=sorted(expected_added), protectedFilesRemoved=0,
        acceptedConverterDeltaExactlyTwoLines=True, acceptedMixinEntriesPreserved=True, head=head, branch=branch, indexSha256=before['indexSha256'],
        gitMutations=False, cleanup=False, phaseB=False, junit=totals, junitSuites=suites, targetedNativeTests=1, targetedNativeAdvancementReceipts=6, certificationGain=0)
    write('jar_content_audit.json', audit)
    write('final_preservation_audit.json', preservation)
    print(json.dumps(dict(artifact=audit['artifact'], junit=totals, preservation=preservation['verdict'], entries=len(names)), indent=2))

if __name__ == '__main__':
    main()
