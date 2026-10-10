"""Read-only build authority checks. No acquisition, repair or network behavior.

Dependency policy: CACHE-BASELINE-LIMITED is byte identity, not universal origin
authentication. Dependency mode checks listed paths only; Gradle lifecycle
ordering and native enforcement require independent later verification.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import zipfile

MANIFEST_SHA256 = '1dbe9d7cd2fe653454f8692b7cdf5e54787c3df74eb12461c01164b99190ac77'
EXPECTED_EXTERNAL_NAMES = {
    'bacap_hardcore.zip', 'terralith.zip', 'amplified_nether.zip', 'nullscape.zip',
    'bacap-language-pack.zip', 'bacap-rus-translate.zip', 'bacap-better-ru.zip',
    'ru-blaze-and-caves-advancements-pack.zip', 'bacaped-language-pack.zip',
    'BSD-2-Clause.txt',
    *{p + s for p in ('bacap-language-pack', 'bacap-rus-translate',
        'bacap-better-ru', 'ru-blaze-and-caves-advancements-pack',
        'bacaped-language-pack') for s in ('_acquisition.json', '_metadata.json')},
}

def require(condition, message):
    if not condition:
        raise ValueError(message)

def sha(data):
    return hashlib.sha256(data).hexdigest()

def safe_file(root, relative):
    rel = Path(relative)
    require(not rel.is_absolute() and '..' not in rel.parts and rel.parts,
            'invalid relative input path: ' + relative)
    path = root / rel
    for item in [root, *[root.joinpath(*rel.parts[:i]) for i in range(1, len(rel.parts)+1)]]:
        require(not item.is_symlink() and not (hasattr(item, 'is_junction') and item.is_junction()),
                'symlink/junction input rejected: ' + str(item))
    require(path.resolve().is_relative_to(root.resolve()), 'escaping input path')
    require(path.is_file(), 'missing required input: ' + relative)
    return path

def check(root, row, key='path'):
    path = safe_file(root, row[key])
    data = path.read_bytes()
    require(len(data) == row['size'], 'size mismatch: ' + row[key])
    require(sha(data) == row['SHA256'], 'SHA256 mismatch: ' + row[key])
    return path

def unique_object(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, 'duplicate JSON authority key: ' + key)
        result[key] = value
    return result

def authority(source):
    data = safe_file(source, 'src/test/resources/build_inputs/manifest.json').read_bytes()
    require(sha(data) == MANIFEST_SHA256, 'fixture/build authority manifest identity mismatch')
    m = json.loads(data, object_pairs_hook=unique_object)
    require(m['fixturePolicy'] == 'DISTRIBUTION-B' and m['dependencyPolicy'] == 'CACHE-BASELINE-LIMITED', 'policy mismatch')
    require(m['automaticDownload'] is False and m['historicalFallback'] is False, 'unsafe input policy')
    names = [r['logicalName'] for r in m['entries']]
    require(len(names) == len(set(names)) == 20 and set(names) == EXPECTED_EXTERNAL_NAMES, 'required 20-file contract mismatch')
    require(len(m['dependencyInputs']) == 372, 'selected dependency contract cardinality mismatch')
    require(len({r['gradleUserHomeRelativePath'] for r in m['dependencyInputs']}) == 372, 'duplicate selected cache paths')
    require(len(m['sourceFiles']) == len({r['path'] for r in m['sourceFiles']}), 'duplicate source authorities')
    require(not any(r['path'] in ('tools/build/verify_inputs.py', 'src/test/resources/build_inputs/manifest.json', 'build.gradle') for r in m['sourceFiles']), 'cyclic checksum authority')
    for row in m['sourceFiles']:
        check(source, row)
    return m

def dependencies(m, home):
    require(home is not None and home.is_dir(), 'explicit Gradle user home required')
    for row in m['dependencyInputs']:
        check(home, row, 'gradleUserHomeRelativePath')
    print('SELECTED_DEPENDENCY_BYTES_PASS ' + str(len(m['dependencyInputs'])))

def external(m, source, supplied):
    require(bool(supplied), 'ACHIEVETODO_TEST_INPUTS_DIR must be provided explicitly')
    root = Path(supplied)
    require(root.is_absolute() and root.is_dir(), 'external root must be an existing absolute directory')
    for item in [root, *root.parents]:
        require(not item.is_symlink() and not (hasattr(item, 'is_junction') and item.is_junction()), 'external root symlink/junction rejected')
    root = root.resolve()
    require(not root.is_relative_to(source.resolve()), 'external root must be outside source checkout')
    require(not any(p.lower() in ('build', '.gradle', '.gradle-user-home', 'reference') or p.lower().startswith('phase_b_') or p.lower().startswith('phase_c_') for p in root.parts), 'historical/source cache input root rejected')
    require({p.name for p in root.iterdir()} == EXPECTED_EXTERNAL_NAMES, 'unexpected/missing external input names')
    for row in m['entries']:
        require(row['required'] is True and row['externalPathRelativeToRoot'] == row['logicalName'], 'ambiguous logical input path')
        path = check(root, row, 'externalPathRelativeToRoot')
        if row['type'] == 'originalZIP':
            with zipfile.ZipFile(path) as z:
                require(z.testzip() is None, 'ZIP integrity failure: ' + path.name)
        elif row['type'] == 'acquisitionJSON':
            acq = json.loads(path.read_bytes())
            prefix = row['logicalName'].removesuffix('_acquisition.json')
            meta = json.loads(safe_file(root, prefix+'_metadata.json').read_bytes())
            require(acq['projectId'] == meta['project']['id'] == row['sourceProject'], 'acquisition project mismatch')
            require(acq['version'] == row['sourceVersion'] and acq['versionId'] == row['acquisitionIdentity']['versionId'], 'acquisition version mismatch')
            require(meta['project']['license']['id'] == acq['license']['id'] == row['licenseReference']['classification'], 'license/provenance mismatch')
            version = next(v for v in meta['versions'] if v['id'] == acq['versionId'])
            require(version['version_number'] == acq['version'], 'metadata version mismatch')
            record = next(f for f in version['files'] if f['filename'] == acq['filename'])
            archive = safe_file(root, prefix+'.zip').read_bytes()
            require(record['size'] == acq['size'] == len(archive), 'archive/acquisition size mismatch')
            for algorithm, value in acq['hashes'].items():
                require(hashlib.new(algorithm, archive).hexdigest() == value, 'archive/acquisition hash mismatch')
            require(record['hashes']['sha1'] == acq['hashes']['sha1'], 'metadata archive hash mismatch')
    for row in m['packSources']:
        path = check(root if row['authority'] == 'external' else source, row)
        require(hashlib.sha1(path.read_bytes()).hexdigest() == row['SHA1'], 'original pack SHA1 mismatch')
    # Only a lightweight manifest may exist in the repository input directory.
    require({p.name for p in (source/'src/test/resources/build_inputs').iterdir()} == {'manifest.json'}, 'repository payload fixture rejected')
    print('EXTERNAL_INPUTS_PASS 20/20; ZIPs 9/9; provenance 5/5')

def exact_git(source, commit):
    def git(*args):
        return subprocess.check_output(['git', *args], cwd=source, env={**os.environ, 'GIT_OPTIONAL_LOCKS': '0'})
    records = git('ls-tree', '-r', '-z', commit).split(b'\0')
    count = 0
    # Binary plumbing, not shell redirection or text-mode checkout conversion.
    process = subprocess.Popen(['git', 'cat-file', '--batch'], cwd=source, stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    try:
        for record in records:
            if not record: continue
            metadata, name = record.split(b'\t', 1)
            mode, kind, oid = metadata.split()
            require(kind == b'blob' and mode in (b'100644', b'100755'), 'unsupported source materialization entry')
            process.stdin.write(oid + b'\n'); process.stdin.flush()
            header = process.stdout.readline().split()
            require(header[1] == b'blob', 'missing Git blob')
            blob = process.stdout.read(int(header[2])); require(process.stdout.read(1) == b'\n', 'Git stream framing')
            path = safe_file(source, name.decode('utf-8'))
            require(path.read_bytes() == blob, 'raw Git blob mismatch: ' + name.decode('utf-8'))
            count += 1
    finally:
        process.stdin.close(); process.wait()
    require(process.returncode == 0, 'Git blob reader failed')
    print('EXACT_GIT_BLOBS_PASS ' + str(count))

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--source-root', type=Path, default=Path(__file__).resolve().parents[2])
    p.add_argument('--dependencies', action='store_true')
    p.add_argument('--gradle-user-home', type=Path)
    p.add_argument('--external-root', default=os.environ.get('ACHIEVETODO_TEST_INPUTS_DIR'))
    p.add_argument('--git-commit', help='Verify every materialized raw tracked blob against this exact accepted commit; no writes')
    a = p.parse_args()
    source = a.source_root.resolve()
    if a.git_commit:
        exact_git(source, a.git_commit)
        return
    m = authority(source)
    if a.dependencies: dependencies(m, a.gradle_user_home)
    else: external(m, source, a.external_root)

if __name__ == '__main__':
    try: main()
    except (ValueError, KeyError, StopIteration, OSError, zipfile.BadZipFile) as exc:
        print('INPUT_VERIFICATION_FAILED: ' + str(exc), file=sys.stderr)
        sys.exit(1)
