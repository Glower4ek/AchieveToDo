"""Exact-current controls plus hostile inputs; no gameplay or runtime acceptance."""
import copy
import io
import json
import os
import shutil
import stat
import tempfile
import unittest
import warnings
import zipfile
from pathlib import Path
from types import SimpleNamespace

from tools.build import gameplay_successor_v3 as g


ROOT = Path(__file__).resolve().parents[3]


class FiveOriginalArchiveTests(unittest.TestCase):
    def originals(self):
        with zipfile.ZipFile(ROOT / g.FIXTURE) as z:
            return {name: z.read(name) for name in g.ARCHIVE_FILES}

    def archive(self, names=None, mode=None, compression=zipfile.ZIP_STORED,
                replacement=None):
        originals = self.originals()
        out = io.BytesIO()
        with warnings.catch_warnings():
            warnings.simplefilter('ignore', UserWarning)
            with zipfile.ZipFile(out, 'w') as z:
                for name in originals if names is None else names:
                    info = zipfile.ZipInfo(name)
                    info.create_system = 3
                    info.external_attr = (mode or (stat.S_IFREG | 0o644)) << 16
                    info.compress_type = compression
                    z.writestr(info, originals.get(name, b'not an original')
                               if replacement is None else replacement)
        return out.getvalue()

    def test_exact_five_original_bytes(self):
        self.assertEqual(self.originals(), g.archive_originals(
            (ROOT / g.FIXTURE).read_bytes(), g.ARCHIVE_FILES))

    def test_missing_extra_duplicate_case_alias_and_unsafe_paths(self):
        names = list(g.ARCHIVE_FILES)
        variants = [names[:-1], names + ['extra'], names[:-1] + [names[0]],
                    names[:-1] + [names[-1].upper()], names[:-1] + ['../escape'],
                    names[:-1] + ['/absolute'], names[:-1] + ['C:/escape'],
                    names[:-1] + ['unsafe\\member']]
        for changed in variants:
            with self.subTest(last=changed[-1]), self.assertRaises(ValueError):
                g.archive_originals(self.archive(changed), g.ARCHIVE_FILES)
        for name in [None, 1, '', './x', 'x/../y', 'x//y', 'x\x00y', 'C:/x', 'x\\y']:
            with self.subTest(name=name), self.assertRaises(ValueError):
                g.relative(name)

    def test_tampered_nonregular_compressed_encrypted_and_oversized(self):
        raw = bytearray(self.archive())
        for signature, offset in [(b'PK\x03\x04', 6), (b'PK\x01\x02', 8)]:
            start = 0
            while True:
                index = raw.find(signature, start)
                if index < 0:
                    break
                raw[index + offset] |= 1
                start = index + 4
        variants = [self.archive(replacement=b'tampered'),
                    self.archive(mode=stat.S_IFLNK | 0o777),
                    self.archive(compression=zipfile.ZIP_DEFLATED), bytes(raw),
                    b'x' * (g.MAX_ARCHIVE + 1), b'not a ZIP']
        for raw in variants:
            with self.subTest(size=len(raw)), self.assertRaises(ValueError):
                g.archive_originals(raw, g.ARCHIVE_FILES)

    def test_redeclared_original_inventory_rejected(self):
        for entries in [{}, {**g.ARCHIVE_FILES, 'extra': {'bytes': 0, 'sha256': '0' * 64}}]:
            with self.assertRaises(ValueError):
                g.archive_originals(self.archive(), entries)
        altered = copy.deepcopy(g.ARCHIVE_FILES)
        altered[g.TARGET]['sha256'] = '0' * 64
        with self.assertRaises(ValueError):
            g.archive_originals(self.archive(), altered)


