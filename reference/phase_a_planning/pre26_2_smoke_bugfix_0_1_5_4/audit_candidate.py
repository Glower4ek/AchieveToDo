import hashlib, json, pathlib, re, shutil, zipfile

ROOT = pathlib.Path(__file__).resolve().parents[3]
PASS = pathlib.Path(__file__).resolve().parent
TMP = ROOT / 'build/tmp/pre26_2_smoke_bugfix_0_1_5_4'
sha = lambda data: hashlib.sha256(data).hexdigest()
before = json.loads((TMP / 'before_hashes.json').read_text(encoding='utf-8-sig'))
allowed = {
    'gradle.properties',
    'src/main/generated/java/com/diskree/achievetodo/BuildConfig.java',
    'src/main/java/com/diskree/achievetodo/client/AchieveToDoClient.java',
    'src/main/java/com/diskree/achievetodo/injection/mixin/client/GuiMixin.java',
    'src/main/java/com/diskree/achievetodo/injection/mixin/client/MinecraftClientMixin.java',
    'src/main/java/com/diskree/achievetodo/injection/mixin/client/MovementTutorialStepHandlerMixin.java',
    'src/main/java/com/diskree/achievetodo/injection/mixin/main/CommandManagerMixin.java',
    'src/main/resources/achievetodo.mixins.json',
    'src/main/resources/assets/achievetodo/lang/en_us.json',
    'src/main/resources/assets/achievetodo/lang/ru_ru.json',
    'src/test/java/com/diskree/achievetodo/injection/mixin/client/MovementTutorialStepHandlerMixinTest.java',
}
changed, preserved = [], 0
for name, original in before.items():
    path = ROOT / name
    after = sha(path.read_bytes()) if path.exists() else None
    if after != original:
        assert name in allowed, ('Unexpected source/evidence delta', name)
        changed.append(dict(path=name, beforeSha256=original, afterSha256=after))
    else:
        preserved += 1
new_files = [
    'src/main/java/com/diskree/achievetodo/client/AdvancementsTutorialProgress.java',
    'src/main/java/com/diskree/achievetodo/client/AdvancementLinkCommand.java',
    'src/main/java/com/diskree/achievetodo/client/OptionalAdvancementSearch.java',
    'src/test/java/com/diskree/achievetodo/client/AdvancementLinkCommandTest.java',
]
for name in new_files:
    assert name not in before
    changed.append(dict(path=name, beforeSha256=None, afterSha256=sha((ROOT/name).read_bytes())))

