"""Owner-A-only contract controls, never V2 activation/certification evidence.

Pure synthetic metadata vectors are explicitly marked as test data, remain in
memory and never become original snapshots, approved capsule pins or payloads.
Separate subprocess controls exercise the real CLI's fail-closed activation.
"""
import ast
import copy
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import zipfile

SOURCE = Path(__file__).resolve().parents[3]
if str(SOURCE) not in sys.path:
    sys.path.insert(0, str(SOURCE))
from tools.build import distribution_b_v2_contract as contract
from tools.phase_b import v2_localization_authority as adapter

ORIGINAL_ROOT = Path(os.environ.get("ATD_V2_TEST_ORIGINAL_ROOT",
    os.environ.get("ACHIEVETODO_TEST_INPUTS_DIR",
                   "E:/Vibecode/AchieveToDo_S5_R6_Work/assets/inputs/original")))
TEMP_ROOT = Path(os.environ.get("ATD_V2_TEST_WORK", str(
    SOURCE / "build/tmp/gate_b_v2_owner_a_implementation_20261028/python-fixtures")))


def vectors():
    """Construct pure semantic-unit vectors, not response/capture replacements."""
    policy = contract.policy(SOURCE)
    rows = policy["sources"]
    result = []
    for row in rows:
        acq = json.loads((ORIGINAL_ROOT / (row["prefix"] + "_acquisition.json")).read_bytes())
        project = {"id": row["projectId"], "license": {"id": row["expectedLicenseId"]}}
        version = {"id": row["versionId"], "project_id": row["projectId"],
                   "version_number": row["versionLabel"], "date_published": row["publishedAt"],
                   "game_versions": row["recordedGameVersions"],
                   "loaders": row.get("recordedLoaders", ["minecraft"]),
                   "files": [{"filename": row["originalArchiveFilename"],
                              "url": row["archiveURL"], "size": row["archiveBytes"],
                              "hashes": dict(row["archiveHashes"])}]}
        result.append((row, project, version, acq, (ORIGINAL_ROOT / row["expectedArchiveName"]).read_bytes()))
    return result


def encoded(value):
    return json.dumps(value, ensure_ascii=False).encode("utf-8")


