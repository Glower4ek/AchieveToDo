"""V2 hostile archive controls; source composition controls added after review binding.

Synthetic archive data here tests a parser, never gameplay or authority acceptance.
"""
import io
import copy
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

from tools.build import gameplay_successor_v2 as g


class PredecessorArchiveTests(unittest.TestCase):
    def rows(self):
        return {f'original/{i}.txt': {'sha256': g.c.sha(b'original'), 'bytes': 8}
                for i in range(32)}

    def archive(self, names=None, replacement=None, mode=None):
        out = io.BytesIO()
        with warnings.catch_warnings():
            warnings.simplefilter('ignore', UserWarning)
            with zipfile.ZipFile(out, 'w', compression=zipfile.ZIP_STORED) as z:
                for name in self.rows() if names is None else names:
                    info = zipfile.ZipInfo(name)
                    info.external_attr = (mode or (stat.S_IFREG | 0o644)) << 16
                    z.writestr(info, b'original' if replacement is None else replacement)
        return out.getvalue()

    def test_exact_original_bytes(self):
        actual = g.archive_originals(self.archive(), self.rows())
        self.assertEqual(set(self.rows()), set(actual))
        self.assertTrue(all(x == b'original' for x in actual.values()))

    def test_missing_extra_duplicate_and_unsafe_members(self):
        names = list(self.rows())
        for changed in [names[:-1], names + ['extra'], names[:-1] + [names[0]],
                        names[:-1] + ['../escape'], names[:-1] + ['/absolute'],
                        names[:-1] + ['Original/31.txt']]:
            with self.subTest(changed=changed[-1]), self.assertRaises(ValueError):
                g.archive_originals(self.archive(changed), self.rows())
        # Windows ZipInfo normalizes backslashes while writing; corrupt both raw name records.
        raw = self.archive().replace(b'original/31.txt', b'original\\31.txt')
        with self.assertRaises(ValueError):
            g.archive_originals(raw, self.rows())

    def test_tampered_bytes_symlink_and_oversized(self):
        for raw in [self.archive(replacement=b'tampered'),
                    self.archive(mode=stat.S_IFLNK | 0o777), b'x' * (g.MAX_ARCHIVE + 1), b'badzip']:
            with self.subTest(size=len(raw)), self.assertRaises(ValueError):
                g.archive_originals(raw, self.rows())

    def test_invalid_expected_inventory(self):
        for bad in [{}, {**self.rows(), 'extra': {'sha256': '0' * 64, 'bytes': 0}},
                    {**self.rows(), 'original/0.txt': {'sha256': '0' * 64, 'bytes': 8}},
                    {**self.rows(), 'original/0.txt': {'sha256': '0' * 64, 'bytes': 350001}}]:
            with self.subTest(count=len(bad)), self.assertRaises(ValueError):
                g.archive_originals(self.archive(), bad)


class AuthoritySelectionTests(unittest.TestCase):
    def test_public_mixed_authority_rejected(self):
        root = Path(__file__).resolve().parents[3]
        base = {g.c.SELECTOR: g.c.AUTHORITY, g.c.APPROVAL_FILE: str(root / g.c.CAPSULE_PATH),
                g.SELECTOR: g.AUTHORITY, g.FILE: str(root / g.PATH)}
        for key in ['ACHIEVETODO_INPUT_AUTHORITY', 'ACHIEVETODO_ALTERNATIVE_MANIFEST',
                    'ACHIEVETODO_SOURCE_ORACLE', 'ACHIEVETODO_SOURCE_ORACLE_FILE']:
            with self.subTest(key=key), self.assertRaisesRegex(ValueError, 'mixed'):
                g.c.validate_selection(root, {**base, key: 'UNAPPROVED'})

    def test_unknown_orphan_wrong_path_and_missing_parents(self):
        with tempfile.TemporaryDirectory(prefix='atd-v2-selector-') as tmp:
            root = Path(tmp)
            good = {g.SELECTOR: g.AUTHORITY, g.FILE: str(root / g.PATH),
                    g.c.SELECTOR: g.c.AUTHORITY,
                    'ACHIEVETODO_UI07_AUTHORITY': 'ATD_UI07_SCOPED_SUCCESSOR_V1'}
            for update in [{g.SELECTOR: None}, {g.SELECTOR: 'ATD_PURPUR_GAMEPLAY_SOURCE_V1'},
                           {g.SELECTOR: 'UNKNOWN'}, {g.FILE: None}, {g.FILE: str(root / 'other.json')},
                           {g.c.SELECTOR: None}, {'ACHIEVETODO_UI07_AUTHORITY': None}]:
                with self.subTest(update=update), self.assertRaisesRegex(ValueError, 'selector'):
                    g.validate(root, {**good, **update}, '0' * 64, {})

    def test_mutated_ancestor_closure(self):
        root = Path(__file__).resolve().parents[3]
        gate = g.c.strict_json((root / g.c.GATE_A_MANIFEST).read_bytes())['productionClosure']
        ui = g.c.strict_json((root / g.c.UI07_MANIFEST).read_bytes())['productionDelta']
        base = dict(gate)
        base[ui['path']] = ui['afterSHA256']
        old, actual = g.predecessor_closure(root, base)
        self.assertEqual(2500, len(actual))
        target = next(p for p, row in old['changes'].items() if row['beforeSHA256'] is not None
                      and p.startswith('port/AchieveToDo-Purpur-26.2/src/main/'))
        base[target] = '0' * 64
        with self.assertRaisesRegex(ValueError, 'predecessor delta'):
            g.predecessor_closure(root, base)


