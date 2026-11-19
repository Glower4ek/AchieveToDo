"""Authenticated V2 raw-pair adapter for the unchanged historical B7 consumer.

No combined metadata file is generated. Only two exact expression nodes in the
literal-pinned B7 provenance function are adapted; every assertion is retained.
The pure transform is testable without granting an input authority. ``bind`` is
the production entrypoint and requires independently approved Owner B activation.
"""
import ast
import copy
import hashlib
from pathlib import Path

from tools.build import distribution_b_v2_contract as contract

B7_PATH = "tools/phase_b/b7_localization_certification.py"
B7_SHA256 = "7da243ade2e508c66395f9eeeacc02cde3e59409d36cff24573034f1b2123554"
_LOAD = "load(b6temp/(prefix+'_metadata.json'))"
_BYTES = "(b6temp/(prefix+'_metadata.json')).read_bytes()"
_LOAD_NAME = "__atd_v2_metadata_view"
_BYTES_NAME = "__atd_v2_raw_pair_binding"


def _dump(node):
    return ast.dump(node, include_attributes=False)


def _expression(text):
    return ast.parse(text, mode="eval").body


def assertion_inventory(function):
    """All assertion ASTs, with no normalization of their conditions/messages."""
    return [_dump(n) for n in ast.walk(function) if isinstance(n, ast.Assert)]


def transform_pinned_source(raw):
    """Return only the adapted function AST and a structural audit.

    This does not evaluate metadata, authenticate a capsule or grant V2 PASS.
    Nonmatching bytes, ambiguous function names or expression counts reject.
    """
    contract.require(hashlib.sha256(raw).hexdigest() == B7_SHA256,
                     "V2_ADAPTER_B7_SOURCE_IDENTITY")
    tree = ast.parse(raw.decode("utf-8"), filename=B7_PATH)
    functions = [n for n in tree.body if isinstance(n, ast.FunctionDef)
                 and n.name == "provenance_certification"]
    contract.require(len(functions) == 1, "V2_ADAPTER_FUNCTION_CARDINALITY")
    original = functions[0]
    patterns = {_dump(_expression(_LOAD)): _LOAD_NAME,
                _dump(_expression(_BYTES)): _BYTES_NAME}
    counts = {_LOAD_NAME: 0, _BYTES_NAME: 0}

    class TwoExpressions(ast.NodeTransformer):
        def visit_Call(self, node):
            name = patterns.get(_dump(node))
            if name is not None:
                counts[name] += 1
                return ast.copy_location(ast.Call(func=ast.Name(id=name, ctx=ast.Load()),
                                                  args=[ast.Name(id="prefix", ctx=ast.Load())],
                                                  keywords=[]), node)
            return self.generic_visit(node)

    adapted = TwoExpressions().visit(copy.deepcopy(original))
    contract.require(counts == {_LOAD_NAME: 1, _BYTES_NAME: 1},
                     "V2_ADAPTER_TWO_EXPRESSION_CARDINALITY")
    assertions = assertion_inventory(original)
    contract.require(assertions and assertion_inventory(adapted) == assertions,
                     "V2_ADAPTER_ASSERTION_INVENTORY_CHANGED")

    class RestoreExpressions(ast.NodeTransformer):
        def visit_Call(self, node):
            if isinstance(node.func, ast.Name) and node.func.id in counts:
                expected = ast.Call(func=ast.Name(id=node.func.id, ctx=ast.Load()),
                                    args=[ast.Name(id="prefix", ctx=ast.Load())], keywords=[])
                contract.require(_dump(node) == _dump(expected), "V2_ADAPTER_RESOLVER_SHAPE")
                return copy.deepcopy(_expression(_LOAD if node.func.id == _LOAD_NAME else _BYTES))
            return self.generic_visit(node)

    restored = RestoreExpressions().visit(copy.deepcopy(adapted))
    contract.require(_dump(restored) == _dump(original), "V2_ADAPTER_UNAPPROVED_AST_DELTA")
    ast.fix_missing_locations(adapted)
    return adapted, {
        "scope": "PURE_AST_ADAPTATION_NOT_INPUT_AUTHORITY",
        "sourceSHA256": B7_SHA256,
        "originalFunctionASTSHA256": hashlib.sha256(_dump(original).encode()).hexdigest(),
        "adaptedFunctionASTSHA256": hashlib.sha256(_dump(adapted).encode()).hexdigest(),
        "adaptedExpressions": 2,
        "preservedAssertions": len(assertions),
        "assertionInventorySHA256": hashlib.sha256("\n".join(assertions).encode()).hexdigest(),
        "allOtherASTUnchanged": True,
    }