class SelectionAndLegacyTests(unittest.TestCase):
    def test_unknown_orphan_wrong_path_and_missing_parent_selectors(self):
        with tempfile.TemporaryDirectory(prefix='atd-v3-selector-') as tmp:
            root = Path(tmp)
            good = {g.SELECTOR: g.AUTHORITY, g.FILE: str(root / g.PATH),
                    g.c.SELECTOR: g.c.AUTHORITY,
                    'ACHIEVETODO_UI07_AUTHORITY': 'ATD_UI07_SCOPED_SUCCESSOR_V1'}
            for update in [{g.SELECTOR: None}, {g.SELECTOR: 'UNKNOWN'},
                           {g.SELECTOR: 'ATD_PURPUR_GAMEPLAY_SOURCE_V2'},
                           {g.FILE: None}, {g.FILE: str(root / 'other.json')},
                           {g.c.SELECTOR: None}, {'ACHIEVETODO_UI07_AUTHORITY': None}]:
                with self.subTest(update=update), self.assertRaisesRegex(ValueError, 'selector'):
                    g.validate(root, {**good, **update}, '0' * 64, {})

    def test_public_mixed_authority_rejected(self):
        base = {g.c.SELECTOR: g.c.AUTHORITY,
                g.c.APPROVAL_FILE: str(ROOT / g.c.CAPSULE_PATH),
                g.SELECTOR: g.AUTHORITY, g.FILE: str(ROOT / g.PATH)}
        for key in ['ACHIEVETODO_INPUT_AUTHORITY', 'ACHIEVETODO_ALTERNATIVE_MANIFEST',
                    'ACHIEVETODO_SOURCE_ORACLE', 'ACHIEVETODO_SOURCE_ORACLE_FILE']:
            with self.subTest(key=key), self.assertRaisesRegex(ValueError, 'mixed'):
                g.c.validate_selection(ROOT, {**base, key: 'UNAPPROVED'})

    def test_eleven_unchanged_v2_controls_are_mandatory(self):
        marker = 'test_exact_current_source_and_unchanged_four_legacy_controls ... ok\n'
        good = marker + '\nRan 11 tests\n\nOK\n'
        g.require_legacy_result(SimpleNamespace(returncode=0, stdout='', stderr=good))
        for code, summary in [(1, good), (0, good.replace('11', '10')),
                              (0, good.replace('OK', 'OK (skipped=1)')),
                              (0, good.replace('OK', 'OK (expected failures=1)')),
                              (0, '\nRan 11 tests\n\nOK\n'),
                              (0, good.replace('OK', 'FAILED'))]:
            with self.subTest(code=code, summary=summary), self.assertRaises(ValueError):
                g.require_legacy_result(SimpleNamespace(returncode=code, stdout='', stderr=summary))