def b7_module():
    spec = importlib.util.spec_from_file_location("atd_v2_b7_unit_original", SOURCE / adapter.B7_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    # Explicit source identity rather than inherited location-discovery state.
    module.ROOT = SOURCE
    module.PHASE = SOURCE / "reference/phase_b"
    return module


def pure_adapter():
    module = b7_module()
    views, frames, identities = {}, {}, {}
    for row, project, version, acq, archive in vectors():
        pb, vb = encoded(project), encoded(version)
        prefix = row["prefix"]
        views[prefix] = {"project": project, "versions": [version]}
        frames[prefix] = contract.metadata_pair_binding(pb, vb)
        identities[prefix] = {"scope": "SYNTHETIC_PURE_UNIT_VECTOR_NOT_CAPTURE",
                              "projectSHA256": contract.sha(pb), "versionSHA256": contract.sha(vb)}
    function, audit = adapter.build_adapter(module, views, frames, identities)
    ru = json.loads((SOURCE / "src/main/resources/assets/minecraft/lang/ru_ru.json").read_bytes())
    manifest = json.loads((SOURCE / "reference/phase_b/b6_ru_translation_manifest.json").read_bytes())
    records = manifest["perChangedKeyProvenance"]
    changed = [r["key"] for r in records]
    requirements = [{"key": r["key"], "consumers": r["consumerContexts"]} for r in records]
    return module, function, audit, ru, changed, requirements, views


def clean_env():
    env = dict(os.environ)
    for key in list(env):
        if key.startswith("ACHIEVETODO_GATE_B") or key in {
            "ACHIEVETODO_INPUT_AUTHORITY", "ACHIEVETODO_ALTERNATIVE_MANIFEST",
            "ACHIEVETODO_SOURCE_ORACLE", "ACHIEVETODO_SOURCE_ORACLE_FILE"}:
            env.pop(key, None)
    env["PYTHONDONTWRITEBYTECODE"] = "1"
    env["ACHIEVETODO_UI_UX_CERTIFICATION_AUTHORITY"] = "ATD_UI_UX_MAINTENANCE_V1"
    env["ACHIEVETODO_UI_UX_CERTIFICATION_FILE"] = str(SOURCE / "tools/certification/authority/ui-ux-maintenance-v1.json")
    return env


class PureContractTests(unittest.TestCase):
    def test_default_has_no_v2_authority(self):
        self.assertIsNone(contract.validate_selection(SOURCE, {}))

    def test_duplicate_keys_and_non_json_reject(self):
        for raw in [b'{"id":"one","id":"two"}', b'{', b'\xff', b'NaN']:
            with self.subTest(raw=raw), self.assertRaises((ValueError, UnicodeError)):
                contract.strict_json(raw)

    def test_pair_binding_is_domain_separated_unambiguous_and_byte_exact(self):
        a = contract.metadata_pair_binding(b"ab", b"c")
        b = contract.metadata_pair_binding(b"a", b"bc")
        self.assertNotEqual(a, b)
        self.assertNotEqual(a, b"abc")
        self.assertNotEqual(a, contract.metadata_pair_binding(b"ab\n", b"c"))

    def test_original_fifteen_unchanged_unit_precondition(self):
        old = json.loads((SOURCE / "src/test/resources/build_inputs/manifest.json").read_bytes())
        rows = [r for r in old["entries"] if r["type"] != "metadataJSON"]
        self.assertEqual(15, len(rows))
        for row in rows:
            raw = (ORIGINAL_ROOT / row["logicalName"]).read_bytes()
            self.assertEqual(row["size"], len(raw))
            self.assertEqual(row["SHA256"], contract.sha(raw))

    def test_synthetic_semantic_pairs_validate_without_input_authority(self):
        for row, project, version, acq, archive in vectors():
            with self.subTest(project=row["projectId"]):
                result = contract.validate_pair(SOURCE, row, encoded(project), encoded(version), acq, archive)
                self.assertIsInstance(result, dict)
        self.assertIsNone(contract.validate_selection(SOURCE, {}))

    def test_project_version_archive_and_license_mutations_reject(self):
        for mutation in ["project", "version", "version-project", "license", "filename", "hash", "size", "url", "game-version", "publication", "duplicate-file"]:
            row, project, version, acq, archive = copy.deepcopy(vectors()[0])
            if mutation == "project": project["id"] = "OTHER"
            elif mutation == "version": version["id"] = "OTHER"
            elif mutation == "version-project": version["project_id"] = "OTHER"
            elif mutation == "license": project["license"]["id"] = "OTHER"
            elif mutation == "filename": version["files"][0]["filename"] = "OTHER.zip"
            elif mutation == "hash": version["files"][0]["hashes"]["sha1"] = "0" * 40
            elif mutation == "size": version["files"][0]["size"] += 1
            elif mutation == "url": version["files"][0]["url"] = "https://example.invalid/untrusted.zip"
            elif mutation == "game-version": version["game_versions"] = ["OTHER"]
            elif mutation == "publication": version["date_published"] = "1900-01-01T00:00:00Z"
            elif mutation == "duplicate-file": version["files"].append(copy.deepcopy(version["files"][0]))
            with self.subTest(mutation=mutation), self.assertRaises((ValueError, KeyError, TypeError)):
                contract.validate_pair(SOURCE, row, encoded(project), encoded(version), acq, archive)

    def test_same_size_archive_and_duplicate_metadata_reject(self):
        row, project, version, acq, archive = vectors()[0]
        altered = bytes([archive[0] ^ 1]) + archive[1:]
        with self.assertRaises(ValueError):
            contract.validate_pair(SOURCE, row, encoded(project), encoded(version), acq, altered)
        raw = b'{"id":"' + row["projectId"].encode() + b'","id":"OTHER"}'
        with self.assertRaises(ValueError):
            contract.validate_pair(SOURCE, row, raw, encoded(version), acq, archive)

    def test_safe_paths_reject_traversal_and_aliases(self):
        TEMP_ROOT.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(prefix="atd-v2-path-control-", dir=TEMP_ROOT) as name:
            root = Path(name)
            (root / "plain").write_bytes(b"pure unit fixture")
            self.assertEqual(b"pure unit fixture", contract.safe_file(root, "plain").read_bytes())
            for relative in ["../plain", "C:/escape", "plain:stream", "missing"]:
                with self.subTest(relative=relative), self.assertRaises((ValueError, OSError)):
                    contract.safe_file(root, relative)
            os.link(root / "plain", root / "hardlink")
            with self.assertRaises(ValueError):
                contract.safe_file(root, "hardlink")

    def test_archive_path_duplicate_and_symlink_controls(self):
        for name in ["../escape.json", "absolute:stream"]:
            stream = io.BytesIO()
            with zipfile.ZipFile(stream, "w") as archive:
                archive.writestr(name, b"PURE_UNIT_BAD_ZIP")
            with self.subTest(name=name), self.assertRaises(ValueError):
                contract._zip_check(stream.getvalue(), "pure-unit-negative.zip")
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, "w") as archive:
            archive.writestr("folder/escape.json", b"PURE_UNIT_BAD_ZIP")
        # Python normalizes a supplied Windows separator when constructing a
        # ZipInfo. Change only the equal-length raw header names so the control
        # actually contains a backslash in the stored ZIP central/local records.
        malformed = stream.getvalue().replace(b"folder/escape.json", b"folder\\escape.json")
        with self.assertRaises(ValueError):
            contract._zip_check(malformed, "pure-unit-raw-backslash.zip")
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, "w") as archive:
            archive.writestr("dictionary.json", b"one")
            archive.writestr("DICTIONARY.JSON", b"two")
        with self.assertRaises(ValueError):
            contract._zip_check(stream.getvalue(), "pure-unit-case-collision.zip")
        stream = io.BytesIO()
        entry = zipfile.ZipInfo("dictionary-link")
        entry.create_system = 3
        entry.external_attr = 0o120777 << 16
        with zipfile.ZipFile(stream, "w") as archive:
            archive.writestr(entry, b"../outside")
        with self.assertRaises(ValueError):
            contract._zip_check(stream.getvalue(), "pure-unit-symlink.zip")

    def test_environment_hash_override_cannot_replace_reviewed_pin(self):
        env = clean_env()
        env[contract.SELECTOR] = contract.AUTHORITY
        env[contract.APPROVAL_FILE] = str(SOURCE / contract.CAPSULE_PATH)
        baseline = dict(env)
        env["ACHIEVETODO_GATE_B_EXPECTED_SHA256"] = contract.sha((SOURCE / contract.CAPSULE_PATH).read_bytes())
        env["ACHIEVETODO_GATE_B_OWNER_B_APPROVED"] = "true"
        # Exercise the real literal trust anchor. The future standard suite must
        # accept a genuinely approved capsule, but an environment hash/boolean
        # must never turn an inactive anchor into approval or replace its bytes.
        if not contract.OWNER_B_CAPSULE_SHA256:
            with self.assertRaisesRegex(ValueError, "OWNER_B_REQUIRED"):
                contract.validate_selection(SOURCE, env)
            with self.assertRaisesRegex(ValueError, "OWNER_B_REQUIRED"):
                contract.validate_selection(SOURCE, baseline)
        else:
            actual = contract.validate_selection(SOURCE, baseline)
            self.assertEqual(actual, contract.validate_selection(SOURCE, env))
            self.assertEqual("APPROVED_OWNER_B", actual["state"])
            self.assertTrue(actual["activationApproved"])
        env[contract.APPROVAL_FILE] = str(SOURCE / "unapproved-foreign-capsule.json")
        with self.assertRaisesRegex(ValueError, "wrong/noncanonical approval path"):
            contract.validate_selection(SOURCE, env)

    def test_transport_receipt_exact_binding_and_mutations(self):
        row = contract.policy(SOURCE)["sources"][0]
        body = b'{"scope":"SYNTHETIC_PURE_UNIT_TRANSPORT_NOT_CAPTURE"}'
        name = row["projectRawName"]
        receipt = {
            "schema": 1, "name": name, "attempt": 1,
            "requestURL": row["projectEndpoint"], "finalURL": row["projectEndpoint"],
            "method": "GET", "tlsValidation": True,
            "requestHeaders": {"Accept": "application/json", "Accept-Encoding": "identity"},
            "responseHeaders": {"Content-Type": "application/json", "Content-Encoding": "identity",
                                "Location": None, "Content-Length": str(len(body))},
            "startedUTC": "2026-10-28T00:00:00+00:00",
            "finishedUTC": "2026-10-28T00:00:01+00:00",
            "status": 200, "result": "RAW_CAPTURE_ONLY_NOT_TRUSTED",
            "bodyBytes": len(body), "bodySHA256": contract.sha(body), "error": None,
        }
        contract.validate_transport_receipt(row, name, receipt, body)
        for mutation in ["url", "final", "tls", "media", "encoding", "redirect", "status",
                         "time-order", "time-limit", "name", "hash", "attempt", "length", "method"]:
            changed = copy.deepcopy(receipt)
            if mutation == "url": changed["requestURL"] = "https://example.invalid/substitute"
            elif mutation == "final": changed["finalURL"] = "https://example.invalid/substitute"
            elif mutation == "tls": changed["tlsValidation"] = False
            elif mutation == "media": changed["responseHeaders"]["Content-Type"] = "text/html"
            elif mutation == "encoding": changed["responseHeaders"]["Content-Encoding"] = "gzip"
            elif mutation == "redirect": changed["responseHeaders"]["Location"] = row["projectEndpoint"]
            elif mutation == "status": changed["status"] = 404
            elif mutation == "time-order": changed["finishedUTC"] = "2026-10-27T23:59:59+00:00"
            elif mutation == "time-limit": changed["finishedUTC"] = "2026-10-28T00:01:00+00:00"
            elif mutation == "name": changed["name"] = "OTHER.v2.raw.json"
            elif mutation == "hash": changed["bodySHA256"] = "0" * 64
            elif mutation == "attempt": changed["attempt"] = 3
            elif mutation == "length": changed["bodyBytes"] += 1
            elif mutation == "method": changed["method"] = "POST"
            with self.subTest(mutation=mutation), self.assertRaises((ValueError, KeyError, TypeError)):
                contract.validate_transport_receipt(row, name, changed, body)


