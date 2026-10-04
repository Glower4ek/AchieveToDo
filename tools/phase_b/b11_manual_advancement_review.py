"""B11 factual packet and review accounting. Semantic verdicts require separate inspection.

Preparation always emits UNREVIEWED. It never derives a verdict from codec,
graph, hash, or reference validity. Normal output is ignored working storage.
"""
import argparse
import collections
import copy
import hashlib
import json
import pathlib
import re
import unittest
import zipfile
import b3_bacap_semantic_diff as b3

ROOT = pathlib.Path(__file__).resolve().parents[2]
TEMP = ROOT / 'build/tmp/phase_b_b11'
CLASSES = ('TARGET_ONLY', 'SEMANTICALLY_CHANGED')
VERDICTS = {'PASS', 'PASS_WITH_NOTE', 'FLAG_PRODUCT_DEFECT', 'FLAG_UPSTREAM_ANOMALY'}

def sha(data):
    return hashlib.sha256(data).hexdigest()

def load(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def semantic(value):
    return sha(json.dumps(value, sort_keys=True, separators=(',', ':'), ensure_ascii=False).encode('utf8'))

def corpus(diff):
    rows = [row for label in CLASSES for row in sorted(diff['advancements'], key=lambda x: x['resourceId']) if row['classification'] == label]
    ids = [row['resourceId'] for row in rows]
    assert len(ids) == len(set(ids)) == 734
    assert collections.Counter(row['classification'] for row in rows) == {'TARGET_ONLY':110, 'SEMANTICALLY_CHANGED':624}
    assert collections.Counter(row['targetCanonical'] for row in rows[:110]) == {True:97, False:13}
    return rows

def grouping(advancement):
    return sorted(sorted(group) for group in advancement.get('requirements', [[name] for name in advancement['criteria']]))

def changed_leaves(old, target, path=''):
    if old == target:
        return []
    if isinstance(old, dict) and isinstance(target, dict):
        return [item for key in sorted(set(old)|set(target)) for item in changed_leaves(old.get(key), target.get(key), path+'/'+key)]
    return [{'path':path, 'old':old, 'target':target}]

def component_keys(value):
    if isinstance(value, dict):
        return ([value['translate']] if 'translate' in value else []) + [key for item in value.values() for key in component_keys(item)]
    if isinstance(value, list):
        return [key for item in value for key in component_keys(item)]
    return []

def current_reference_index():
    """Current executable resource references, separate from historical B3 occurrences."""
    index = collections.defaultdict(list)
    for path in sorted((ROOT/'src/main/resources/resourcepacks').rglob('*.mcfunction')):
        digest = sha(path.read_bytes())
        for number, line in enumerate(path.read_text(encoding='utf8').splitlines(), 1):
            if not line.strip() or line.lstrip().startswith('#'):
                continue
            for identifier in set(re.findall(r'(?:blazeandcave|minecraft):[a-z0-9_./-]+', line)):
                kinds = []
                if re.search(r'/advancementssearch highlight '+re.escape(identifier)+r'(?:\s|["\\])', line):
                    kinds.append('SEARCH_MESSAGE')
                if re.search(r'adv_id:["\']'+re.escape(identifier)+r'["\']', line):
                    kinds.append('WRAPPER_ARGUMENT')
                if kinds:
                    index[identifier].append({'path':path.relative_to(ROOT).as_posix(), 'line':number, 'kinds':kinds, 'sha256':digest})
    return index

def compact_facts(row):
    """Fingerprints and summaries only; never a semantic verdict or full resource body."""
    def criteria(value):
        if value is None:
            return None
        return {'count':len(value['criteria']), 'triggers':sorted({c['trigger'] for c in value['criteria'].values()}),
                'conditionSha256':semantic(value['criteria']), 'groupingSha256':semantic(grouping(value)),
                'groups':len(grouping(value)), 'orGroups':sum(len(g)>1 for g in grouping(value))}
    return {'resourceId':row['resourceId'], 'classification':row['classification'], 'canonical':row['targetCanonical'],
            'oldSemanticSha256':row['oldSemanticSha256'], 'targetSemanticSha256':row['targetSemanticSha256'],
            'changedFields':row['changedFields'], 'treeSummary':{'old':row['oldTree'], 'target':row['targetTree']},
            'displaySummary':{'present':bool(row['target'].get('display')), 'hidden':row['target'].get('display',{}).get('hidden',False),
                              'titleKeys':row['titleKeys'], 'descriptionKeys':row['descriptionKeys'], 'frame':row['target'].get('display',{}).get('frame','task')},
            'criteriaRequirementsSummary':{'old':criteria(row['old']), 'target':criteria(row['target'])},
            'rewardSummary':{'old':(row['old'] or {}).get('rewards'), 'target':row['target'].get('rewards')},
            'integrationReferences':{'historicalB3Occurrences':row['atdReferences'], 'currentResources':row['currentReferences'],
                                     'trackers':row['trackerBindings'], 'guiOrder':row['guiOrderBindings'], 'companions':row['companionOwners']},
            'riskFlags':row['riskFlags'], 'renameMoveContext':row['renameCandidateInfo'],
            'effectiveViews':row['effectivePresence'], 'mainEffectiveSha256':row['mainEffectiveSha256'],
            'provenance':{'b3ResourceId':row['resourceId'], 'targetPath':row['targetPath'], 'targetByteSha256':row['targetByteSha256'],
                          'oldPath':row['oldPath'], 'oldByteSha256':row['oldByteSha256']}}

def coverage(rows):
    common = [r for r in rows if r['classification']=='SEMANTICALLY_CHANGED']
    result = {field:sum(bool(r['fieldChanges'][field]) for r in common) for field in ['parent','display','criteria','requirements','rewards']}
    result['effectiveGrouping'] = sum(r['fieldChanges']['requirementsDetails']['completionGroupingSemanticsChanged'] for r in common)
    return result

def reconcile(rows, reviews):
    expected = {r['resourceId'] for r in rows}
    assert len(reviews)==len(expected)==734
    assert {r['resourceId'] for r in reviews}==expected
    assert len({r['resourceId'] for r in reviews})==len(reviews)
    for row in reviews:
        assert row['manualVerdict'] in VERDICTS
        assert isinstance(row['manualReason'], str) and len(row['manualReason'].strip()) >= 25
        assert row.get('inspectedFacts'), 'Explicit inspected factual anchors required'
    duplicates = collections.Counter(r['manualReason'] for r in reviews)
    assert max(duplicates.values())<=4, 'Repeated reasons need individual inspection'
    return dict(collections.Counter(r['manualVerdict'] for r in reviews))

def reason_quality(packets, reviews):
    """Detect empty/template-only notes. Lexical anchors do not prove semantic correctness."""
    by_id={r['resourceId']:r for r in reviews}
    excluded={'minecraft','blazeandcave','bacap','rewards','criterion','criteria','trigger','conditions','entity','player',
              'reward','location','advancement','advancements','block','blocks','display','title','description','change',
              'changed','parent','false','true','green','task','goal','challenge','technical','type','specific','equipment',
              'mainhand','inventory','hidden','items','item','killed'}
    records=[]
    for row in packets:
        facts=' '.join([row['resourceId'],*row['titleKeys'],*row['descriptionKeys'],json.dumps(row['target']['criteria']),
                        str(row['target'].get('parent','')),str(row['target'].get('rewards',''))])
        tokens={t.lower() for t in re.findall('[A-Za-z0-9]+',facts) if (len(t)>=4 or t.isdigit()) and t.lower() not in excluded}
        reason_tokens={t.lower() for t in re.findall('[A-Za-z0-9]+',by_id[row['resourceId']]['manualReason'])}
        anchors=sorted(tokens&reason_tokens)
        assert anchors, 'Reason lacks a concrete source anchor: '+row['resourceId']
        records.append({'resourceId':row['resourceId'], 'sourceFactAnchors':anchors})
    return records

def prepare():
    diffpath=ROOT/'reference/phase_b/b3_advancement_diff.json';diff=load(diffpath);rows=corpus(diff)
    oldpath=ROOT/'reference/phase_a_preservation/files/final/bacap.zip'
    targetpath=ROOT/"build/tmp/phase_b_b1/BlazeandCave's Advancements Pack 1.21.zip"
    assert sha(oldpath.read_bytes())=='8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70'
    assert sha(targetpath.read_bytes())=='c71d1aa1a84dbe00a3f85a42144b46214c4669a3cccf07ff66631d28f16a99b2'
    assert hashlib.sha1(targetpath.read_bytes()).hexdigest()=='14da3f07b5467e8b59ffc0253fd8212c938cd739'
    b8path=ROOT/'reference/phase_b/b8_static_certification_resumed.json';b8=load(b8path);views={}
    for name,receipt in b8['effectiveViews'].items():
        p=ROOT/'build/tmp/phase_b_b8/post_r2'/('codec_advancements_'+name+'.json')
        assert sha(p.read_bytes())==receipt['codec']['advancementInputsSha256']
        views[name]=load(p)
    b4=load(ROOT/'reference/phase_b/b4_product_change_manifest.json')
    b56=load(ROOT/'reference/phase_b/b5_6_companions_resumed.json')
    packets=[]
    current_refs=current_reference_index()
    with zipfile.ZipFile(oldpath) as oldzip, zipfile.ZipFile(targetpath) as targetzip:
        for n,row in enumerate(rows):
            identifier=row['resourceId'];ns,p=identifier.split(':');target_resource=f'data/{ns}/advancement/{p}.json'
            raw=targetzip.read(target_resource);target=json.loads(raw)
            old_resource=f'data/{ns}/advancement/{p}.json'
            oldraw=oldzip.read(old_resource) if row['oldPresent'] else None
            old=b3.json_parse(oldraw, old_resource, 'old', []) if oldraw else None
            assert sha(raw)==row['targetByteSha256']
            assert not oldraw or sha(oldraw)==row['oldByteSha256']
            assert semantic(target)==row['targetSemanticSha256']
            assert old is None or semantic(old)==row['oldSemanticSha256']
            expected=copy.deepcopy(target)
            if 'display' in expected:expected['display']['announce_to_chat']=False
            effective=views['main'].get(identifier)
            drift=changed_leaves(expected,effective)
            fields=[field for field in sorted(set(old or {})|set(target)) if (old or {}).get(field)!=target.get(field)]
            display=target.get('display',{})
            owners=[{k:merge[k] for k in ['companion','path','resultingSha256','fieldsMerged']} for merge in b56['advancementMerges']['records'] if merge['advancementId']==identifier]
            tracker=[r['targetBinding'] for r in b4['trackers'] if r['targetAdvancementId']==identifier]
            gui=[r for r in b4['treeGuiSearch']['orderedChildren'] if r['resourceId']==identifier]
            flags=[]
            for label,flag in [('targetOnlyCanonical',row['classification']=='TARGET_ONLY' and row['targetCanonical']),('minecraftNamespace',ns=='minecraft'),('tracker',bool(tracker)),('guiOrder',bool(gui)),('companionMerge',bool(owners)),('effectiveDrift',bool(drift))]:
                if flag:flags.append(label)
            flags.extend('changed:'+field for field in fields)
            if current_refs.get(identifier):flags.append('currentSearchMessageOrWrapper')
            if row['renameCandidateInfo'] and row['renameCandidateInfo'].get('pairs'):flags.append('renameMoveCandidate')
            if row['treeChanges'] and any(row['treeChanges'].get(k) for k in ['root','category']):flags.append('rootCategoryChanged')
            if 'criteria' in fields and 'requirements' in fields:flags.append('criteriaAndRequirementsChanged')
            if p.startswith('technical/') and display:flags.append('technicalWithDisplay')
            if not p.startswith('technical/') and not display:flags.append('nonTechnicalWithoutDisplay')
            if row['fieldChanges'] and row['fieldChanges']['requirementsDetails']['completionGroupingSemanticsChanged']:flags.append('effectiveGroupingChanged')
            packets.append({'ordinal':n+1,**row,'targetPath':target_resource,'oldPath':old_resource if oldraw else None,'target':target,'old':old,'changedLeaves':changed_leaves(old,target) if old else [],'changedFields':fields,'titleKeys':component_keys(display.get('title')),'descriptionKeys':component_keys(display.get('description')),'criteriaCount':len(target['criteria']),'triggerTypes':sorted({c['trigger'] for c in target['criteria'].values()}),'effectiveGrouping':grouping(target),'effectivePresence':{name:identifier in view for name,view in views.items()},'mainEffectiveSha256':semantic(effective),'mainSemanticDrift':drift,'trackerBindings':tracker,'guiOrderBindings':gui,'companionOwners':owners,'riskFlags':flags,'manualVerdict':'UNREVIEWED','manualReason':''})
            packets[-1]['currentReferences']=current_refs.get(identifier,[])
    TEMP.mkdir(exist_ok=True)
    (TEMP/'packet.json').write_text(json.dumps(packets,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    ids=[r['resourceId'] for r in rows]
    manifest={'total':734,'targetOnly':110,'semanticallyChanged':624,'orderedCorpusSha256':sha(('\n'.join(ids)+'\n').encode()),'coverage':coverage(rows),'sourceHashes':{'old':sha(oldpath.read_bytes()),'target':sha(targetpath.read_bytes()),'b3':sha(diffpath.read_bytes()),'b8':sha(b8path.read_bytes())},'unexplainedMainEffectiveDrift':[r['resourceId'] for r in packets if r['mainSemanticDrift']]}
    (TEMP/'packet_manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf8')
    return packets, manifest

class PacketTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.diff=load(ROOT/'reference/phase_b/b3_advancement_diff.json');cls.rows=corpus(cls.diff)
        cls.packets,cls.manifest=prepare()
    def test_classification_and_order(self):
        self.assertTrue(all(r['classification']=='TARGET_ONLY' for r in self.rows[:110]))
        self.assertEqual([r['resourceId'] for r in self.rows[110:]],sorted(r['resourceId'] for r in self.rows[110:]))
    def test_734_arithmetic(self):self.assertEqual(len(self.rows),110+624)
    def test_canonical_split(self):self.assertEqual(sum(r['targetCanonical'] for r in self.rows[:110]),97)
    def test_field_coverage(self):self.assertEqual(coverage(self.rows),{'parent':34,'display':55,'criteria':574,'requirements':25,'rewards':27,'effectiveGrouping':106})
    def test_default_and_explicit_grouping(self):
        self.assertEqual(grouping({'criteria':{'a':{},'b':{}}}),[['a'],['b']])
        self.assertEqual(grouping({'criteria':{'a':{},'b':{}},'requirements':[['a','b']]}),[['a','b']])
    def test_component_extraction(self):self.assertEqual(component_keys({'translate':'outer','with':[{'translate':'inner'}]}),['outer','inner'])
    def test_change_leaf_extraction(self):self.assertEqual(changed_leaves({'parent':'a'},{'parent':'b'}),[{'path':'/parent','old':'a','target':'b'}])
    def test_unreviewed_rejected(self):
        with self.assertRaises(AssertionError):reconcile(self.rows,[{'resourceId':r['resourceId'],'manualVerdict':'UNREVIEWED','manualReason':''} for r in self.rows])
    def test_duplicate_and_missing_rejected(self):
        with self.assertRaises(AssertionError):reconcile(self.rows,[])
        with self.assertRaises(AssertionError):reconcile(self.rows,[{'resourceId':self.rows[0]['resourceId']}]*734)
    def test_no_verdict_generated_by_preparation(self):
        self.assertTrue(all(r['manualVerdict']=='UNREVIEWED' for r in self.packets));self.assertEqual(self.manifest['unexplainedMainEffectiveDrift'],[])
    def test_compact_record_schema(self):
        for row in self.packets:
            facts=compact_facts(row)
            self.assertEqual(facts['criteriaRequirementsSummary']['target']['count'],len(row['target']['criteria']))
            self.assertIn('provenance',facts)
            self.assertNotIn('target',facts)
            self.assertNotIn('manualVerdict',facts)
    def test_final_manual_verdict_reconciliation(self):
        reviews=[r for p in sorted(TEMP.glob('reviews_*.json')) for r in load(p)]
        if not reviews:
            self.skipTest('Manual inspection not yet recorded')
        totals=reconcile(self.rows,reviews)
        self.assertEqual(sum(totals.values()),734)
        self.assertEqual([r['ordinal'] for r in reviews],list(range(1,735)))
        self.assertFalse(totals.get('FLAG_PRODUCT_DEFECT',0))
    def test_final_reasons_have_source_facts(self):
        reviews=[r for p in sorted(TEMP.glob('reviews_*.json')) for r in load(p)]
        self.assertEqual(len(reason_quality(self.packets,reviews)),734)

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--self-test',action='store_true');args=parser.parse_args()
    if args.self_test:unittest.main(argv=['b11_manual_advancement_review'],verbosity=2)
    else:
        _,manifest=prepare();print(json.dumps(manifest,ensure_ascii=False))