old = ROOT / 'build/libs/achievetodo-mc26.2+0.1.5.3.jar'
new = ROOT / 'build/libs/achievetodo-mc26.2+0.1.5.4.jar'
assert sha(old.read_bytes()) == 'c2554cd44d95e921e2c4a7a7b4b1db3b0abc6c7903c84b3baf2add3e0570bab0'
with zipfile.ZipFile(old) as prior, zipfile.ZipFile(new) as jar:
    names = jar.namelist()
    assert len(names) == len(set(names))
    metadata = json.loads(jar.read('fabric.mod.json'))
    expected = json.loads(prior.read('fabric.mod.json'))
    expected['version'] = '0.1.5.4'
    assert metadata == expected
    assert metadata['authors'] == ['Glower4ek']
    assert metadata['contact']['sources'] == 'https://github.com/Glower4ek/AchieveToDo'
    assert b'Copyright (c) 2026 Glower4ek' in jar.read('LICENSE_achievetodo')
    assert jar.read('LICENSE_achievetodo') == prior.read('LICENSE_achievetodo')
    build_config = 'com/diskree/achievetodo/BuildConfig.class'
    assert jar.read(build_config) == prior.read(build_config).replace(b'0.1.5.3', b'0.1.5.4')
    mixins = json.loads(jar.read('achievetodo.mixins.json'))
    mixin_count = 0
    for side in ['mixins', 'client', 'server']:
        for name in mixins.get(side, []):
            mixin_count += 1
            assert (mixins['package']+'.'+name).replace('.', '/')+'.class' in names
    for entries in metadata['entrypoints'].values():
        for name in entries:
            assert name.replace('.', '/')+'.class' in names
    leak = [n for n in names if re.search(r'(^|/)(reference|planning|debug|gametest|certification|test)(/|\.)|Test\.class$|GameTest|Pre26Smoke|AuthorIdentity', n, re.I)]
    assert not leak, leak
    classes = [n for n in names if n.endswith('.class')]
    assert all(n.startswith('com/diskree/') for n in classes)
    assert not any(n.startswith('io/github/diskria/') for n in names)
    assert not any(b'io/github/diskria/advancements_search' in jar.read(n)
                   or b'io/github/diskria/advancements_fullscreen' in jar.read(n) for n in classes)
    compiled = ROOT/'build/classes/java/main'
    compiled_classes = {p.relative_to(compiled).as_posix() for p in compiled.rglob('*.class')}
    assert set(classes) == compiled_classes
    for name in classes:
        assert jar.read(name) == (compiled/name).read_bytes()
    resources = ROOT/'build/resources/main'
    for path in resources.rglob('*'):
        if path.is_file():
            name = path.relative_to(resources).as_posix()
            if name == 'fabric.mod.json':
                processed = json.loads(path.read_bytes())
                packaged = json.loads(jar.read(name))
                packaged.pop('jars')
                assert processed == packaged, 'Only Loom nested-library metadata may differ from processed resources'
            else:
                assert jar.read(name) == path.read_bytes(), name
    old_names = set(prior.namelist())
    removed = sorted(old_names-set(names))
    added = sorted(set(names)-old_names)
    assert removed == ['com/diskree/achievetodo/injection/mixin/main/CommandManagerMixin.class']
    assert all(n.startswith('com/diskree/achievetodo/client/') and n.endswith('.class') for n in added)
    changed_entries = sorted(n for n in old_names.intersection(names) if prior.read(n) != jar.read(n))
    assert set(changed_entries) == {
        build_config, 'fabric.mod.json', 'achievetodo.mixins.json',
        'assets/achievetodo/lang/en_us.json', 'assets/achievetodo/lang/ru_ru.json',
        'com/diskree/achievetodo/client/AchieveToDoClient.class',
        'com/diskree/achievetodo/injection/mixin/client/GuiMixin.class',
        'com/diskree/achievetodo/injection/mixin/client/MinecraftClientMixin.class',
        'com/diskree/achievetodo/injection/mixin/client/MovementTutorialStepHandlerMixin.class',
    }, changed_entries
    for locale in ['en_us', 'ru_ru']:
        name = f'assets/achievetodo/lang/{locale}.json'
        old_language = json.loads(prior.read(name))
        new_language = json.loads(jar.read(name))
        assert {k:v for k,v in new_language.items() if k in old_language} == old_language
        assert set(new_language)-set(old_language) == {'achievetodo.advancement_navigation_unavailable'}
    for name in names:
        if name.endswith('.jar'):
            assert jar.read(name) == prior.read(name), name
    artifact = dict(path=str(new), bytes=new.stat().st_size, sha256=sha(new.read_bytes()),
        version=metadata['version'], metadata=metadata, entries=len(names), classes=len(classes),
        mixinReferences=mixin_count, addedEntries=added, removedEntries=removed, changedEntries=changed_entries,
        productResourceLoss=False, hardOptionalDependency=False, bundledOptionalImplementation=False,
        leakage=[], mainOutputByteEquivalent=True)

for path in TMP.iterdir():
    if path.is_file() and path.name not in ['before_hashes.json']:
        shutil.copy2(path, PASS/path.name)
shutil.copytree(TMP/'baseline-junit', PASS/'baseline-junit', dirs_exist_ok=True)
terminal = json.loads((ROOT/'reference/phase_a_planning/final19/terminal_reconciliation.json').read_text())
assert terminal['productCertified'] == 1152 and terminal['productUncertified'] == 0
preservation = dict(protectedFilesPreserved=preserved, changedFiles=changed,
    terminal=terminal['verdict'], productCertified=1152, productUncertified=0,
    historicalPhaseACertified=terminal['historicalPhaseACertified'],
    gitIndexSha256=sha((ROOT/'.git/index').read_bytes()), gitMutations=False,
    priorCandidateJarPreserved=True, fullscreenProductPatched=False)
(PASS/'jar_audit.json').write_text(json.dumps(artifact, indent=2)+'\n', encoding='utf-8')
(PASS/'preservation_audit.json').write_text(json.dumps(preservation, indent=2)+'\n', encoding='utf-8')
print(json.dumps(dict(artifact=artifact, preservation=preservation), indent=2))