class AdapterTests(unittest.TestCase):
    def test_exact_two_expressions_all_other_ast_and_assertions_preserved(self):
        raw = (SOURCE / adapter.B7_PATH).read_bytes()
        function, audit = adapter.transform_pinned_source(raw)
        self.assertEqual(2, audit["adaptedExpressions"])
        self.assertGreater(audit["preservedAssertions"], 20)
        self.assertTrue(audit["allOtherASTUnchanged"])
        self.assertEqual(audit["preservedAssertions"], len(adapter.assertion_inventory(function)))
        self.assertEqual(adapter.B7_SHA256, hashlib.sha256(raw).hexdigest())

    def test_changed_original_b7_cannot_be_adapted(self):
        raw = (SOURCE / adapter.B7_PATH).read_bytes()
        with self.assertRaises(ValueError):
            adapter.transform_pinned_source(raw + b"\n# unauthorized")

    def test_real_original_provenance_assertions_execute_with_pure_unit_views(self):
        module, function, audit, ru, changed, requirements, views = pure_adapter()
        self.assertIsNot(module.provenance_certification, function)
        result = function(ru, changed, requirements, ORIGINAL_ROOT)
        self.assertEqual({"RETAINED_PHASE_A": 3186, "OFFICIAL_LP_1_21_PERMISSION": 211,
                          "OTHER_PERMITTED_SOURCE": 21, "MANUAL_B6": 64}, result["partition"])
        self.assertEqual("V2_RAW_PAIR_BINDING", result["metadataBindingKind"])
        self.assertEqual(5, len(result["rawMetadataIdentities"]))
        self.assertEqual(296, len(result["changedValueRecords"]))
        self.assertEqual(0, result["hashMismatch"])
        self.assertEqual(audit, result["consumerAdapterAudit"])
        self.assertFalse(any((ORIGINAL_ROOT / (prefix + "_metadata.json")).exists() for prefix in views))

    def test_original_wrong_value_assertion_still_rejects(self):
        module, function, audit, ru, changed, requirements, views = pure_adapter()
        ru[changed[0]] = "unauthorized changed value"
        with self.assertRaises(AssertionError):
            function(ru, changed, requirements, ORIGINAL_ROOT)

    def test_original_same_label_wrong_version_and_source_checks_still_execute(self):
        module, function, audit, ru, changed, requirements, views = pure_adapter()
        frames = {p: b"SYNTHETIC_PURE_UNIT_VECTOR_NOT_AUTHORITY" for p in views}
        views["bacap-language-pack"]["versions"][0]["id"] = "OTHER_SAME_LABEL"
        broken, unused = adapter.build_adapter(module, views, frames, {})
        with self.assertRaises(StopIteration):
            broken(ru, changed, requirements, ORIGINAL_ROOT)
        module, function, audit, ru, changed, requirements, views = pure_adapter()
        views["bacap-language-pack"]["project"]["id"] = "OTHER"
        broken, unused = adapter.build_adapter(module, views, frames, {})
        with self.assertRaises(AssertionError):
            broken(ru, changed, requirements, ORIGINAL_ROOT)

    def test_production_bind_rejects_unknown_authority_in_every_context(self):
        env = clean_env()
        env["ACHIEVETODO_GATE_B_AUTHORITY"] = "UNKNOWN"
        env["ACHIEVETODO_GATE_B_APPROVAL_FILE"] = str(SOURCE / "tools/certification/authority/distribution-b-v2-approval.json")
        command = [sys.executable, "-B", "-c",
                   "import importlib.util;from pathlib import Path;from tools.phase_b.v2_localization_authority import bind;"
                   "p=Path('tools/phase_b/b7_localization_certification.py');s=importlib.util.spec_from_file_location('b7',p);"
                   "m=importlib.util.module_from_spec(s);s.loader.exec_module(m);"
                   "m.ROOT=Path.cwd().resolve();m.PHASE=m.ROOT/'reference/phase_b';"
                   "bind(m,r'" + str(ORIGINAL_ROOT) + "')"]
        result = subprocess.run(command, cwd=SOURCE, env=env, capture_output=True, text=True)
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn("EXTERNAL_INPUTS_V2_PASS", result.stdout + result.stderr)
        self.assertIn("unknown/orphan selector", result.stdout + result.stderr)

    def test_actual_capsule_boundary_uses_real_reviewed_pin_and_supplied_inputs(self):
        env = clean_env()
        env[contract.SELECTOR] = contract.AUTHORITY
        env[contract.APPROVAL_FILE] = str(SOURCE / contract.CAPSULE_PATH)
        if contract.OWNER_B_CAPSULE_SHA256:
            # A real approved standard run provides its verified twenty-five
            # inputs explicitly. Never substitute the historical fifteen here.
            supplied = os.environ.get("ACHIEVETODO_TEST_INPUTS_DIR")
            self.assertIsNotNone(supplied, "Approved V2 run needs explicit 25-file inputs")
        else:
            supplied = str(ORIGINAL_ROOT)  # Must reject at Owner B before reading inputs.
        command = [sys.executable, "-B", "-c",
                   "import importlib.util,sys;from pathlib import Path;"
                   "from tools.phase_b.v2_localization_authority import bind;"
                   "p=Path('tools/phase_b/b7_localization_certification.py');"
                   "s=importlib.util.spec_from_file_location('b7',p);"
                   "m=importlib.util.module_from_spec(s);s.loader.exec_module(m);"
                   "m.ROOT=Path.cwd().resolve();m.PHASE=m.ROOT/'reference/phase_b';"
                   "bind(m,sys.argv[1]);print('AUTHENTICATED_V2_CONSUMER_BOUND')", supplied]
        result = subprocess.run(command, cwd=SOURCE, env=env, capture_output=True, text=True)
        if contract.OWNER_B_CAPSULE_SHA256:
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)
            self.assertIn("AUTHENTICATED_V2_CONSUMER_BOUND", result.stdout)
        else:
            self.assertEqual(1, result.returncode)
            self.assertIn("OWNER_B_REQUIRED", result.stderr)
            self.assertNotIn("AUTHENTICATED_V2_CONSUMER_BOUND", result.stdout)

    def test_production_adapter_rejects_cross_checkout_context(self):
        module = b7_module()
        module.ROOT = SOURCE.parent
        with self.assertRaisesRegex(ValueError, "V2_ADAPTER_CROSS_CHECKOUT_ROOT"):
            adapter._activated_adapter(module, ORIGINAL_ROOT)
        module = b7_module()
        module.PHASE = SOURCE.parent / "reference/phase_b"
        with self.assertRaisesRegex(ValueError, "V2_ADAPTER_CROSS_CHECKOUT_PHASE"):
            adapter._activated_adapter(module, ORIGINAL_ROOT)