class CurrentSourceCompositionTests(unittest.TestCase):
    ROOT = Path(__file__).resolve().parents[3]

    def check(self, root=None, digest=None):
        root = self.ROOT if root is None else root
        gate = g.c.strict_json((root / g.c.GATE_A_MANIFEST).read_bytes())['productionClosure']
        delta = g.c.strict_json((root / g.c.UI07_MANIFEST).read_bytes())['productionDelta']
        base = dict(gate)
        base[delta['path']] = delta['afterSHA256']
        env = dict(os.environ)
        env.update({g.SELECTOR: g.AUTHORITY, g.FILE: str(root / g.PATH),
                    g.c.SELECTOR: g.c.AUTHORITY,
                    'ACHIEVETODO_UI07_AUTHORITY': 'ATD_UI07_SCOPED_SUCCESSOR_V1'})
        return g.validate(root, env, g.c.GAMEPLAY_V2_SUCCESSOR_SHA256 if digest is None else digest, base)

    def test_exact_current_source_and_unchanged_four_legacy_controls(self):
        result, paths = self.check()
        self.assertEqual(2504, len(result))
        self.assertTrue(g.ALLOWED <= set(paths))

    def test_manifest_code_review_fixture_ancestor_and_production_tampering(self):
        m = g.c.strict_json((self.ROOT / g.PATH).read_bytes())
        required = set(m['predecessorContext']) | set(m['codeBindings']) | g.ALLOWED | {g.PATH}
        targets = [g.PATH, 'tools/build/gameplay_successor_v2.py',
                   'tools/build/tests/test_gameplay_successor_v2.py',
                   m['independentReview']['path'], g.FIXTURE, g.V1_PATH,
                   'tools/build/gameplay_successor.py',
                   next(p for p in g.ALLOWED if '/src/main/' in p),
                   'src/main/unapproved-v2-negative.txt']
        with tempfile.TemporaryDirectory(prefix='atd-v2-current-negative-') as tmp:
            root = Path(tmp)
            for name in required:
                src = self.ROOT / name
                if src.is_file():
                    dst = root / name
                    dst.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(src, dst)
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
            target = root / next(iter(m['incrementalProductionChanges']))
            data = target.read_bytes()
            target.unlink()
            with self.assertRaises(ValueError):
                self.check(root)
            target.write_bytes(data)

    def test_repinned_semantically_invalid_manifests(self):
        m = g.c.strict_json((self.ROOT / g.PATH).read_bytes())
        required = set(m['predecessorContext']) | set(m['codeBindings']) | g.ALLOWED | {g.PATH}
        with tempfile.TemporaryDirectory(prefix='atd-v2-semantic-negative-') as tmp:
            root = Path(tmp)
            for name in required:
                src = self.ROOT / name
                if src.is_file():
                    dst = root / name
                    dst.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(src, dst)
            variants = []
            for key in ['oldGateA', 'oldOwnerB', 'oldUI07', 'oldV1']:
                bad = copy.deepcopy(m)
                bad[key] = '0' * 64
                variants.append((key, bad))
            bad = copy.deepcopy(m)
            bad['predecessorContext'][next(iter(m['predecessorContext']))] = '0' * 64
            variants.append(('wrong predecessor context', bad))
            bad = copy.deepcopy(m)
            bad['incrementalProductionChanges']['src/main/orphan.txt'] = {'beforeSHA256': None, 'afterSHA256': '0' * 64}
            variants.append(('orphan incremental', bad))
            bad = copy.deepcopy(m)
            bad['allGameplayChanges'][next(iter(g.ALLOWED))]['afterSHA256'] = '0' * 64
            variants.append(('wrong after', bad))
            bad = copy.deepcopy(m)
            bad['codeBindings']['unapproved-helper.py'] = '0' * 64
            variants.append(('unknown helper', bad))
            bad = copy.deepcopy(m)
            bad['predecessorContext'].pop(next(iter(m['predecessorContext'])))
            variants.append(('missing context', bad))
            for label, bad in variants:
                raw = json.dumps(bad).encode()
                (root / g.PATH).write_bytes(raw)
                # The digest is recomputed ONLY in this negative test to reach internal semantics.
                # None of these digests is an accepted literal build/Java/Python anchor.
                with self.subTest(label=label), self.assertRaises(ValueError):
                    self.check(root, g.c.sha(raw))

    def test_four_legacy_controls_are_mandatory(self):
        for code, summary in [(1, 'Ran 4 tests\n\nOK\n'), (0, 'Ran 3 tests\n\nOK\n'),
                              (0, 'Ran 4 tests\n\nOK (skipped=1)\n'), (0, ''),
                              (0, 'Ran 4 tests\n\nFAILED (failures=1)\n')]:
            with self.subTest(code=code, summary=summary), self.assertRaises(ValueError):
                g.require_legacy_result(SimpleNamespace(returncode=code, stdout='', stderr=summary))
        g.require_legacy_result(SimpleNamespace(returncode=0, stdout='', stderr='Ran 4 tests\n\nOK\n'))


if __name__ == '__main__':
    unittest.main()
