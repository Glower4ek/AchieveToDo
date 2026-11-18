"""Separate raw-capture authority. Candidate inspection is NEVER activation.

The empty Owner B pin is intentional: Owner A cannot approve unknown response
bytes. No environment variable, candidate report or editable draft can fill it.
"""
from pathlib import Path, PurePosixPath
from datetime import datetime, timezone
import hashlib
import importlib.util
import json
import os
import stat
import struct
import zipfile

AUTHORITY = 'ATD_DISTRIBUTION_B_V2_RAW_CAPTURE_V1'
POLICY_PATH = 'tools/certification/authority/distribution-b-v2-policy.json'
CAPSULE_PATH = 'tools/certification/authority/distribution-b-v2-approval.json'
POLICY_SHA256 = '218d064203060cb6d49b798c0dc3e12617313cad597b1c0848b4ef9b6bff13c3'
OWNER_B_CAPSULE_SHA256 = '39116d736a8c352aca6a05453474f0fae965cc300a075fdabdbf266523a3f9f9'  # Intentionally inactive; requires separate reviewed Owner B implementation.
UI07_SUCCESSOR_SHA256 = '57e44e6d13df96fb427c4509576fbcb87061eeef377d47d50d070016acdafe2e'
UI07_MANIFEST = 'tools/certification/authority/ui07-successor-v1.json'
UI07_MODULE = 'tools/build/ui07_successor.py'
GAMEPLAY_SUCCESSOR_SHA256 = '64b9f7cefac0b862366e5b6988e0e95847986fcf7868e4734327790b7e838693'
GAMEPLAY_MANIFEST = 'tools/certification/authority/gameplay-successor-v1.json'
GAMEPLAY_V2_SUCCESSOR_SHA256 = '522fecccba5e2c3f657afe2db0bc2b2db54c93d674c95206a2061bf695c48bab'
GAMEPLAY_V2_MANIFEST = 'tools/certification/authority/gameplay-successor-v2.json'
GAMEPLAY_V2_MODULE = 'tools/build/gameplay_successor_v2.py'
GAMEPLAY_V3_SUCCESSOR_SHA256 = '179a8fb1fce386cfd4f78aa4d96e5f6dfab94701fac35a2f27ccbb17b78ed44c'
GAMEPLAY_V3_MANIFEST = 'tools/certification/authority/gameplay-successor-v3.json'
GAMEPLAY_V3_MODULE = 'tools/build/gameplay_successor_v3.py'
GAMEPLAY_V4_SUCCESSOR_SHA256 = 'e069f6109cbbf2dd679859b10e814d987e77ba8c9c9338899e13be9b4abcce53'
GAMEPLAY_V4_MANIFEST = 'tools/certification/authority/gameplay-successor-v4.json'
GAMEPLAY_V4_MODULE = 'tools/build/gameplay_successor_v4.py'
GAMEPLAY_MODULE = 'tools/build/gameplay_successor.py'
SELECTOR = 'ACHIEVETODO_GATE_B_AUTHORITY'
APPROVAL_FILE = 'ACHIEVETODO_GATE_B_APPROVAL_FILE'
HISTORICAL_MANIFEST = 'src/test/resources/build_inputs/manifest.json'
HISTORICAL_VERIFIER = 'tools/build/verify_inputs.py'
GATE_A_MANIFEST = 'tools/certification/authority/ui-ux-maintenance-v1.json'
OVERLAY_PATHS = frozenset({
    'src/test/java/com/diskree/achievetodo/certification/UiUxMaintenanceGateContractTest.java',
})
FIXTURE_PATH = 'src/test/java/com/diskree/achievetodo/client/PhaseBPackTestFixtures.java'
OWNER_A_FIXTURE_BASELINE_SHA256 = '6e9877b677eccfeb8837e5211d3f5ebd06b20c4b8a100b3e37c058dc7fa84caf'
REVIEW_CODE_PATHS = frozenset({
    'tools/build/acquire_distribution_b_v2.py',
    'tools/build/verify_inputs_v2.py',
    'tools/phase_b/v2_localization_authority.py',
    'tools/certification/gate-b-v2.init.gradle',
    'src/test/java/com/diskree/achievetodo/certification/GateBInputAuthorityContractTest.java',
    'tools/build/tests/test_distribution_b_v2_contract.py',
    FIXTURE_PATH,
    *OVERLAY_PATHS,
})
RUNTIME_TRUST_ANCHOR_PATHS = frozenset({'tools/build/distribution_b_v2_contract.py'})