class IndependentProcessControls(unittest.TestCase):
    def test_candidate_mode_never_ignores_present_authority_selectors(self):
        for key, value in [(contract.SELECTOR, "UNKNOWN"), (contract.SELECTOR, ""),
                           (contract.APPROVAL_FILE, ""), ("ACHIEVETODO_INPUT_AUTHORITY", "")]:
            env = clean_env()
            env[key] = value
            result = subprocess.run([sys.executable, "-B", str(SOURCE / "tools/build/verify_inputs_v2.py"),
                                     "--source-root", str(SOURCE), "--candidate-report"],
                                    cwd=SOURCE, env=env, capture_output=True, text=True)
            with self.subTest(selector=key, value=value):
                self.assertEqual(1, result.returncode)
                self.assertIn("candidate diagnostic cannot ignore authority selectors", result.stderr)
                self.assertNotIn("EXTERNAL_INPUTS_V2_PASS", result.stdout + result.stderr)
            print("INDEPENDENT_V2_CANDIDATE_SELECTOR_REJECTED", key, "exit=" + str(result.returncode))

    def test_metadata_semantic_controls_use_real_separate_processes(self):
        script = """
import sys
from tools.build.tests.test_distribution_b_v2_contract import vectors, encoded, contract, SOURCE
row, project, version, acquisition, archive = vectors()[0]
control = sys.argv[1]
if control == 'project': project['id'] = 'OTHER'
elif control == 'license': project['license']['id'] = 'OTHER'
elif control == 'archive-hash': version['files'][0]['hashes']['sha1'] = '0' * 40
elif control != 'positive': raise ValueError('Unknown pure unit control')
contract.validate_pair(SOURCE, row, encoded(project), encoded(version), acquisition, archive)
print('PURE_UNIT_PAIR_CHECK_ONLY_NOT_INPUT_AUTHORITY')
"""
        for name, expected in [("positive", 0), ("project", 1), ("license", 1), ("archive-hash", 1)]:
            result = subprocess.run([sys.executable, "-B", "-c", script, name], cwd=SOURCE,
                                    env=clean_env(), capture_output=True, text=True)
            with self.subTest(control=name):
                self.assertEqual(expected, result.returncode, result.stdout + result.stderr)
                self.assertNotIn("EXTERNAL_INPUTS_V2_PASS", result.stdout + result.stderr)
                if expected:
                    self.assertIn("GATE_B_V2_REJECTED", result.stderr)
                else:
                    self.assertIn("PURE_UNIT_PAIR_CHECK_ONLY_NOT_INPUT_AUTHORITY", result.stdout)
            print("INDEPENDENT_V2_METADATA_CONTROL", name, "exit=" + str(result.returncode))

    def test_real_verifier_checks_each_selector_against_actual_authority_context(self):
        capsule = str(SOURCE / "tools/certification/authority/distribution-b-v2-approval.json")
        controls = {
            "unknown": {"ACHIEVETODO_GATE_B_AUTHORITY": "UNKNOWN", "ACHIEVETODO_GATE_B_APPROVAL_FILE": capsule},
            "orphan": {"ACHIEVETODO_GATE_B_APPROVAL_FILE": capsule},
            "mixed": {"ACHIEVETODO_GATE_B_AUTHORITY": contract.AUTHORITY,
                      "ACHIEVETODO_GATE_B_APPROVAL_FILE": capsule, "ACHIEVETODO_INPUT_AUTHORITY": "S5_R3_ALT_METADATA_V1"},
            "actual-capsule": {"ACHIEVETODO_GATE_B_AUTHORITY": contract.AUTHORITY, "ACHIEVETODO_GATE_B_APPROVAL_FILE": capsule},
        }
        for name, supplied in controls.items():
            env = clean_env(); env.update(supplied)
            command = [sys.executable, "-B", str(SOURCE / "tools/build/verify_inputs_v2.py"),
                       "--source-root", str(SOURCE), "--approval-only"]
            result = subprocess.run(command, cwd=SOURCE, env=env, capture_output=True, text=True)
            with self.subTest(control=name):
                approved_actual = name == "actual-capsule" and bool(contract.OWNER_B_CAPSULE_SHA256)
                self.assertEqual(0 if approved_actual else 1, result.returncode, result.stdout + result.stderr)
                self.assertNotIn("EXTERNAL_INPUTS_V2_PASS", result.stdout + result.stderr)
                self.assertIn("V2", result.stdout + result.stderr)
                if approved_actual:
                    self.assertEqual(contract.AUTHORITY, json.loads(result.stdout)["authority"])
                else:
                    diagnostic = {"unknown": "unknown/orphan selector", "orphan": "unknown/orphan selector",
                                  "mixed": "mixed historical/alternative selectors", "actual-capsule": "OWNER_B_REQUIRED"}[name]
                    self.assertIn(diagnostic, result.stdout + result.stderr)
            print("INDEPENDENT_V2_PROCESS_CONTEXT_CHECK", name, "exit=" + str(result.returncode))

    def test_changed_draft_capsule_cannot_replace_the_real_reviewed_capsule(self):
        TEMP_ROOT.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(prefix="atd-v2-draft-capsule-control-", dir=TEMP_ROOT) as name:
            source = Path(name).resolve()
            foreign = source / contract.CAPSULE_PATH
            foreign.parent.mkdir(parents=True)
            changed = json.loads((SOURCE / contract.CAPSULE_PATH).read_bytes())
            changed["state"] = "DRAFT_OWNER_A_NOT_OWNER_B"
            changed["activationApproved"] = False
            changed["testVectorKind"] = "DISPOSABLE_DRAFT_SUBSTITUTION_NOT_AUTHORITY"
            foreign.write_bytes(encoded(changed))
            env = clean_env()
            env[contract.SELECTOR] = contract.AUTHORITY
            env[contract.APPROVAL_FILE] = str(foreign)
            env["ACHIEVETODO_UI_UX_CERTIFICATION_FILE"] = str(source / contract.GATE_A_MANIFEST)
            command = [sys.executable, "-B", str(SOURCE / "tools/build/verify_inputs_v2.py"),
                       "--source-root", str(source), "--approval-only"]
            result = subprocess.run(command, cwd=SOURCE, env=env, capture_output=True, text=True)
            self.assertEqual(1, result.returncode)
            expected = "Owner B capsule digest" if contract.OWNER_B_CAPSULE_SHA256 else "OWNER_B_REQUIRED"
            self.assertIn(expected, result.stderr)
            self.assertNotIn("EXTERNAL_INPUTS_V2_PASS", result.stdout + result.stderr)
            print("INDEPENDENT_V2_MODIFIED_DRAFT_REJECTED", "exit=" + str(result.returncode))


if __name__ == "__main__":
    unittest.main(verbosity=2)
