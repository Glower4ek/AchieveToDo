"""Exact UI07 successor composition; no fallback, acquisition or mutable approvals."""
from pathlib import Path

AUTHORITY = 'ATD_UI07_SCOPED_SUCCESSOR_V1'
PATH = 'tools/certification/authority/ui07-successor-v1.json'
SELECTOR = 'ACHIEVETODO_UI07_AUTHORITY'
FILE = 'ACHIEVETODO_UI07_AUTHORITY_FILE'
PRODUCTION_PATH = 'src/main/java/com/diskree/achievetodo/injection/mixin/client/AdvancementsScreenMixin.java'
CODE_PATHS = frozenset({
    'tools/build/ui07_successor.py',
    'src/test/java/com/diskree/achievetodo/certification/Ui07NativeLayoutContractTest.java',
    'tools/certification/authority/ui07-owner-decision.json',
    'tools/certification/authority/ui07-independent-repair-review.json',
})

def validate(source, env, expected, gate_a):
    from tools.build import distribution_b_v2_contract as c
    source = Path(source).absolute()
    c.require(env.get(SELECTOR) == AUTHORITY and env.get(FILE) == str(source/PATH), 'UI07 unknown/orphan/path selector')
    raw = c.safe_file(source, PATH).read_bytes()
    c.require(c.sha(raw) == expected, 'UI07 manifest digest')
    m = c.strict_json(raw)
    c.require(set(m) == {'schema','authority','state','oldGateA','oldOwnerB','productionDelta','codeBindings','ownerDecision','independentReview'}, 'UI07 exact schema')
    c.require(m['schema'] == 1 and m['authority'] == AUTHORITY and m['state'] == 'AUTHORIZED_SCOPED_SUCCESSOR', 'UI07 state/identity')
    c.require(m['oldGateA'] == c.policy(source)['gateAManifestSHA256'] and m['oldOwnerB'] == c.OWNER_B_CAPSULE_SHA256, 'UI07 predecessor binding')
    delta = m['productionDelta']
    c.require(set(delta) == {'path','beforeSHA256','afterSHA256'} and delta['path'] == PRODUCTION_PATH, 'UI07 one-path delta')
    c.require(delta['beforeSHA256'] == gate_a['productionClosure'][PRODUCTION_PATH] and delta['beforeSHA256'] != delta['afterSHA256'], 'UI07 before/after identity')
    c.require(set(m['codeBindings']) == CODE_PATHS, 'UI07 exact code closure')
    for name, digest in m['codeBindings'].items():
        c.require(c.sha(c.safe_file(source,name).read_bytes()) == digest, 'UI07 code binding: '+name)
    for key,name in [('ownerDecision','tools/certification/authority/ui07-owner-decision.json'),('independentReview','tools/certification/authority/ui07-independent-repair-review.json')]:
        c.require(m[key] == {'path':name,'SHA256':m['codeBindings'][name]}, 'UI07 decision reference')
    owner=c.strict_json(c.safe_file(source,m['ownerDecision']['path']).read_bytes())
    c.require(owner['origin'] == 'human-owner:current-task-instruction' and owner['approvedScopedSuccessor'] is True and owner['allowReviewerRequestedUi07TechnicalRevisions'] is True, 'UI07 real owner scope record')
    review=c.strict_json(c.safe_file(source,m['independentReview']['path']).read_bytes())
    c.require(review['decision'] == 'PASS_EXACT_REVISED_REPAIR_TECHNICAL_REVIEW' and review['afterSHA256'] == delta['afterSHA256'] and review['beforeSHA256'] == delta['beforeSHA256'], 'UI07 independent reviewed bytes')
    c.require(review['reviewer'] == 'independent reviewer agent /root/ui07_review' and review['reviewerDidNotAuthorRepair'] is True, 'UI07 independent reviewer')
    result=dict(gate_a['productionClosure']);result[PRODUCTION_PATH]=delta['afterSHA256']
    return result, sorted(CODE_PATHS | {PATH})