def require(ok, message):
    if not ok:
        raise ValueError('GATE_B_V2_REJECTED: ' + message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def _unique(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, 'duplicate JSON key: ' + key)
        result[key] = value
    return result


def strict_json(data):
    require(isinstance(data, bytes), 'raw JSON bytes required')
    return json.loads(data.decode('utf-8'), object_pairs_hook=_unique,
                      parse_constant=lambda value: require(False, 'non-finite JSON: ' + value))


def _no_links(path):
    for item in [path, *path.parents]:
        require(not item.is_symlink() and not (hasattr(item, 'is_junction') and item.is_junction()), 'symlink/junction: ' + str(item))


def safe_root(root, source=None):
    root = Path(root)
    require(root.is_absolute() and root.is_dir(), 'absolute existing root required')
    _no_links(root)
    require(root.resolve() == root, 'noncanonical root')
    if source is not None:
        require(not root.is_relative_to(Path(source).resolve()), 'root inside source')
        require(not any(p.lower() in ('build', '.gradle', '.gradle-user-home', 'reference')
                        or p.lower().startswith(('phase_b_', 'phase_c_')) for p in root.parts), 'forbidden external root ancestor')
    return root


def safe_file(root, relative):
    root = Path(root).absolute()
    rel = PurePosixPath(relative)
    require(relative and not rel.is_absolute() and '\\' not in relative and ':' not in relative
            and '..' not in rel.parts and '.' not in rel.parts, 'unsafe relative path: ' + relative)
    path = root.joinpath(*rel.parts)
    _no_links(path)
    require(path.is_file() and path.resolve().is_relative_to(root.resolve()), 'missing/escaping file: ' + relative)
    info = path.stat()
    require(stat.S_ISREG(info.st_mode) and info.st_nlink == 1, 'nonregular/hardlinked file: ' + relative)
    return path


def policy(source):
    raw = safe_file(source, POLICY_PATH).read_bytes()
    require(sha(raw) == POLICY_SHA256, 'policy digest')
    value = strict_json(raw)
    require(value['schema'] == 1 and value['authority'] == AUTHORITY, 'policy identity')
    require(value['automaticDownload'] is False and value['automaticFallback'] is False, 'automatic acquisition/fallback')
    require(len(value['sources']) == 5 and len(value['originals']) == 15 and value['payloadCardinality'] == 25, 'policy cardinality')
    sources = value['sources']
    require(len({x['prefix'] for x in sources}) == len({x['projectId'] for x in sources})
            == len({x['versionId'] for x in sources}) == 5, 'duplicate source identity')
    names = [x['logicalName'] for x in value['originals']]
    for row in sources:
        require(row['projectEndpoint'] == 'https://api.modrinth.com/v2/project/'+row['projectId']
                and row['versionEndpoint'] == 'https://api.modrinth.com/v2/version/'+row['versionId'], 'fixed endpoint identity')
        require(row['projectRawName'] == 'project-'+row['projectId']+'.v2.raw.json'
                and row['versionRawName'] == 'version-'+row['versionId']+'.v2.raw.json', 'raw response identity')
        names.extend((row['projectRawName'], row['versionRawName']))
    require(len(names) == len(set(x.casefold() for x in names)) == 25, 'duplicate payload name')
    return value


def inherited_authority(source):
    p = policy(source)
    require(sha(safe_file(source, HISTORICAL_MANIFEST).read_bytes()) == p['historicalManifestSHA256'], 'historical manifest digest')
    require(sha(safe_file(source, HISTORICAL_VERIFIER).read_bytes()) == p['historicalVerifierSHA256'], 'historical verifier digest')
    spec = importlib.util.spec_from_file_location('atd_original_distribution_b_verifier', Path(source)/HISTORICAL_VERIFIER)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    original = module.authority(Path(source))  # All eleven original source pins remain mandatory.
    require(len(original['sourceFiles']) == 11 and len(original['dependencyInputs']) == 372, 'historical closure cardinality')
    require([r for r in original['entries'] if r['type'] != 'metadataJSON'] == p['originals'], 'original fifteen substitution')
    return original


def check_identity(root, row, name_key='logicalName', hash_key='SHA256'):
    path = safe_file(root, row[name_key])
    raw = path.read_bytes()
    require(type(row['size']) is int and len(raw) == row['size'], 'size: ' + row[name_key])
    require(sha(raw) == row[hash_key], 'digest: ' + row[name_key])
    return raw


def metadata_pair_binding(project, version):
    return b'ATD_DISTRIBUTION_B_V2_RAW_PAIR\0' + struct.pack('>Q', len(project)) + project + struct.pack('>Q', len(version)) + version


def _strings(value, label):
    require(isinstance(value, list) and value and all(isinstance(x, str) and x for x in value)
            and len(value) == len(set(value)), 'nonempty unique strings: ' + label)
    return value


def validate_pair(source, row, project_bytes, version_bytes, acquisition, archive_bytes):
    require(len(project_bytes) <= 10485760 and len(version_bytes) <= 10485760, 'response size limit')
    project = strict_json(project_bytes)
    version = strict_json(version_bytes)
    require(isinstance(project, dict) and isinstance(version, dict), 'JSON object schema')
    require(project.get('id') == version.get('project_id') == acquisition.get('projectId') == row['projectId'], 'project identity')
    require(version.get('id') == acquisition.get('versionId') == row['versionId'], 'version identity')
    require(version.get('version_number') == acquisition.get('version') == row['versionLabel'], 'version label')
    require(version.get('date_published') == acquisition.get('publishedAt') == row['publishedAt'], 'version publication')
    versions = _strings(version.get('game_versions'), 'game_versions')
    require(versions == row['recordedGameVersions'] == acquisition.get('gameVersions'), 'recorded game compatibility')
    loaders = _strings(version.get('loaders'), 'loaders')
    require('minecraft' in loaders, 'translation resource-pack loader')
    require(isinstance(project.get('license'), dict) and isinstance(acquisition.get('license'), dict), 'license object schema')
    license_id = project['license'].get('id')
    require(license_id == acquisition.get('license', {}).get('id') == row['expectedLicenseId'], 'license conflict')
    require(type(version.get('files')) is list and version['files'], 'version files schema')
    names = [x.get('filename') for x in version['files'] if isinstance(x, dict)]
    require(len(names) == len(version['files']) and all(isinstance(x, str) and x for x in names)
            and len(names) == len(set(names)), 'duplicate/invalid archive filenames')
    chosen = [f for f in version['files'] if f['filename'] == row['originalArchiveFilename']]
    require(len(chosen) == 1, 'exact selected archive')
    record = chosen[0]
    require(record.get('url') == acquisition.get('url') == row['archiveURL'], 'archive URL binding')
    require(type(record.get('size')) is int and record['size'] == acquisition.get('size') == row['archiveBytes'] == len(archive_bytes), 'archive size binding')
    require(acquisition.get('filename') == row['originalArchiveFilename'], 'acquisition archive filename')
    require(acquisition.get('hashes') == row['archiveHashes'], 'original acquisition hash identities')
    returned = record.get('hashes')
    require(isinstance(returned, dict) and {'sha1', 'sha512'} <= set(returned), 'missing archive hashes')
    for algorithm, digest in {**row['archiveHashes'], **returned}.items():
        require(algorithm in ('sha1', 'sha256', 'sha512') and isinstance(digest, str), 'unsupported archive hash algorithm')
        require(hashlib.new(algorithm, archive_bytes).hexdigest() == digest, 'archive hash binding: ' + algorithm)
    for algorithm in set(returned) & set(row['archiveHashes']):
        require(returned[algorithm] == row['archiveHashes'][algorithm], 'metadata/acquisition hash conflict')
    if row['projectId'] == 'YX5bAAJN':
        permission = row['licensePermissionEvidence']
        raw = safe_file(source, permission['path']).read_bytes()
        require(sha(raw) == permission['SHA256'], 'permission evidence digest')
        evidence = strict_json(raw)
        lp = evidence['sources']['official_lp_1_21']
        require(lp['project_id'] == row['projectId'] and lp['version_id'] == row['versionId']
                and lp['version_number'] == row['versionLabel'] and lp['game_versions'] == ['26.2'], 'ARR permission identity/scope')
        require('direct permission' in evidence['provenance'] and 'Allowed for this project via direct permission' in lp['usage'], 'ARR permission missing')
    return {'metadata_view': {'project': project, 'versions': [version]},
            'raw_binding': {'projectSHA256': sha(project_bytes), 'versionSHA256': sha(version_bytes),
                            'bindingSHA256': sha(metadata_pair_binding(project_bytes, version_bytes)),
                            'bindingKind': 'V2_RAW_PAIR_BINDING'},
            'compatibility': {'gameVersions': versions, 'loaders': loaders, 'ownerBDispositionRequired': True},
            'license': {'id': license_id, 'ownerBReviewRequired': True}}


def _zip_check(raw, name):
    import io
    with zipfile.ZipFile(io.BytesIO(raw)) as archive:
        infos = archive.infolist()
        require(len(infos) <= 100000, 'ZIP entry limit: ' + name)
        names = [x.filename for x in infos]
        require(len(names) == len(set(x.casefold() for x in names)), 'ZIP duplicate/case collision: ' + name)
        total = 0
        for entry in infos:
            original_name = entry.orig_filename
            require(original_name == entry.filename and '\x00' not in original_name
                    and '\\' not in original_name, 'ZIP raw unsafe name: ' + name)
            rel = PurePosixPath(entry.filename)
            require(not rel.is_absolute() and '..' not in rel.parts and '\\' not in entry.filename
                    and ':' not in entry.filename, 'ZIP unsafe path: ' + name)
            mode = (entry.external_attr >> 16) & 0o170000
            require(mode != stat.S_IFLNK, 'ZIP symlink: ' + name)
            total += entry.file_size
            require(entry.file_size <= 512*1024*1024 and total <= 2*1024*1024*1024, 'ZIP expansion limit: ' + name)
        require(archive.testzip() is None, 'ZIP CRC: ' + name)


def inspect_candidate(source, root, policy_data=None):
    """Public candidate diagnostics must not silently ignore repair authority."""
    require(not any(k in os.environ for k in (JUNIT_REPAIR_SELECTOR,JUNIT_REPAIR_FILE)), "candidate diagnostic cannot ignore JUnit repair selectors")
    return _inspect_candidate_inputs(source,root,policy_data)


def _inspect_candidate_inputs(source, root, policy_data=None):
    """Pure candidate checks; never establishes approved raw-byte identities."""
    source = Path(source).absolute()
    p = policy(source) if policy_data is None else policy_data
    original = inherited_authority(source)
    root = safe_root(root, source)
    expected = {x['logicalName'] for x in p['originals']} | {x[k] for x in p['sources'] for k in ('projectRawName', 'versionRawName')}
    observed = [x.name for x in root.iterdir()]
    require(len(expected) == 25 and set(observed) == expected and len({x.casefold() for x in observed}) == 25, 'exact 25 payload names')
    identities = []
    for row in p['originals']:
        raw = check_identity(root, row)
        identities.append({'name': row['logicalName'], 'size': len(raw), 'SHA256': sha(raw), 'kind': 'HISTORICAL_ORIGINAL'})
        if row['type'] == 'originalZIP':
            _zip_check(raw, row['logicalName'])
    views, bindings, compatibility, licenses = {}, {}, {}, {}
    for row in p['sources']:
        project = safe_file(root, row['projectRawName']).read_bytes()
        version = safe_file(root, row['versionRawName']).read_bytes()
        acq = strict_json(safe_file(root, row['prefix']+'_acquisition.json').read_bytes())
        archive = safe_file(root, row['expectedArchiveName']).read_bytes()
        checked = validate_pair(source, row, project, version, acq, archive)
        views[row['prefix']] = checked['metadata_view']
        bindings[row['prefix']] = checked['raw_binding']
        compatibility[row['prefix']] = checked['compatibility']
        licenses[row['prefix']] = checked['license']
        for key, raw in [('projectRawName', project), ('versionRawName', version)]:
            identities.append({'name': row[key], 'size': len(raw), 'SHA256': sha(raw), 'kind': 'UNTRUSTED_RAW_CAPTURE'})
    for row in original['packSources']:
        raw = check_identity(root if row['authority'] == 'external' else source, row, 'path')
        require(hashlib.sha1(raw).hexdigest() == row['SHA1'], 'pack source SHA1')
    return {'scope': 'CANDIDATE_STRUCTURAL_CHECKS_ONLY', 'authority': AUTHORITY,
            'activated': False, 'originalGate': 'STANDARD_INPUT_BLOCKED', 'metadata_views': views,
            'raw_bindings': bindings, 'payloadIdentities': identities,
            'compatibility': compatibility, 'licenses': licenses}


def ui07_composition(source, env, gate_a):
    if env.get('ACHIEVETODO_UI07_AUTHORITY') is None and env.get('ACHIEVETODO_UI07_AUTHORITY_FILE') is None:
        require(env.get('ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY') is None and env.get('ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_FILE') is None, 'gameplay orphan predecessor')
        return gate_a['productionClosure'], []
    raw = safe_file(source, UI07_MANIFEST).read_bytes()
    require(sha(raw) == UI07_SUCCESSOR_SHA256, 'UI07 manifest digest')
    manifest = strict_json(raw)
    module_raw = safe_file(source, UI07_MODULE).read_bytes()
    require(sha(module_raw) == manifest['codeBindings'][UI07_MODULE], 'UI07 validator digest before execution')
    spec = importlib.util.spec_from_file_location('atd_ui07_successor', Path(source)/UI07_MODULE)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    production, paths = module.validate(source, env, UI07_SUCCESSOR_SHA256, gate_a)
    if env.get('ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY') is None and env.get('ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_FILE') is None:
        return production, paths
    if env.get('ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY') == 'ATD_PURPUR_GAMEPLAY_SOURCE_V4':
        raw_v4 = safe_file(source, GAMEPLAY_V4_MANIFEST).read_bytes()
        require(sha(raw_v4) == GAMEPLAY_V4_SUCCESSOR_SHA256, 'gameplay V4 manifest digest')
        v4 = strict_json(raw_v4)
        v4_path = safe_file(source, GAMEPLAY_V4_MODULE)
        v4_source = v4_path.read_bytes()
        require(sha(v4_source) == v4['codeBindings'][GAMEPLAY_V4_MODULE], 'gameplay V4 validator digest before execution')
        import types
        gameplay = types.ModuleType('atd_gameplay_successor_v4')
        gameplay.__file__ = str(v4_path)
        gameplay.__package__ = 'tools.build'
        exec(compile(v4_source, str(v4_path), 'exec', dont_inherit=True), gameplay.__dict__)
        production, added = gameplay.validate(source, env, GAMEPLAY_V4_SUCCESSOR_SHA256, production)
        return production, sorted(set(paths) | set(added))
    if env.get('ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY') == 'ATD_PURPUR_GAMEPLAY_SOURCE_V3':
        raw_v3 = safe_file(source, GAMEPLAY_V3_MANIFEST).read_bytes()
        require(sha(raw_v3) == GAMEPLAY_V3_SUCCESSOR_SHA256, 'gameplay V3 manifest digest')
        v3 = strict_json(raw_v3)
        require(sha(safe_file(source, GAMEPLAY_V3_MODULE).read_bytes()) == v3['codeBindings'][GAMEPLAY_V3_MODULE], 'gameplay V3 validator digest before execution')
        spec = importlib.util.spec_from_file_location('atd_gameplay_successor_v3', Path(source)/GAMEPLAY_V3_MODULE)
        gameplay = importlib.util.module_from_spec(spec); spec.loader.exec_module(gameplay)
        production, added = gameplay.validate(source, env, GAMEPLAY_V3_SUCCESSOR_SHA256, production)
        return production, sorted(set(paths) | set(added))
    if env.get('ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY') == 'ATD_PURPUR_GAMEPLAY_SOURCE_V2':
        raw_v2 = safe_file(source, GAMEPLAY_V2_MANIFEST).read_bytes()
        require(sha(raw_v2) == GAMEPLAY_V2_SUCCESSOR_SHA256, 'gameplay V2 manifest digest')
        v2 = strict_json(raw_v2)
        require(sha(safe_file(source, GAMEPLAY_V2_MODULE).read_bytes()) == v2['codeBindings'][GAMEPLAY_V2_MODULE], 'gameplay V2 validator digest before execution')
        spec = importlib.util.spec_from_file_location('atd_gameplay_successor_v2', Path(source)/GAMEPLAY_V2_MODULE)
        gameplay = importlib.util.module_from_spec(spec); spec.loader.exec_module(gameplay)
        production, added = gameplay.validate(source, env, GAMEPLAY_V2_SUCCESSOR_SHA256, production)
        return production, sorted(set(paths) | set(added))
    successor_raw = safe_file(source, GAMEPLAY_MANIFEST).read_bytes()
    require(sha(successor_raw) == GAMEPLAY_SUCCESSOR_SHA256, 'gameplay manifest digest')
    successor = strict_json(successor_raw)
    require(sha(safe_file(source, GAMEPLAY_MODULE).read_bytes()) == successor['codeBindings'][GAMEPLAY_MODULE], 'gameplay validator digest before execution')
    spec = importlib.util.spec_from_file_location('atd_gameplay_successor', Path(source)/GAMEPLAY_MODULE)
    gameplay = importlib.util.module_from_spec(spec);spec.loader.exec_module(gameplay)
    production, added = gameplay.validate(source, env, GAMEPLAY_SUCCESSOR_SHA256, production)
    return production, sorted(set(paths) | set(added))



JUNIT_REPAIR_SELECTOR = "ACHIEVETODO_GAMEPLAY_JUNIT_REPAIR"
JUNIT_REPAIR_FILE = "ACHIEVETODO_GAMEPLAY_JUNIT_REPAIR_FILE"
JUNIT_REPAIR_PATH = "tools/certification/authority/gameplay-junit-overlay-v1.json"
JUNIT_REPAIR_MODULE = "tools/build/gameplay_junit_overlay_v1.py"
JUNIT_REPAIR_SHA256 = "5bec165c457465e7c9fd61c21689f8cffa7238289826c6f658a12310ef284a5f"
JUNIT_REPAIR_MODULE_SHA256 = "3bb547ce24c9b0bbf05e889d3c1eb6865477b93ce5601bc8334b474d68519b85"

def junit_repair(source, env):
    source = Path(source).absolute()
    selector, path = env.get(JUNIT_REPAIR_SELECTOR), env.get(JUNIT_REPAIR_FILE)
    require(selector == "ATD_GAMEPLAY_JUNIT_REPAIR_V1" and path == str(source/JUNIT_REPAIR_PATH), "JUnit repair unknown/orphan/path selector")
    raw = safe_file(source,JUNIT_REPAIR_PATH).read_bytes()
    require(sha(raw) == JUNIT_REPAIR_SHA256, "JUnit repair manifest digest")
    module_path = safe_file(source,JUNIT_REPAIR_MODULE)
    module_raw = module_path.read_bytes()
    require(sha(module_raw) == JUNIT_REPAIR_MODULE_SHA256, "JUnit repair helper digest before execution")
    import types
    module = types.ModuleType("atd_exact_junit_repair")
    module.__file__ = str(module_path); module.__package__ = "tools.build"
    exec(compile(module_raw,str(module_path),"exec",dont_inherit=True),module.__dict__)
    return module.validate(source,env,strict_json(raw))

def validate_selection(source, env=None):
    env = os.environ if env is None else env
    selector, path = env.get(SELECTOR), env.get(APPROVAL_FILE)
    if selector is None and path is None:
        require(not any(k in env for k in (JUNIT_REPAIR_SELECTOR,JUNIT_REPAIR_FILE)), "explicit V2 approval required; JUnit repair orphan")
        return None
    require(selector == AUTHORITY and path is not None, 'unknown/orphan selector')
    require(not any(k in env for k in ('ACHIEVETODO_INPUT_AUTHORITY', 'ACHIEVETODO_ALTERNATIVE_MANIFEST',
            'ACHIEVETODO_SOURCE_ORACLE', 'ACHIEVETODO_SOURCE_ORACLE_FILE')), 'mixed historical/alternative selectors')
    source = Path(source).absolute()
    require(env.get('ACHIEVETODO_UI_UX_CERTIFICATION_AUTHORITY') == 'ATD_UI_UX_MAINTENANCE_V1'
            and env.get('ACHIEVETODO_UI_UX_CERTIFICATION_FILE') == str(source/GATE_A_MANIFEST), 'explicit Gate A composition authority required')
    require(Path(path).is_absolute() and Path(path) == source/CAPSULE_PATH, 'wrong/noncanonical approval path')
    require(len(OWNER_B_CAPSULE_SHA256) == 64 and all(c in '0123456789abcdef' for c in OWNER_B_CAPSULE_SHA256), 'OWNER_B_REQUIRED')
    raw = safe_file(source, CAPSULE_PATH).read_bytes()
    require(sha(raw) == OWNER_B_CAPSULE_SHA256, 'Owner B capsule digest')
    capsule = strict_json(raw)
    require(capsule['schema'] == 1 and capsule['authority'] == AUTHORITY and capsule['state'] == 'APPROVED_OWNER_B'
            and capsule['activationApproved'] is True, 'draft/incomplete capsule')
    require(capsule['policySHA256'] == POLICY_SHA256, 'capsule policy digest')
    for key in ('ownerB', 'independentReview'):
        record = capsule.get(key)
        require(isinstance(record, dict) and record.get('approved') is True
                and isinstance(record.get('identity'), str) and record['identity'].strip()
                and isinstance(record.get('decisionSHA256'), str) and len(record['decisionSHA256']) == 64, 'missing approval: '+key)
    require(capsule['ownerB']['identity'] != capsule['independentReview']['identity'], 'independent approval identities')
    authors = capsule.get('implementationAuthors')
    require(isinstance(authors, list) and authors and all(isinstance(x, str) and x.strip() for x in authors)
            and len(authors) == len(set(x.casefold() for x in authors)), 'implementation author identities')
    require(capsule['independentReview']['identity'].casefold() not in {x.casefold() for x in authors},
            'reviewer must differ from implementation authors')
    p = policy(source)
    require(sha(safe_file(source, GATE_A_MANIFEST).read_bytes()) == p['gateAManifestSHA256'], 'Gate A authority changed')
    gate_a = strict_json(safe_file(source, GATE_A_MANIFEST).read_bytes())
    overlay = capsule['sourceAuxiliaryBindings']
    require(set(overlay) == OVERLAY_PATHS, 'auxiliary overlay scope')
    require(capsule.get('ownerABaselines', {}).get(FIXTURE_PATH) == OWNER_A_FIXTURE_BASELINE_SHA256,
            'Owner A fixture baseline identity')
    for file, row in overlay.items():
        require(row['historicalSHA256'] == gate_a['auxiliaryFiles'][file], 'Gate A historical auxiliary substitution')
        require(sha(safe_file(source, file).read_bytes()) == row['currentSHA256'], 'current auxiliary bytes')
    code = capsule['codeBindings']
    require(isinstance(code, dict) and set(code) == REVIEW_CODE_PATHS, 'exact reviewed code closure')
    require(set(capsule['fixturePaths']) == set(code) | RUNTIME_TRUST_ANCHOR_PATHS | {POLICY_PATH, CAPSULE_PATH}, 'fixture closure substitution')
    repair_selected = any(k in env for k in (JUNIT_REPAIR_SELECTOR,JUNIT_REPAIR_FILE))
    repair = junit_repair(source,env) if repair_selected else None
    for file, expected in code.items():
        require(not file.startswith(('src/main/', 'port/')) and file not in (CAPSULE_PATH, HISTORICAL_MANIFEST, HISTORICAL_VERIFIER, GATE_A_MANIFEST), 'illegal code overlay')
        if repair is not None and file == "src/test/java/com/diskree/achievetodo/certification/GateBInputAuthorityContractTest.java":
            require(expected == repair["historicalOriginalG"] and sha(safe_file(source,file).read_bytes()) == repair["currentSuccessorG"], "JUnit separate historic/current G identity")
        else:
            require(sha(safe_file(source, file).read_bytes()) == expected, 'code binding: '+file)
    expected_production, unused = ui07_composition(source, env, gate_a)
    observed = {}
    for relative in ('src/main', 'port/AchieveToDo-Purpur-26.2/src/main'):
        folder = source/relative
        require(folder.is_dir(), 'missing production root')
        for file in folder.rglob('*'):
            _no_links(file)
            if file.is_file():
                name = file.relative_to(source).as_posix()
                observed[name] = sha(safe_file(source, name).read_bytes())
    require(observed == expected_production, 'Gate A production closure/additions')
    for file, expected in gate_a['auxiliaryFiles'].items():
        current = overlay[file]['currentSHA256'] if file in overlay else expected
        require(sha(safe_file(source, file).read_bytes()) == current, 'Gate A auxiliary bytes: '+file)
    for file, pin in gate_a['protectedPins'].items():
        require(sha(safe_file(source, file).read_bytes()) == pin['fabricSHA256'], 'protected gameplay pin: '+file)
    return capsule


def verify_activation(source, external_root, env=None):
    capsule = validate_selection(source, env)
    require(capsule is not None, 'explicit V2 authority required')
    checked = _inspect_candidate_inputs(source, external_root)
    raw = [x for x in checked['payloadIdentities'] if x['kind'] == 'UNTRUSTED_RAW_CAPTURE']
    require(capsule['rawInputs'] == raw, 'approved raw cohort bytes')
    review = capsule.get('licenseReview')
    require(isinstance(review, dict) and review.get('approved') is True, 'license review required')
    require(review.get('licenses') == {prefix: row['id'] for prefix, row in checked['licenses'].items()}, 'reviewed license identities')
    require(review.get('scope') == 'LOCAL_CERTIFICATION_NO_DISTRIBUTION_RIGHTS_GRANTED'
            and review.get('upstreamNoticeLimitation') == 'NOTICE_SOURCE_INCOMPLETE_UPSTREAM', 'reviewed license scope/limitation')
    permission = next(x for x in policy(source)['sources'] if x['projectId'] == 'YX5bAAJN')['licensePermissionEvidence']
    require(review.get('permissionEvidenceSHA256') == permission['SHA256'], 'reviewed ARR permission evidence')
    require(set(capsule['compatibilityDispositions']) == set(checked['compatibility'])
            and all(x.get('approved') is True for x in capsule['compatibilityDispositions'].values()), 'compatibility review required')
    for prefix, actual in checked['compatibility'].items():
        disposition = capsule['compatibilityDispositions'][prefix]
        require(disposition.get('gameVersions') == actual['gameVersions']
                and disposition.get('loaders') == actual['loaders'], 'reviewed compatibility identities')
        expected = 'DECLARED_UPSTREAM_26_2' if '26.2' in actual['gameVersions'] else 'HISTORICAL_TRANSLATION_ONLY_REQUIRES_CONSUMER_RUNTIME_PROOF'
        require(disposition.get('disposition') == expected, 'reviewed compatibility scope')
    receipts = capsule['transportReceipts']
    require(len(receipts) == 10 and len({x['name'] for x in receipts}) == 10, 'ten transport receipts required')
    for receipt in receipts:
        path = Path(receipt['path'])
        require(path.is_absolute(), 'absolute transport receipt')
        data = safe_file(path.parent, path.name).read_bytes()
        require(sha(data) == receipt['SHA256'], 'transport receipt digest')
        value = strict_json(data)
        item = next((x for x in raw if x['name'] == receipt['name']), None)
        require(item is not None and value['bodySHA256'] == item['SHA256'] and value['bodyBytes'] == item['size'], 'transport body binding')
        row = next(x for x in policy(source)['sources'] if receipt['name'] in (x['projectRawName'], x['versionRawName']))
        validate_transport_receipt(row, receipt['name'], value, safe_file(external_root, receipt['name']).read_bytes())
    checked['scope'] = 'AUTHENTICATED_V2_INPUTS'
    checked['activated'] = True
    return checked


def validate_transport_receipt(row, name, receipt, body):
    """Pure receipt/binding validation; does not approve a captured body."""
    require(isinstance(receipt, dict) and receipt.get('schema') == 1, 'transport receipt schema')
    kind = 'project' if name == row['projectRawName'] else 'version' if name == row['versionRawName'] else None
    require(kind is not None, 'transport response identity')
    endpoint = row[kind+'Endpoint']
    require(receipt.get('name') == name and receipt.get('requestURL') == receipt.get('finalURL') == endpoint,
            'transport exact endpoint')
    require(receipt.get('method') == 'GET' and receipt.get('tlsValidation') is True, 'transport method/TLS')
    require(type(receipt.get('status')) is int and receipt['status'] == 200
            and receipt.get('result') == 'RAW_CAPTURE_ONLY_NOT_TRUSTED' and receipt.get('error') is None, 'transport result')
    request = receipt.get('requestHeaders')
    require(isinstance(request, dict) and request.get('Accept') == 'application/json'
            and request.get('Accept-Encoding') == 'identity', 'transport request headers')
    headers = receipt.get('responseHeaders')
    require(isinstance(headers, dict) and isinstance(headers.get('Content-Type'), str)
            and headers['Content-Type'].split(';', 1)[0].strip().lower() == 'application/json', 'transport JSON media type')
    require(headers.get('Content-Encoding') in (None, 'identity') and headers.get('Location') is None, 'transport encoding/redirect')
    require(type(receipt.get('attempt')) is int and 1 <= receipt['attempt'] <= 2, 'transport attempt bounds')
    try:
        start = datetime.fromisoformat(receipt['startedUTC'])
        end = datetime.fromisoformat(receipt['finishedUTC'])
        require(start.tzinfo is not None and end.tzinfo is not None
                and start.utcoffset() == end.utcoffset() == timezone.utc.utcoffset(start), 'transport UTC timestamps')
        require(0 <= (end-start).total_seconds() <= 30, 'transport time bound/order')
    except (TypeError, KeyError, ValueError) as error:
        raise ValueError('GATE_B_V2_REJECTED: transport timestamps: '+str(error)) from error
    require(type(receipt.get('bodyBytes')) is int and receipt['bodyBytes'] == len(body)
            and receipt.get('bodySHA256') == sha(body) and len(body) <= 10485760, 'transport actual body binding')
    length = headers.get('Content-Length')
    require(length is None or (isinstance(length, str) and length.isdecimal() and int(length) == len(body)), 'transport content length')
    return {'scope': 'TRANSPORT_STRUCTURE_ONLY_NOT_APPROVAL', 'name': name, 'bodySHA256': sha(body)}


def dependencies(source, home, env=None):
    require(validate_selection(source, env) is not None, 'explicit V2 dependency authority')
    original = inherited_authority(source)
    home = safe_root(home)
    for row in original['dependencyInputs']:
        check_identity(home, row, 'gradleUserHomeRelativePath')
    return {'authority': AUTHORITY, 'selectedDependencyInputs': 372}