def build_adapter(module, metadata_views, raw_pair_bindings, raw_identities):
    """Pure consumer adapter; caller-supplied data is not an approved authority.

    Intended for structural/consumer unit tests and the authenticated ``bind``
    path. Data enters only the two designated expression sites. No original
    module globals or filesystem access functions are monkeypatched.
    """
    source = Path(module.__file__).read_bytes()
    function, audit = transform_pinned_source(source)
    contract.require(_LOAD_NAME not in module.__dict__ and _BYTES_NAME not in module.__dict__,
                     "V2_ADAPTER_RESERVED_GLOBAL_COLLISION")

    def metadata(prefix):
        contract.require(prefix in metadata_views, "V2_ADAPTER_MISSING_METADATA_VIEW")
        return copy.deepcopy(metadata_views[prefix])

    def binding(prefix):
        contract.require(prefix in raw_pair_bindings, "V2_ADAPTER_MISSING_RAW_BINDING")
        data = raw_pair_bindings[prefix]
        contract.require(isinstance(data, bytes), "V2_ADAPTER_BINDING_TYPE")
        return data

    namespace = dict(module.__dict__)
    namespace[_LOAD_NAME] = metadata
    namespace[_BYTES_NAME] = binding
    code = compile(ast.Module(body=[function], type_ignores=[]), B7_PATH + "[V2_TWO_IO_ADAPTER]", "exec")
    exec(code, namespace)
    original_assertions = namespace["provenance_certification"]

    def provenance(*args, **kwargs):
        result = original_assertions(*args, **kwargs)
        contract.require(isinstance(result, dict), "V2_ADAPTER_PROVENANCE_RESULT_TYPE")
        result["metadataBindingKind"] = "V2_RAW_PAIR_BINDING"
        result["rawMetadataIdentities"] = copy.deepcopy(raw_identities)
        result["consumerAdapterAudit"] = dict(audit)
        return result

    provenance.__name__ = "provenance_certification"
    provenance.__doc__ = "V2 raw-pair consumer: all pinned original B7 assertions retained."
    return provenance, audit


def _activated_adapter(module, input_root):
    """Fresh authentication and raw-view framing at every consumer boundary."""
    source = Path(module.__file__).resolve().parents[2]
    contract.require(Path(module.ROOT).resolve() == source,
                     "V2_ADAPTER_CROSS_CHECKOUT_ROOT")
    contract.require(Path(module.PHASE).resolve() == source / "reference/phase_b",
                     "V2_ADAPTER_CROSS_CHECKOUT_PHASE")
    root = Path(input_root)
    verified = contract.verify_activation(source, root)
    policy = contract.policy(source)
    rows = policy.get("sources", policy.get("newRawSources"))
    contract.require(isinstance(rows, list) and len(rows) == 5, "V2_ADAPTER_POLICY_SOURCES")
    pair_bytes = {}
    for row in rows:
        project = contract.safe_file(root, row["projectRawName"]).read_bytes()
        version = contract.safe_file(root, row["versionRawName"]).read_bytes()
        expected = verified["raw_bindings"][row["prefix"]]
        contract.require(contract.sha(project) == expected["projectSHA256"]
                         and contract.sha(version) == expected["versionSHA256"],
                         "V2_ADAPTER_RAW_CHANGED_DURING_BINDING")
        pair_bytes[row["prefix"]] = contract.metadata_pair_binding(project, version)
        contract.require(contract.sha(pair_bytes[row["prefix"]]) == expected["bindingSHA256"],
                         "V2_ADAPTER_FRAME_BINDING_CHANGED")
    adapted, audit = build_adapter(module, verified["metadata_views"], pair_bytes,
                                   verified["raw_bindings"])
    return adapted, audit


def bind(module, input_root):
    """Install only after Owner B; reauthenticate on each provenance invocation."""
    unused, audit = _activated_adapter(module, input_root)

    def authenticated_provenance(*args, **kwargs):
        fresh, unused_audit = _activated_adapter(module, input_root)
        return fresh(*args, **kwargs)

    module.provenance_certification = authenticated_provenance
    return audit