class CurrentCompositionTests(unittest.TestCase):
    def check(self, root=ROOT, digest=None):
        gate = g.c.strict_json((root / g.c.GATE_A_MANIFEST).read_bytes())['productionClosure']
        delta = g.c.strict_json((root / g.c.UI07_MANIFEST).read_bytes())['productionDelta']
        base = dict(gate)
        base[delta['path']] = delta['afterSHA256']
        env = dict(os.environ)
        env.update({g.SELECTOR: g.AUTHORITY, g.FILE: str(root / g.PATH),
                    g.c.SELECTOR: g.c.AUTHORITY,
                    'ACHIEVETODO_UI07_AUTHORITY': 'ATD_UI07_SCOPED_SUCCESSOR_V1'})
        return g.validate(root, env, g.c.GAMEPLAY_V3_SUCCESSOR_SHA256
                          if digest is None else digest, base)

    def copy_context(self, root):
        m = g.c.strict_json((ROOT / g.PATH).read_bytes())
        required = set(m['predecessorContext']) | set(m['codeBindings']) | set(m['allGameplayChanges']) | {g.PATH}
        for name in required:
            source = ROOT / name
            self.assertTrue(source.is_file(), name)
            dest = root / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, dest)

    def test_exact_current_source_and_all_unchanged_parent_controls(self):
        result, paths = self.check()
        self.assertEqual(2504, len(result))
        self.assertIn(g.TARGET, paths)
        self.assertTrue(g.CODE_PATHS <= set(paths))

    def test_wrong_manifest_digest_rejected(self):
        with self.assertRaisesRegex(ValueError, 'manifest digest'):
            self.check(digest='0' * 64)

    def test_immutable_v2_still_rejects_current_changed_source(self):
        from tools.build import gameplay_successor_v2 as parent
        gate = g.c.strict_json((ROOT / g.c.GATE_A_MANIFEST).read_bytes())['productionClosure']
        delta = g.c.strict_json((ROOT / g.c.UI07_MANIFEST).read_bytes())['productionDelta']
        base = dict(gate)
        base[delta['path']] = delta['afterSHA256']
        env = {g.SELECTOR: parent.AUTHORITY, g.FILE: str(ROOT / parent.PATH),
               g.c.SELECTOR: g.c.AUTHORITY,
               'ACHIEVETODO_UI07_AUTHORITY': 'ATD_UI07_SCOPED_SUCCESSOR_V1'}
        with self.assertRaises(ValueError):
            parent.validate(ROOT, env, parent.c.GAMEPLAY_V2_SUCCESSOR_SHA256, base)

    def test_manifest_helper_review_original_extra_missing_and_source_tampering(self):
        parent, old = g.parent_data(ROOT)
        peer = next(p for p in old if p != g.TARGET)
        targets = [g.PATH, g.MODULE, g.TESTS, g.REVIEW, g.FIXTURE, g.PARENT,
                   'tools/build/gameplay_successor_v2.py',
                   'tools/build/tests/test_gameplay_successor_v2.py',
                   parent['independentReview']['path'], g.JUNIT, g.TARGET, peer,
                   'src/main/unapproved-v3-negative.txt']
        with tempfile.TemporaryDirectory(prefix='atd-v3-tamper-') as tmp:
            root = Path(tmp)
            self.copy_context(root)
            for name in targets:
                target = root / name
                original = target.read_bytes() if target.exists() else None
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes((original or b'') + b'TAMPER')
                with self.subTest(path=name), self.assertRaises(ValueError):
                    self.check(root)
                if original is None:
                    target.unlink()
                else:
                    target.write_bytes(original)
            target = root / peer
            original = target.read_bytes()
            target.unlink()
            with self.assertRaises(ValueError):
                self.check(root)
            target.write_bytes(original)

    def test_repinned_semantically_invalid_manifests(self):
        m = g.c.strict_json((ROOT / g.PATH).read_bytes())
        variants = []
        for key in ['oldGateA', 'oldOwnerB', 'oldUI07', 'oldV1']:
            bad = copy.deepcopy(m)
            bad['ancestorPins'][key] = '0' * 64
            variants.append(bad)
        for section, path, field in [('productionDelta', g.TARGET, 'beforeSHA256'),
                                     ('productionDelta', g.TARGET, 'afterSHA256'),
                                     ('auxiliaryDelta', g.JUNIT, 'beforeSHA256'),
                                     ('auxiliaryDelta', g.JUNIT, 'afterSHA256')]:
            bad = copy.deepcopy(m)
            bad[section][path][field] = '0' * 64
            variants.append(bad)
        for section in ['productionDelta', 'auxiliaryDelta', 'codeBindings', 'predecessorContext']:
            bad = copy.deepcopy(m)
            bad[section]['unapproved'] = '0' * 64
            variants.append(bad)
        bad = copy.deepcopy(m)
        bad['oldV2']['sha256'] = '0' * 64
        variants.append(bad)
        bad = copy.deepcopy(m)
        bad['currentProductionClosure'].pop(next(iter(bad['currentProductionClosure'])))
        variants.append(bad)
        bad = copy.deepcopy(m)
        bad['allGameplayChanges'][g.TARGET]['beforeSHA256'] = bad['productionDelta'][g.TARGET]['beforeSHA256']
        variants.append(bad)
        bad = copy.deepcopy(m)
        bad['independentReview']['sha256'] = '0' * 64
        variants.append(bad)
        bad = copy.deepcopy(m)
        bad['archiveEntries'][g.TARGET]['sha256'] = '0' * 64
        variants.append(bad)
        bad = copy.deepcopy(m)
        bad['predecessorContext'].pop(next(iter(bad['predecessorContext'])))
        variants.append(bad)
        bad = copy.deepcopy(m)
        bad['productReady'] = True
        variants.append(bad)
        with tempfile.TemporaryDirectory(prefix='atd-v3-semantic-') as tmp:
            root = Path(tmp)
            self.copy_context(root)
            for index, bad in enumerate(variants):
                raw = json.dumps(bad).encode()
                (root / g.PATH).write_bytes(raw)
                # Test-only digest reaches semantics; no literal authority anchor is changed.
                with self.subTest(index=index), self.assertRaises(ValueError):
                    self.check(root, g.c.sha(raw))


if __name__ == '__main__':
    unittest.main()
