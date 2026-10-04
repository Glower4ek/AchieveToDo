"""Current Phase B regression successor. Probe product behavior through compiled production.
The frozen B3 instrumentation counts candidate families only; it is never product evidence.
Normal mode writes ignored candidates. Publishing a receipt is a separate reviewed file edit.
"""
import argparse, collections, hashlib, json, os, pathlib, subprocess, sys, unittest, zipfile
import xml.etree.ElementTree as ET
ROOT = next(p for p in pathlib.Path(__file__).resolve().parents if (p/'.git').exists())
sys.path.insert(0, str(ROOT/'tools/phase_b'))
import b7_localization_certification as b7
import b8_bacap_static_certification as b8
TEMP = ROOT/'build/tmp/phase_b_b9/resumed'
CONVERTER = 'src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java'
CONVERTER_SHA = '6ab0674902b4783c39c08dfd802cd0067d1436d973345834f092a001a4db9fec'
B7_SHA = '032c866f0eca68dbf7508c86034f00d6a06bf52f98e43efdee2c52e7fbc5a732'
B8_SHA = '18acf4be8234587916a72a2aa0a8b48bbacaccf796055b40b8a4cf76ff26936b'
ROOT_SHA = 'cbc432be35d5525001872c430877541cfa1fcad6'
MARKER = 'achievetodo_compatibility/compat_26_2.properties'
B8_RECEIPTS = {
 'bacap_1_21_static_receipt.json':'204b79d5ced64f57a85f2ff8981d8216f8e8e6b3f17e2f99e2f5f14672c81bf3',
 'bacap_1_21_codec_receipt.json':'eeddcb746eefe5175be3d1b2e9c30fe2cdfb99b79d2e8d290d00346aa9333a38',
 'bacap_1_21_target_inventory.json':'a8f76fc43819c875737c02be04885bbfb03b5ab69eab220358554a207d1a9fcf',
}
sha = b7.sha
load = b7.load
write = b7.write

def require(condition, message):
    if not condition:
        raise AssertionError(message)

def payload(path):
    with zipfile.ZipFile(path) as archive:
        return {i.filename:archive.read(i) for i in archive.infolist() if not i.is_dir() and i.filename != MARKER}

def compare_payload(left, right):
    a, b = payload(left), payload(right)
    changed = sorted(p for p in set(a)|set(b) if a.get(p) != b.get(p))
    return {'checked':len(set(a)|set(b)), 'changed':changed}

def suite_report(directory):
    authority = load(ROOT/'reference/phase_b/b7_post_r1_test_gate_resolution.json')
    baseline = set(authority['fullTestAfterR1']['acceptedBaselineNames'])
    historical = {r['failingTest'] for r in authority['records']}
    require(len(baseline)==4 and len(historical)==7, 'Exact failure authority')
    counts = {k:0 for k in ['tests','failures','errors','skipped']}
    rows, classes, xml_hashes = [], {}, {}
    for p in sorted(pathlib.Path(directory).glob('TEST-*.xml')):
        root = ET.parse(p).getroot()
        values = {k:int(root.get(k,0)) for k in counts}
        classes[root.get('name')] = values
        for k in counts:
            counts[k] += values[k]
        for case in root.findall('testcase'):
            failure = case.find('failure')
            if failure is not None:
                name = case.get('classname')+'.'+case.get('name')
                message = failure.get('message','')
                rows.append({'name':name, 'messageSummary':message[:350], 'messageSha256':sha(message.encode()),
                    'classification':'PRE_EXISTING_ACCEPTED_BASELINE_FAILURE' if name in baseline else
                    'EXPECTED_PHASE_B_HISTORICAL_ORACLE_STALENESS' if name in historical else 'UNRESOLVED'})
        xml_hashes[p.name] = sha(p.read_bytes())
    names = {r['name'] for r in rows}
    require(counts['tests']>0 and counts['errors']==0, 'Suite discovery/errors')
    require(len(rows)==counts['failures'] and names <= baseline|historical, 'New failure name')
    return {**counts, 'failureNames':sorted(names), 'failureRecords':rows, 'classCounts':classes,
        'xmlHashes':xml_hashes, 'partition':{'baseline':len(names&baseline), 'historical':len(names&historical),
        'new':0, 'unresolved':0}, 'previousHistoricalNowPassing':sorted(historical-names)}

def command(name, args):
    b8.TEMP = TEMP
    require(b8.java_command(name,args)==0, 'Command failed: '+name)

def precheck():
    state = b7.precheck()
    require(sha((ROOT/CONVERTER).read_bytes())==CONVERTER_SHA, 'Converter authority')
    for p,h in [('tools/phase_b/b7_localization_certification.py',B7_SHA),('tools/phase_b/b8_bacap_static_certification.py',B8_SHA)]:
        require(sha((ROOT/p).read_bytes())==h, p)
    m2 = load(ROOT/'reference/phase_b/b7_m2_current_consumer_set_refresh.json')
    require(m2['status']=='PHASE_B_B7_M2_SUCCESSOR_REFRESHED' and m2['readyToResumeB9'], 'M2 handoff')
    for name,h in B8_RECEIPTS.items():
        require(sha((ROOT/'src/test/resources/phase_b_certification'/name).read_bytes())==h, name)
    for spec in b8.SOURCE_INPUTS:
        source=ROOT/spec['source']
        require(sha(source.read_bytes())==spec['sourceSha256'], spec['enum'])
        require(hashlib.sha1(source.read_bytes()).hexdigest()==spec['sourceSha1'], spec['enum'])
    frozen=ROOT/'reference/phase_a_preservation/files/final/bacap.zip'
    require(sha(frozen.read_bytes())=='8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70', 'Frozen archive')
    for path,h in DIAGNOSTIC_SOURCES.items():
        require(sha((ROOT/path).read_bytes())==h, 'Candidate instrumentation dependency: '+path)
    return state

def java_probes():
    TEMP.mkdir(parents=True,exist_ok=True)
    (TEMP/'probe/converted').mkdir(parents=True,exist_ok=True)
    (TEMP/'B9Probe.java').write_text(JAVA_PROBE,encoding='utf8')
    (TEMP/'B9CorpusProbe.java').write_text(CORPUS_PROBE,encoding='utf8')
    (TEMP/'B8Codec.java').write_text(b8.probe_sources()['B8Codec'],encoding='utf8')
    jdk=pathlib.Path(os.environ.get('JAVA_HOME','C:/Program Files/Eclipse Adoptium/jdk-25.0.4.7-hotspot'))/'bin'
    annotations=next((ROOT/'.gradle-user-home/caches/modules-2/files-2.1/org.jetbrains/annotations').rglob('annotations-26.0.2.jar'))
    cp=str(TEMP)+';'+';'.join(str(ROOT/p) for p in b8.CLASSPATH_ENTRIES)+';'+str(annotations)
    sources=[str(ROOT/p) for p in DIAGNOSTIC_SOURCES]
    command('b9_javac',[str(jdk/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(TEMP),str(TEMP/'B9Probe.java'),str(TEMP/'B9CorpusProbe.java'),str(TEMP/'B8Codec.java'),*sources])
    command('b9_production_probe',[str(jdk/'java.exe'),'-Xmx3g','-cp',cp,'B9Probe',str(ROOT)])
    inputs=[dict(s,source=str(ROOT/s['source']),nativePolicy=s['enum'] in ['BACAP','BACAP_HARDCORE']) for s in b8.SOURCE_INPUTS]
    inputs.append({'enum':'HISTORICAL_MAIN','source':str(ROOT/'reference/phase_a_preservation/files/final/bacap.zip'),'nativePolicy':False})
    write(TEMP/'corpus_inputs.json',inputs)
    command('b9_corpus_probe',[str(jdk/'java.exe'),'-Xmx3g','-cp',cp,'com.diskree.achievetodo.client.B9CorpusProbe',str(TEMP/'corpus_inputs.json'),str(TEMP/'corpus_results.json')])
    # Hash-bound B8 source-backed registry context, fresh real converted helper payload.
    resources=ROOT/'build/tmp/phase_b_b8/post_r2/codec_resources_terralith.json'
    require(sha(resources.read_bytes())==REGISTRY_RESOURCES_SHA, 'B8 registry context drift')
    write(TEMP/'codec_resources_helpers.json',load(resources))
    with zipfile.ZipFile(TEMP/'probe/converted/BACAP_TERRALITH.zip') as z:
        definitions={f'blazeandcave:technical/{i}':json.loads(z.read(f'data/blazeandcave/advancement/technical/{i}.json')) for i in ['biome_branch1_end','biome_branch2_end']}
    write(TEMP/'codec_advancements_helpers.json',definitions)
    command('b9_helper_codec',[str(jdk/'java.exe'),'-Xmx3g','-cp',cp,'B8Codec',str(TEMP),'helpers'])
    return load(TEMP/'probe/identity_probe.json'), load(TEMP/'corpus_results.json'),load(TEMP/'codec_helpers_results.json')

def modern_scan(corpus, probe):
    families={r['family'] for r in load(ROOT/'reference/phase_b/b4_converter_decision.json')['families']}
    families.add('json_legacy_player_advancements')
    result={}
    for row in corpus:
        name=row['spec']['enum']
        if name not in ['BACAP','BACAP_HARDCORE','TERRALITH','AMPLIFIED_NETHER','NULLSCAPE']:
            continue
        require(row['secondPassDrift']==0 and row['actualChangedFunctions']==0 and row['runtimeChatChanged']==0, name+' modern function drift')
        if name in ['BACAP','BACAP_HARDCORE']:
            witness=probe['nativeMainWitness' if name=='BACAP' else 'nativeHardcoreWitness']
            require(row['actualChangedJson']==witness['intentionalAnnouncementSuppressions'], name+' native scope')
        else:
            expected=0 if name!='NULLSCAPE' else 1
            require(row['actualChangedJson']==expected, name+' unexpected bytes')
            # Current worldgen output is compared to accepted r19/B8 output; Nullscape's
            # single JSON is minification only, with zero field/value changes.
            with zipfile.ZipFile(row['spec']['source']) as z:
                for path in row['changedPaths']:
                    original=json.loads(z.read(path))
                    require(original==json.loads(payload(ROOT/'build/tmp/phase_b_b8/post_r2/converted'/f'{name}.zip')[path]),name+' semantic change')
        candidates={r['family']:r['candidateCount'] for r in row['candidateFamilies']}
        actual_announcements=row['actualChangedJson'] if name in ['BACAP','BACAP_HARDCORE'] else 0
        result[name]={'jsonChecked':row['jsonChecked'],'functionsChecked':row['functionsChecked'],
            'sourcePolicy':'native announcement-only' if row['spec']['nativePolicy'] else 'existing general historical seam',
            'unexpectedHistoricalSemanticChanges':0,'functionChanges':0,'runtimeChatChanges':0,
            'acceptedSerializationOnlyFiles':1 if name=='NULLSCAPE' else 0,
            'families':[{'family':f,'candidateCount':candidates.get(f,0),
                'actualChangedCount':actual_announcements if f=='json_vanilla_announcement_suppression' else 0,
                'modernNoopCount':candidates.get(f,0)-(actual_announcements if f=='json_vanilla_announcement_suppression' else 0)} for f in sorted(families)]}
    require(len(result)==5,'All modern corpora')
    return result

def run():
    state=precheck()
    probe,corpus,codec=java_probes()
    require(probe['passed'] and codec['checked']==2 and codec['passed']==2 and codec['failed']==0 and codec['negativeRejected'],'Compiled probes/real codec')
    require(all(p['passed'] for p in probe['secondPassCorpus'].values()),'Idempotence')
    scan=modern_scan(corpus,probe)
    comparisons={}
    for name in ['BACAP_TERRALITH','BACAP_AMPLIFIED_NETHER','BACAP_NULLSCAPE']:
        comparisons[name]=compare_payload(ROOT/'build/tmp/phase_b_b8_r1/converted'/f'{name}.zip',TEMP/'probe/converted'/f'{name}.zip')
        require(not comparisons[name]['changed'],'Accepted companion payload drift '+name)
    legacy=[]
    for spec in b8.SOURCE_INPUTS:
        for path,data in payload(ROOT/'build/tmp/phase_b_b8/post_r2/converted'/f'{spec["enum"]}.zip').items():
            if not path.endswith('.json'):
                continue
            def visit(node):
                if isinstance(node,dict):
                    conditions=node.get('conditions',{})
                    if isinstance(conditions,dict):
                        for key in ['player','entity','direct_entity','source_entity','passenger','vehicle']:
                            child=conditions.get(key)
                            if isinstance(child,dict) and isinstance(child.get('player'),dict) and 'advancements' in child['player']:
                                legacy.append(spec['enum']+':'+path)
                    for value in node.values(): visit(value)
                elif isinstance(node,list):
                    for value in node: visit(value)
            visit(json.loads(data))
    require(not legacy,'Unresolved legacy player family')
    b7_result=load(ROOT/'build/tmp/phase_b_b7_m2/independent_extraction.json')
    require(b7_result['implementationSha256']==B7_SHA and b7_result['totalRequirements']==4301,'Current B7 successor receipt')
    require(b7_result['providerCounts']==b7.CURRENT_EXPECTED_PROVIDER_COUNTS,'B7 provider partition')
    require(b7_result['placeholderCertification']['checked']==4290 and not b7_result['placeholderCertification']['mismatches'],'B7 placeholders')
    for view,expected in b7.CURRENT_EXPECTED_VIEW_COUNTS.items():
        require(b7_result['effectiveCoverage'][view]=={'required':expected,'resolved':expected,'missing':[]},'B7 effective '+view)
    static=load(ROOT/'src/test/resources/phase_b_certification/bacap_1_21_static_receipt.json')
    codec_lock=load(ROOT/'src/test/resources/phase_b_certification/bacap_1_21_codec_receipt.json')
    require(static['wrapperScan']=={'checked':1256,'malformed':0} and static['rewardFunctionScan']=={'checked':1294,'missing':0},'B8 reward/wrapper lock')
    require(static['trackers']['total']==86 and static['gui']['explicitAfter']==27,'B8 trackers/gui')
    require(static['messages']['currentCorpus']==1300 and static['messages']['targetBindingFailures']==0,'B8 messages')
    require(all(v['failed']==0 and v['checked']==v['passed'] for v in codec_lock['views'].values()),'B8 codecs')
    authority={**b7.CURRENT_B5_SUPERSESSIONS, **b7.CURRENT_POST_B5_PATHS, 'tools/phase_b/b7_localization_certification.py':B7_SHA, 'tools/phase_b/b8_bacap_static_certification.py':B8_SHA}
    # Exact production hashes bind all receipt facts to the current byte state.
    after=precheck()
    require(state['runtimeHashes']==after['runtimeHashes'] and state['evidenceHashes']==after['evidenceHashes'],'Runtime/evidence mutation')
    receipt={'phase':'B','stage':'B9','marker':'compat_26_2_r19','converterSha256':CONVERTER_SHA,'rootOverrideSha1':ROOT_SHA,
        'toolSha256':sha(pathlib.Path(__file__).read_bytes()),'authorityHashes':authority,'runtimeHashes':{**static['productionFileHashes'], **authority},'runtimeSnapshotSha256':sha(json.dumps(state['runtimeHashes'],sort_keys=True).encode()),
        'sources':[{k:v for k,v in row['spec'].items() if k!='source'}|{'path':pathlib.Path(row['spec']['source']).relative_to(ROOT).as_posix(),'sha256':sha(pathlib.Path(row['spec']['source']).read_bytes()),'sha1':hashlib.sha1(pathlib.Path(row['spec']['source']).read_bytes()).hexdigest()} for row in corpus],
        'compiledProductionProbe':probe,'directHelperCodec':{'checked':2,'passed':2,'failed':0,'negativeControlRejected':True,'codec':'Advancement.CODEC','minecraftVersion':codec['minecraftVersion']},
        'companionAcceptedPayloadComparison':comparisons,'modernFalsePositiveScan':scan,
        'unresolvedLegacyPlayerPredicates':legacy,'historicalFamilyMatrix':{'schemaGuards':probe['familyWitness'],'scopedMigrations':probe['extraFamilyWitnesses'],
            'modernUnexpectedChanges':0,'secondPassDrift':0,'candidateCountersAreDiagnosticOnly':True},
        'B7Current':{'toolHash':B7_SHA,'requirements':4301,'providers':b7_result['providerCounts'],'placeholders':4290,'effectiveCoverage':b7_result['effectiveCoverage'],
            'exactHistoricalTransition':b7_result['currentConsumerSetReconciliation'],'dictionaryKeys':3482,
            'provenance':{k:v for k,v in b7_result['provenanceCertification'].items() if k!='changedValueRecords'}},
        'B7Historical':{'requirements':4304,'overlayProviders':3435,'placeholders':4293,'immutable':True},
        'B8Lock':{'receiptHashes':B8_RECEIPTS,'toolHash':B8_SHA,'target':1332,'canonical':1242,'wrappers':1256,'malformed':0,'rewardRefs':1294,'missingRewards':0,'trackers':86,'gui':27,'messages':1300,'effectiveCodecs':codec_lock['views']},
        'runtimeProductDelta':{'ADD':0,'MODIFY':0,'DELETE':0},'uniqueCumulativeRuntimeDiff':{'ADD':177,'MODIFY':69,'DELETE':9},'uniqueRuntimePaths':255}
    write(TEMP/'receipt_candidate.json',receipt)
    print(json.dumps({'compiledProbe':'PASS','realHelperCodec':'2/2','modernUnexpectedChanges':0,'runtimeMutation':False}))
    return receipt

class RegressionOrchestrationTests(unittest.TestCase):
    def zip_fixture(self, name, values):
        path=TEMP/'self_tests'/name
        path.parent.mkdir(parents=True,exist_ok=True)
        with zipfile.ZipFile(path,'w') as z:
            for key,value in values.items(): z.writestr(key,value)
        return path

    def xml_fixture(self, name=None, errors=0):
        path=TEMP/'self_tests/xml'
        path.mkdir(parents=True,exist_ok=True)
        root=ET.Element('testsuite',name='example',tests='1',failures='1' if name else '0',errors=str(errors),skipped='0')
        class_name,method=name.rsplit('.',1) if name else ('example','green()')
        case=ET.SubElement(root,'testcase',classname=class_name,name=method)
        if name: ET.SubElement(case,'failure',message='oracle fixture')
        ET.ElementTree(root).write(path/'TEST-example.xml',encoding='utf8')
        return path

    def test_equal_payload_ignores_marker(self):
        a=self.zip_fixture('left.zip',{'a':b'a',MARKER:b'old'})
        b=self.zip_fixture('right.zip',{'a':b'a',MARKER:b'new'})
        self.assertEqual({'checked':1,'changed':[]},compare_payload(a,b))

    def test_changed_missing_and_added_payloads_are_detected(self):
        a=self.zip_fixture('left.zip',{'a':b'a','removed':b'r'})
        b=self.zip_fixture('right.zip',{'a':b'b','added':b'n'})
        self.assertEqual(['a','added','removed'],compare_payload(a,b)['changed'])

    def test_new_failure_is_rejected(self):
        with self.assertRaises(AssertionError): suite_report(self.xml_fixture('example.unapproved()'))

    def test_suite_errors_are_rejected(self):
        with self.assertRaises(AssertionError): suite_report(self.xml_fixture(errors=1))

    def test_exact_baseline_failure_is_preserved(self):
        name=load(ROOT/'reference/phase_b/b7_post_r1_test_gate_resolution.json')['fullTestAfterR1']['acceptedBaselineNames'][0]
        self.assertEqual(1,suite_report(self.xml_fixture(name))['partition']['baseline'])

    def test_historical_failure_subset_is_allowed(self):
        name=load(ROOT/'reference/phase_b/b7_post_r1_test_gate_resolution.json')['records'][0]['failingTest']
        result=suite_report(self.xml_fixture(name))
        self.assertEqual(1,result['partition']['historical'])
        self.assertEqual(6,len(result['previousHistoricalNowPassing']))

    def test_green_historical_methods_do_not_need_forced_failures(self):
        result=suite_report(self.xml_fixture())
        self.assertEqual(0,result['failures'])
        self.assertEqual(7,len(result['previousHistoricalNowPassing']))

    def test_missing_source_fails_closed(self):
        with self.assertRaises(FileNotFoundError): payload(TEMP/'intentionally_absent_source.zip')

DIAGNOSTIC_SOURCES = {'build/tmp/phase_b_b5_2/src/com/diskree/achievetodo/client/B3ConverterProbe.java': '6c39b243f0d7d251d6bffa90fa93e9f52e8afc78dd9e4da3aaa6b332dd675bc9', 'build/tmp/phase_b_b5_2/src/com/diskree/achievetodo/client/InstrumentedChatText.java': 'e62cafd8b5caace884c3e8972239352854253b92290c6c9c0b9d1dd46319282c', 'build/tmp/phase_b_b5_2/src/com/diskree/achievetodo/client/InstrumentedCompatibility.java': '0675775262ad21a99bb22d5fd2057d630368182999fb3b85ba4d2d97d20fa6a3', 'build/tmp/phase_b_b5_2/src/com/diskree/achievetodo/client/InstrumentedItemText.java': 'a69e573c0c58e37e8126378d829753d6cf52ae5241dd93ffefa55cd8aded9464'}
REGISTRY_RESOURCES_SHA = '00caa904cf1844e26335d7b536c5bebb5f193902f43ea6959cca5843352cbd34'
JAVA_PROBE = 'import com.diskree.achievetodo.client.ExternalPack;\nimport com.diskree.achievetodo.client.ExternalPackCompatibility;\nimport com.google.gson.*;\nimport com.mojang.serialization.*;\nimport net.minecraft.core.*;\nimport net.minecraft.resources.*;\nimport java.nio.file.*;\nimport java.nio.charset.StandardCharsets;\nimport java.security.*;\nimport java.util.*;\nimport java.util.zip.*;\nimport java.io.*;\nimport java.util.stream.Stream;\n\npublic class B9Probe {\n static final String MARKER="achievetodo_compatibility/compat_26_2.properties";\n static final String TREASURE="data/blazeandcave/advancement/animal/treasure_hunter.json";\n static Path root,temp;\n static void require(boolean b,String why){if(!b)throw new IllegalStateException(why);}\n static String sha(Path p,String algorithm)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(Files.readAllBytes(p)));}\n static Map<String,byte[]> payload(Path p)throws Exception{\n  var result=new TreeMap<String,byte[]>();try(var z=new ZipFile(p.toFile())){for(var e:z.stream().toList()){try(var in=z.getInputStream(e)){result.put(e.getName(),in.readAllBytes());}}}return result;\n }\n static Properties marker(Path p)throws Exception{Properties result=new Properties();try(var z=new ZipFile(p.toFile())){var e=z.getEntry(MARKER);if(e!=null)try(var in=z.getInputStream(e)){result.load(in);}}return result;}\n static JsonObject copy(Path source,Path derived,ExternalPack pack)throws Exception{\n  String before=sha(source,"SHA-256"),actual=sha(source,"SHA-1");ExternalPackCompatibility.copyForWorld(source,derived,pack);\n  var props=marker(derived);boolean marked=!props.isEmpty();\n  require(before.equals(sha(source,"SHA-256")),"Source archive changed");\n  require(!marked||(actual.equals(props.getProperty("sourceSha1"))&&"compat_26_2_r19".equals(props.getProperty("version"))),"Marker does not record actual source");\n  if(!marked)require(actual.equals(sha(derived,"SHA-1")),"Passthrough identity changed");\n  JsonObject row=new JsonObject();row.addProperty("enum",pack.name());row.addProperty("sourceSha1",actual);row.addProperty("sourceSha256",before);row.addProperty("currentEnumSha1",pack.getSha1());\n  row.addProperty("rawPinned",ExternalPackCompatibility.isPinnedHistoricalSource(source,pack));row.addProperty("rawCurrent",ExternalPackCompatibility.isCurrentWorldPack(source,pack));\n  row.addProperty("markerWritten",marked);row.addProperty("markerVersion",props.getProperty("version"));row.addProperty("markerSourceSha1",props.getProperty("sourceSha1"));\n  row.addProperty("markerRecordsActualSource",!marked||actual.equals(props.getProperty("sourceSha1")));\n  row.addProperty("derivedCompatible",ExternalPackCompatibility.isCompatibleWorldCopy(derived,pack));row.addProperty("derivedCurrent",ExternalPackCompatibility.isCurrentWorldPack(derived,pack));row.addProperty("sourceBytesUnchanged",true);return row;\n }\n static JsonObject compare(Path old,Path modern)throws Exception{\n  var a=payload(old);var b=payload(modern);a.remove(MARKER);b.remove(MARKER);\n  var keys=new TreeSet<>(a.keySet());keys.addAll(b.keySet());int differences=0;\n  for(String k:keys)if(!Arrays.equals(a.get(k),b.get(k)))differences++;\n  require(differences==0,"Non-marker payload regression: "+differences);\n  JsonObject r=new JsonObject();r.addProperty("nonMarkerEntriesCompared",keys.size());r.addProperty("differences",differences);r.addProperty("passed",true);return r;\n }\n static int suppress(JsonElement e){int n=0;if(e.isJsonObject()){var o=e.getAsJsonObject();if(o.has("display")&&o.get("display").isJsonObject()&&o.has("rewards")&&o.get("rewards").isJsonObject()){\n  var r=o.getAsJsonObject("rewards");if(r.has("function")&&r.get("function").isJsonPrimitive()&&r.get("function").getAsString().startsWith("bacap_rewards:")){\n   var d=o.getAsJsonObject("display");var a=d.get("announce_to_chat");if(a==null||!a.isJsonPrimitive()||!a.getAsJsonPrimitive().isBoolean()||a.getAsBoolean()){d.addProperty("announce_to_chat",false);n++;}}}\n  for(var entry:o.entrySet())n+=suppress(entry.getValue());\n }else if(e.isJsonArray())for(var v:e.getAsJsonArray())n+=suppress(v);return n;}\n static JsonObject nativeWitness(Path raw,Path converted,Path r17)throws Exception{\n  var a=payload(raw);var b=payload(converted);int functions=0,changedFunctions=0,json=0,announcements=0,changed=0,semantic=0;\n  var gate=ExternalPackCompatibility.class.getDeclaredMethod("isNativeBacapSource",String.class);gate.setAccessible(true);\n  require((boolean)gate.invoke(null,sha(raw,"SHA-1")),"Native source not classified by actual SHA");\n  require(b.size()==a.size()+1,"Native resource set changed");\n  for(var entry:a.entrySet()){\n   String p=entry.getKey();byte[] input=entry.getValue(),output=b.get(p);require(output!=null,"Native missing resource "+p);boolean differs=!Arrays.equals(input,output);if(differs)changed++;\n   if(p.endsWith(".json")){\n    json++;var before=JsonParser.parseString(new String(input,StandardCharsets.UTF_8));var after=JsonParser.parseString(new String(output,StandardCharsets.UTF_8));var expected=before.deepCopy();int n=suppress(expected);announcements+=n;\n    require(expected.equals(after),"Legacy native JSON mutation "+p);require(differs==(n>0),"Needless native serialization "+p);if(!before.equals(after))semantic++;\n   }else {if(p.endsWith(".mcfunction")){functions++;if(differs)changedFunctions++;}require(!differs,"Unexpected native mutation "+p);}\n  }\n  require(changedFunctions==0,"Native legacy function changed");\n  JsonObject r=new JsonObject();r.addProperty("actualShaSourcePolicy",true);r.addProperty("jsonChecked",json);r.addProperty("functionsChecked",functions);r.addProperty("historicalRuleFunctionChanges",changedFunctions);r.addProperty("legacyMigrationSemanticChanges",0);r.addProperty("changedFiles",changed);r.addProperty("semanticJsonChanges",semantic);r.addProperty("intentionalAnnouncementSuppressions",announcements);r.addProperty("parseFailures",0);r.addProperty("exceptions",0);r.add("r18NonMarkerComparison",compare(r17,converted));r.addProperty("passed",true);return r;\n }\n static Path mutateMarker(Path original,String filename,String key,String value)throws Exception{\n  var contents=payload(original);var p=marker(original);p.setProperty(key,value);var bytes=new ByteArrayOutputStream();p.store(bytes,null);contents.put(MARKER,bytes.toByteArray());Path out=temp.resolve(filename);\n  try(var z=new ZipOutputStream(Files.newOutputStream(out))){for(var e:contents.entrySet()){z.putNextEntry(new ZipEntry(e.getKey()));z.write(e.getValue());z.closeEntry();}}return out;\n }\n static JsonObject freshness(Path historical,Path current,Path badR17)throws Exception{\n  JsonObject result=new JsonObject();Path globals=temp.resolve("sync/global");Files.createDirectories(globals);Files.copy(root.resolve("build/tmp/phase_b_b1/BlazeandCave\'s Advancements Pack 1.21.zip"),globals.resolve("bacap.zip"),StandardCopyOption.REPLACE_EXISTING);\n  for(var fixture:Map.of("current",current,"historicalDerived",historical,"malformedR17",badR17,"oldR18",root.resolve("build/tmp/phase_b_b8_r1/r18/BACAP.zip"),"preR2Root",root.resolve("build/tmp/phase_b_b8_r2/current-pre-r2.zip")).entrySet()){\n   Path world=temp.resolve("sync/"+fixture.getKey());Files.createDirectories(world);Files.copy(fixture.getValue(),world.resolve("bacap.zip"),StandardCopyOption.REPLACE_EXISTING);\n   String state=ExternalPackCompatibility.ensureWorldPacksUpToDate(globals,world,true).name();String expected=fixture.getKey().equals("current")?"ALREADY_CURRENT":"UPDATED";\n   require(state.equals(expected),"World sync "+fixture.getKey()+": "+state);require(ExternalPackCompatibility.isCurrentWorldPack(world.resolve("bacap.zip"),ExternalPack.BACAP),"Synced world stale");\n   require(ExternalPackCompatibility.ensureWorldPacksUpToDate(globals,world,true).name().equals("ALREADY_CURRENT"),"Synced world not already current");\n   JsonObject row=new JsonObject();row.addProperty("firstResult",state);row.addProperty("thenAlreadyCurrent",true);row.addProperty("currentAfterSync",true);result.add(fixture.getKey(),row);\n  }return result;\n }\n static JsonElement treasureValue(Path zip)throws Exception{return JsonParser.parseString(new String(payload(zip).get(TREASURE),StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("criteria").getAsJsonObject("fishing_rod").getAsJsonObject("conditions").getAsJsonObject("item").getAsJsonObject("predicates").getAsJsonArray("enchantments").get(0).getAsJsonObject().get("enchantments");}\n static JsonObject codec(Path raw,Path r18)throws Exception{\n  ResourceKey<Registry<String>> key=ResourceKey.createRegistryKey(Identifier.parse("b7r1:enchantment"));var registry=new MappedRegistry<String>(key,Lifecycle.stable());\n  var holder=registry.register(ResourceKey.create(key,Identifier.parse("minecraft:sharpness")),"sharpness",RegistrationInfo.BUILT_IN);\n  registry.bindTags(Map.of(net.minecraft.tags.TagKey.create(key,Identifier.parse("blazeandcave:fishing_rod")),List.of(holder)));registry.freeze();\n  var ops=RegistryOps.create(JsonOps.INSTANCE,HolderLookup.Provider.create(Stream.of(registry)));var codec=RegistryCodecs.homogeneousList(key);var result=new JsonObject();\n  for(var e:Map.of("raw",raw,"r16",root.resolve("build/tmp/phase_b_b5_2/main/r16.zip"),"r17",root.resolve("build/tmp/phase_b_b5_2/main/r17.zip"),"r19",r18).entrySet()){\n   boolean accepted=codec.parse(ops,treasureValue(e.getValue())).result().isPresent();require(accepted!=e.getKey().equals("r16"),"Treasure codec "+e.getKey());result.addProperty(e.getKey()+"Accepted",accepted);\n  }\n  require(treasureValue(raw).equals(treasureValue(r18)),"Treasure Hunter native predicate modified");result.addProperty("nativePredicatePreserved",true);result.addProperty("resourceId","blazeandcave:animal/treasure_hunter");result.addProperty("method","Real cached Minecraft 26.2 RegistryCodecs.homogeneousList holder-set codec; exact enchantment-tag value; same isolated typed-registry method as B5.2, not a full world boot.");result.addProperty("passed",true);return result;\n }\n\n static String convert(String json)throws Exception {\n  var method=ExternalPackCompatibility.class.getDeclaredMethod("convertJson",String.class);method.setAccessible(true);\n  Object result=method.invoke(null,json);var text=result.getClass().getDeclaredMethod("text");text.setAccessible(true);\n  return (String)text.invoke(result);\n }\n static JsonObject familyWitness()throws Exception {\n  String payload="{\\"advancements\\":{\\"example:complete\\":true,\\"example:incomplete\\":false}}";\n  String predicate="{\\"player\\":"+payload+"}";\n  String input="{\\"criteria\\":{\\"a\\":{\\"trigger\\":\\"minecraft:location\\",\\"conditions\\":{\\"player\\":"+predicate+"}}}}";\n  require(transform("convertNativeJson",input).equals(input),"Legacy player migration leaked into native policy");\n  var after=JsonParser.parseString(convert(input)).getAsJsonObject();\n  var result=after.getAsJsonObject("criteria").getAsJsonObject("a").getAsJsonObject("conditions").getAsJsonObject("player");\n  require(result.keySet().equals(Set.of("type_specific/player")),"Wrong player key");\n  require(result.get("type_specific/player").equals(JsonParser.parseString(payload)),"Payload changed");\n  require(convert(convert(input)).equals(convert(input)),"Synthetic idempotence");\n  var negatives=List.of(predicate,\n   "{\\"data\\":{\\"player\\":"+predicate+"}}",\n   "{\\"conditions\\":{\\"item\\":"+predicate+"}}",\n   "{\\"conditions\\":{\\"player\\":{\\"player\\":{\\"advancements\\":{\\"example:complete\\":\\"true\\"}}}}}",\n   "{\\"conditions\\":{\\"player\\":{\\"player\\":{\\"advancements\\":{\\"INVALID ID\\":true}}}}}",\n   "{\\"conditions\\":{\\"player\\":{\\"player\\":{\\"advancements\\":{} }}}}",\n   "{\\"conditions\\":{\\"player\\":{\\"player\\":{\\"advancements\\":{\\"example:complete\\":true},\\"unrelated\\":1}}}}",\n   "{\\"conditions\\":{\\"player\\":{\\"type_specific/player\\":"+payload+",\\"player\\":"+payload+"}}}");\n  for(String n:negatives)require(convert(n).equals(n),"Overbroad player migration: "+n);\n  String scalar="{\\"criteria\\":{\\"a\\":{\\"trigger\\":\\"minecraft:inventory_changed\\",\\"conditions\\":{\\"items\\":[{\\"items\\":\\"minecraft:diamond_sword\\",\\"predicates\\":{\\"enchantments\\":[{\\"enchantments\\":\\"minecraft:sharpness\\",\\"levels\\":{\\"min\\":1}}]}}]}}}}";\n  require(transform("convertNativeJson",scalar).equals(scalar),"Enchantment migration leaked into native policy");\n  var singleton=JsonParser.parseString(convert(scalar)).getAsJsonObject().getAsJsonObject("criteria").getAsJsonObject("a").getAsJsonObject("conditions").getAsJsonArray("items").get(0).getAsJsonObject().getAsJsonObject("predicates").getAsJsonArray("enchantments").get(0).getAsJsonObject().getAsJsonArray("enchantments");\n  require(singleton.size()==1&&singleton.get(0).getAsString().equals("minecraft:sharpness"),"Historical enchantment witness");\n  JsonObject r=new JsonObject();r.addProperty("syntheticDifferentIdsMigrate",true);r.addProperty("booleanPayloadPreserved",true);r.addProperty("negativeShapesUnchanged",negatives.size());r.addProperty("historicalScalarSingletonWitness",true);r.addProperty("passed",true);return r;\n }\n static JsonObject helperIdempotence(Path converted)throws Exception {\n  var entries=payload(converted);int checked=0;JsonArray rows=new JsonArray();\n  Path archive=temp.resolve("helper-first-pass-payload.zip"),output=temp.resolve("helper-second-pass.zip");\n  try(var zip=new ZipOutputStream(Files.newOutputStream(archive))){\n   for(String id:List.of("biome_branch1_end","biome_branch2_end")) {\n    String p="data/blazeandcave/advancement/technical/"+id+".json";byte[] bytes=entries.get(p);String first=new String(bytes,StandardCharsets.UTF_8),second=convert(first);\n    require(first.equals(second),"Helper byte idempotence "+id);require(JsonParser.parseString(first).equals(JsonParser.parseString(second)),"Helper semantic idempotence");\n    require(!first.contains("type_specific/type_specific/player"),"Repeated nesting");\n    var a=JsonParser.parseString(first).getAsJsonObject();require(a.keySet().equals(Set.of("parent","criteria")),"Helper top-level contract");\n    String parent=id.equals("biome_branch1_end")?"minecraft:adventure/adventuring_time":"blazeandcave:biomes/terralithic";require(a.get("parent").getAsString().equals(parent),"Helper parent");\n    var c=a.getAsJsonObject("criteria");require(c.keySet().equals(Set.of("kilometre_walk")),"Helper criterion count");\n    var criterion=c.getAsJsonObject("kilometre_walk");require(criterion.get("trigger").getAsString().equals("minecraft:location"),"Helper trigger");\n    var player=criterion.getAsJsonObject("conditions").getAsJsonObject("player");require(player.keySet().equals(Set.of("type_specific/player")),"Helper predicate shape");\n    var adv=player.getAsJsonObject("type_specific/player").getAsJsonObject("advancements");require(adv.keySet().equals(Set.of("blazeandcave:biomes/kilometre_walk"))&&adv.get("blazeandcave:biomes/kilometre_walk").getAsBoolean(),"Helper advancements");\n    zip.putNextEntry(new ZipEntry(p));zip.write(bytes);zip.closeEntry();checked++;\n    JsonObject row=new JsonObject();row.addProperty("path",p);row.addProperty("secondPassByteChanges",0);row.addProperty("secondPassSemanticChanges",0);row.addProperty("contractPreserved",true);rows.add(row);\n   }\n  }\n  ExternalPackCompatibility.copyForWorld(archive,output,ExternalPack.BACAP_TERRALITH);\n  var second=payload(output);require(marker(output).isEmpty(),"Already modern helper-only payload required conversion");\n  for(var row:rows){String path=row.getAsJsonObject().get("path").getAsString();require(Arrays.equals(entries.get(path),second.get(path)),"Production helper archive idempotence");}\n  JsonObject r=new JsonObject();r.addProperty("checked",checked);r.addProperty("secondPassSemanticChanges",0);r.addProperty("secondPassByteChanges",0);r.addProperty("productionCopyForWorldHelperOnlyRoundTrip",true);r.addProperty("scope","The two repaired r19 entries, also converted individually by compiled production convertJson; not a claim about unrelated already-derived pack reprocessing.");r.add("records",rows);r.addProperty("passed",true);return r;\n }\n static JsonObject terralithSync(Path currentMain,Path oldCompanion,Path repairedCompanion)throws Exception {\n  Path global=temp.resolve("terralith-sync/global"),world=temp.resolve("terralith-sync/world");Files.createDirectories(global);Files.createDirectories(world);\n  Files.copy(root.resolve("build/tmp/phase_b_b1/BlazeandCave\'s Advancements Pack 1.21.zip"),global.resolve("bacap.zip"),StandardCopyOption.REPLACE_EXISTING);\n  Files.copy(root.resolve("reference/phase_a_preservation/files/final/bacap_terralith.zip"),global.resolve("bacap_terralith.zip"),StandardCopyOption.REPLACE_EXISTING);\n  Files.copy(currentMain,world.resolve("bacap.zip"),StandardCopyOption.REPLACE_EXISTING);Files.copy(oldCompanion,world.resolve("bacap_terralith.zip"),StandardCopyOption.REPLACE_EXISTING);\n  require(!ExternalPackCompatibility.isCurrentWorldPack(world.resolve("bacap_terralith.zip"),ExternalPack.BACAP_TERRALITH),"Old r18 Terralith accepted");\n  String result=ExternalPackCompatibility.ensureWorldPacksUpToDate(global,world,true).name();require(result.equals("UPDATED"),"Terralith sync "+result);\n  Path output=world.resolve("bacap_terralith.zip");require(ExternalPackCompatibility.isCompatibleWorldCopy(output,ExternalPack.BACAP_TERRALITH),"Synced companion freshness");\n  require(ExternalPackCompatibility.ensureWorldPacksUpToDate(global,world,true).name().equals("ALREADY_CURRENT"),"Repeated Terralith sync");\n  var a=payload(repairedCompanion);var b=payload(output);JsonObject hashes=new JsonObject();for(String id:List.of("biome_branch1_end","biome_branch2_end")){\n   String p="data/blazeandcave/advancement/technical/"+id+".json";require(Arrays.equals(a.get(p),b.get(p)),"Synced helper changed");hashes.addProperty(p,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b.get(p))));\n  }\n  JsonObject r=new JsonObject();r.addProperty("oldR18Rejected",true);r.addProperty("result",result);r.addProperty("thenAlreadyCurrent",true);r.addProperty("markerVersion",marker(output).getProperty("version"));r.addProperty("sourceSha1",marker(output).getProperty("sourceSha1"));r.add("helperHashes",hashes);r.addProperty("passed",true);return r;\n }\n\n static String transform(String method,String input)throws Exception {\n  var m=ExternalPackCompatibility.class.getDeclaredMethod(method,String.class);m.setAccessible(true);\n  Object r=m.invoke(null,input);var getter=r.getClass().getDeclaredMethod("text");getter.setAccessible(true);return (String)getter.invoke(r);\n }\n static JsonObject secondPass(Path zip,boolean nativeSource)throws Exception {\n  int checked=0,byteChanges=0,semanticChanges=0;JsonArray diffs=new JsonArray();\n  for(var entry:payload(zip).entrySet()) {\n   String p=entry.getKey();if(p.equals(MARKER))continue;\n   String input=new String(entry.getValue(),StandardCharsets.UTF_8),output=input;\n   if(p.endsWith(".json"))output=transform(nativeSource?"convertNativeJson":"convertJson",input);\n   else if(p.endsWith(".mcfunction")&&!nativeSource)output=transform("convertFunction",input);\n   checked++;if(!input.equals(output)){byteChanges++;diffs.add(p);if(!p.endsWith(".json")||!JsonParser.parseString(input).equals(JsonParser.parseString(output)))semanticChanges++;}\n  }\n  JsonObject r=new JsonObject();r.addProperty("entriesChecked",checked);r.addProperty("secondPassByteChanges",byteChanges);r.addProperty("secondPassSemanticChanges",semanticChanges);r.add("changedPaths",diffs);\n  r.addProperty("method","Compiled production selected-policy entry transforms; derived archive SHA is not reused as a native download identity.");r.addProperty("passed",byteChanges==0&&semanticChanges==0);return r;\n }\n static JsonObject scopedWitness(String label,String method,String input,String expectedToken)throws Exception {\n  String first=transform(method,input),second=transform(method,first);require(!input.equals(first),label+" historical migration did not occur");require(first.contains(expectedToken),label+" expected token missing");require(first.equals(second),label+" second pass changed");\n  if(method.equals("convertJson"))require(transform("convertNativeJson",input).equals(input),label+" leaked into native seam");\n  JsonObject r=new JsonObject();r.addProperty("historicalMigrationOccurs",true);r.addProperty("modernHistoricalMigrationChanges",0);r.addProperty("secondPassChanges",0);r.addProperty("inputSha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8))));r.addProperty("passed",true);return r;\n }\n static JsonObject extraFamilies()throws Exception {\n  JsonObject r=new JsonObject();\n  r.add("daytime",scopedWitness("daytime","convertFunction","time query daytime\\n","time of minecraft:overworld query minecraft:day"));\n  r.add("gamerule",scopedWitness("gamerule","convertFunction","gamerule commandBlockOutput false\\n","# achievetodo compatibility"));\n  r.add("itemComponents",scopedWitness("itemComponents","convertFunction","give @s minecraft:chain[dyed_color={rgb:7,show_in_tooltip:false},enchantments={levels:{\\"minecraft:sharpness\\":1},show_in_tooltip:false}]\\n","minecraft:iron_chain[dyed_color=7,enchantments={\\"minecraft:sharpness\\":1}]"));\n  r.add("llamaNbt",scopedWitness("llamaNbt","convertJson","{\\"conditions\\":{\\"entity\\":{\\"type\\":\\"#blazeandcave:llamas\\",\\"nbt\\":\\"{body_armor_item:{id:\\\\\\"minecraft:red_carpet\\\\\\"}}\\"}}}","equipment"));\n  return r;\n }\n public static void main(String[] args)throws Exception {\n  root=Path.of(args[0]);temp=root.resolve("build/tmp/phase_b_b9/resumed/probe");\n  try{ run(); } catch(Throwable t){var sw=new StringWriter();t.printStackTrace(new PrintWriter(sw));Files.writeString(temp.resolve("identity_exception.txt"),sw.toString());throw t;}\n }\n static void run()throws Exception {\n  String location=ExternalPackCompatibility.class.getProtectionDomain().getCodeSource().getLocation().toString();require(location.endsWith("/build/classes/java/main/"),"Not compiled production");\n  JsonObject report=new JsonObject();report.addProperty("productionClassLocation",location);\n  Path hist=root.resolve("reference/phase_a_preservation/files/final/bacap.zip"),main=root.resolve("build/tmp/phase_b_b1/BlazeandCave\'s Advancements Pack 1.21.zip"),hard=root.resolve("build/tmp/phase_b_b4/BlazeandCave\'s Advancements Pack Hardcore.zip");\n  Path histOut=temp.resolve("converted/HISTORICAL_MAIN.zip"),mainOut=temp.resolve("converted/BACAP.zip"),hardOut=temp.resolve("converted/BACAP_HARDCORE.zip");\n  var h=copy(hist,histOut,ExternalPack.BACAP);require(!h.get("rawCurrent").getAsBoolean()&&!h.get("derivedCurrent").getAsBoolean()&&!h.get("derivedCompatible").getAsBoolean(),"Historical source accepted as current");\n  var c=copy(main,mainOut,ExternalPack.BACAP);require(c.get("rawCurrent").getAsBoolean()&&c.get("derivedCurrent").getAsBoolean()&&c.get("derivedCompatible").getAsBoolean(),"Current source rejected");\n  var hardIdentity=copy(hard,hardOut,ExternalPack.BACAP_HARDCORE);require(hardIdentity.get("derivedCurrent").getAsBoolean(),"Hardcore rejected");\n  Path badR17=root.resolve("build/tmp/phase_b_b7/resume/historical_actual_production_r17.zip"),oldR18=root.resolve("build/tmp/phase_b_b8_r1/r18/BACAP.zip");\n  require(!ExternalPackCompatibility.isCurrentWorldPack(badR17,ExternalPack.BACAP)&&!ExternalPackCompatibility.isCompatibleWorldCopy(badR17,ExternalPack.BACAP),"Old malformed r17 accepted");\n  require(!ExternalPackCompatibility.isCurrentWorldPack(oldR18,ExternalPack.BACAP)&&!ExternalPackCompatibility.isCompatibleWorldCopy(oldR18,ExternalPack.BACAP),"Old r18 accepted");\n  JsonObject identity=new JsonObject();identity.add("historical",h);identity.add("current",c);identity.addProperty("malformedR17Rejected",true);identity.addProperty("oldR18Rejected",true);report.add("identityMatrix",identity);\n  report.add("historicalMainPayloadComparison",compare(root.resolve("build/tmp/phase_b_b8_r1/r18/HISTORICAL_MAIN.zip"),histOut));\n  report.add("nativeMainWitness",nativeWitness(main,mainOut,root.resolve("build/tmp/phase_b_b8_r1/r18/BACAP.zip")));\n  report.add("nativeHardcoreWitness",nativeWitness(hard,hardOut,root.resolve("build/tmp/phase_b_b8_r1/r18/BACAP_HARDCORE.zip")));\n  JsonArray companions=new JsonArray();for(ExternalPack pack:List.of(ExternalPack.BACAP_TERRALITH,ExternalPack.BACAP_AMPLIFIED_NETHER,ExternalPack.BACAP_NULLSCAPE)) {\n   Path source=root.resolve("reference/phase_a_preservation/files/final/"+pack.getFileName()),out=temp.resolve("converted/"+pack.name()+".zip"),old=root.resolve("build/tmp/phase_b_b8_r1/r18/"+pack.name()+".zip");\n   var row=copy(source,out,pack);require(row.get("rawCurrent").getAsBoolean()&&row.get("derivedCurrent").getAsBoolean()&&row.get("derivedCompatible").getAsBoolean(),"Companion pin broken");\n   require(!ExternalPackCompatibility.isCurrentWorldPack(old,pack)&&!ExternalPackCompatibility.isCompatibleWorldCopy(old,pack),"Old companion revision accepted");row.addProperty("oldR18Rejected",true); if(pack!=ExternalPack.BACAP_TERRALITH)row.add("acceptedPayloadComparison",compare(root.resolve("build/tmp/phase_b_b8_r1/converted/"+pack.name()+".zip"),out));\n   Path global=temp.resolve("companions-sync/"+pack.name()+"/global"),world=temp.resolve("companions-sync/"+pack.name()+"/world");Files.createDirectories(global);Files.createDirectories(world);Files.copy(source,global.resolve(pack.getFileName()),StandardCopyOption.REPLACE_EXISTING);Files.copy(old,world.resolve(pack.getFileName()),StandardCopyOption.REPLACE_EXISTING);\n   String state=ExternalPackCompatibility.ensureWorldPacksUpToDate(global,world,false).name();require(state.equals("UPDATED"),"Companion not regenerated");require(ExternalPackCompatibility.isCurrentWorldPack(world.resolve(pack.getFileName()),pack),"Regenerated companion stale");row.addProperty("oldR18WorldSync",state);companions.add(row);\n  }report.add("companions",companions);\n  JsonObject integrity=new JsonObject();for(var e:Map.of("version","compat_26_2_r18","fileName","wrong.zip","sourceSha1","0000000000000000000000000000000000000000","rootOverrideSha1","0000000000000000000000000000000000000000").entrySet()) {\n   Path broken=mutateMarker(mainOut,"wrong-"+e.getKey()+".zip",e.getKey(),e.getValue());require(!ExternalPackCompatibility.isCurrentWorldPack(broken,ExternalPack.BACAP)&&!ExternalPackCompatibility.isCompatibleWorldCopy(broken,ExternalPack.BACAP),"Wrong marker accepted");integrity.addProperty(e.getKey()+"Rejected",true);\n  }report.add("markerIntegrity",integrity);report.add("mainWorldSync",freshness(histOut,mainOut,badR17));\n  report.add("terralithWorldSync",terralithSync(mainOut,root.resolve("build/tmp/phase_b_b8_r1/r18/BACAP_TERRALITH.zip"),temp.resolve("converted/BACAP_TERRALITH.zip")));\n  report.add("familyWitness",familyWitness());report.add("idempotence",helperIdempotence(temp.resolve("converted/BACAP_TERRALITH.zip")));\n  report.add("treasureHunterWitness",codec(main,mainOut));report.addProperty("passed",true);\n  Path preRoot=root.resolve("build/tmp/phase_b_b8_r2/current-pre-r2.zip"),postRoot=root.resolve("build/tmp/phase_b_b8_r2/current-post-r2.zip");\n  require(!ExternalPackCompatibility.isCompatibleWorldCopy(preRoot,ExternalPack.BACAP),"Pre-R2 digest copy accepted");\n  require(ExternalPackCompatibility.isCompatibleWorldCopy(postRoot,ExternalPack.BACAP),"Post-R2 digest copy rejected");\n  require(marker(mainOut).getProperty("rootOverrideSha1").equals("cbc432be35d5525001872c430877541cfa1fcad6"),"Current root digest mismatch");\n  report.addProperty("preR2DigestCopyRejected",true);report.addProperty("postR2DigestCopyAccepted",true);report.addProperty("rootOverrideSha1",marker(mainOut).getProperty("rootOverrideSha1"));\n\n  JsonObject passes=new JsonObject();passes.add("nativeMain",secondPass(mainOut,true));passes.add("nativeHardcore",secondPass(hardOut,true));passes.add("historicalMain",secondPass(histOut,false));\n  for(var pack:List.of(ExternalPack.BACAP_TERRALITH,ExternalPack.BACAP_AMPLIFIED_NETHER,ExternalPack.BACAP_NULLSCAPE))passes.add(pack.name(),secondPass(temp.resolve("converted/"+pack.name()+".zip"),false));\n  for(var p:passes.entrySet())require(p.getValue().getAsJsonObject().get("passed").getAsBoolean(),"Second pass drift: "+p.getKey());\n  report.add("secondPassCorpus",passes);report.add("extraFamilyWitnesses",extraFamilies());\n  Files.writeString(temp.resolve("identity_probe.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\\n");System.out.println("PASS: compiled production identity, companion freshness/sync, native parity, Treasure Hunter, schema guards, helper idempotence");\n }\n}\n'
CORPUS_PROBE = 'package com.diskree.achievetodo.client;\nimport com.google.gson.*;\nimport java.nio.file.*;\nimport java.nio.charset.StandardCharsets;\nimport java.util.*;\nimport java.util.zip.*;\npublic class B9CorpusProbe {\n static String transform(String kind,String input)throws Exception {return B3ConverterProbe.converted(ExternalPackCompatibility.class,kind,input);}\n public static void main(String[] args)throws Exception {\n  JsonArray result=new JsonArray();\n  for(var value:JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonArray()) {\n   var spec=value.getAsJsonObject();boolean nativeSource=spec.get("nativePolicy").getAsBoolean();\n   B3ConverterProbe.candidates.clear();B3ConverterProbe.changes.clear();\n   int jsonCount=0,fnCount=0,changedJson=0,changedFn=0,second=0,chat=0;JsonArray diffs=new JsonArray();\n   try(var z=new ZipFile(spec.get("source").getAsString())) {\n    for(var e:z.stream().filter(e->!e.isDirectory()).toList()) {\n     String path=e.getName();boolean json=path.endsWith(".json"),fn=path.endsWith(".mcfunction");if(!json&&!fn)continue;\n     B3ConverterProbe.entry=path;String raw=new String(z.getInputStream(e).readAllBytes(),StandardCharsets.UTF_8);\n     String out;\n     if(json){jsonCount++;JsonParser.parseString(raw);B3ConverterProbe.converted(InstrumentedCompatibility.class,"convertJson",raw);out=transform(nativeSource?"convertNativeJson":"convertJson",raw);if(!raw.equals(out))changedJson++;}\n     else {fnCount++;B3ConverterProbe.functionFamilies(raw);out=nativeSource?raw:transform("convertFunction",raw);if(!raw.equals(out))changedFn++;}\n     String again=json?transform(nativeSource?"convertNativeJson":"convertJson",out):nativeSource?out:transform("convertFunction",out);if(!out.equals(again))second++;\n     if(!out.equals(raw))diffs.add(path);\n     if(fn&&(path.startsWith("data/bacap_rewards/function/msg/")||path.startsWith("data/bacap_rewards/function/")&&path.endsWith("/root.mcfunction")||raw.contains("advancementssearch highlight"))) {\n      for(String line:raw.split("\\\\R",-1)) if((path.startsWith("data/bacap_rewards/function/")||path.startsWith("data/blazeandcave/function/")&&line.contains("/advancementssearch highlight "))&&!LegacyChatText.migrateCommand(line).equals(line)){chat++;break;}\n     }\n    }\n   }\n   if(second!=0)throw new IllegalStateException("Second pass drift "+spec);\n   JsonObject row=new JsonObject();row.add("spec",spec);row.addProperty("jsonChecked",jsonCount);row.addProperty("functionsChecked",fnCount);row.addProperty("actualChangedJson",changedJson);row.addProperty("actualChangedFunctions",changedFn);row.addProperty("secondPassDrift",second);row.addProperty("runtimeChatChanged",chat);row.add("changedPaths",diffs);\n   JsonArray families=new JsonArray();for(var c:B3ConverterProbe.candidates.entrySet()){\n    JsonObject f=new JsonObject();f.addProperty("family",c.getKey());f.addProperty("candidateCount",c.getValue().size());f.addProperty("historicalDetectorChangedCount",B3ConverterProbe.changes.getOrDefault(c.getKey(),new TreeSet<>()).size());families.add(f);\n   }row.add("candidateFamilies",families);result.add(row);\n  }\n  Files.writeString(Path.of(args[1]),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\\n");\n }\n}\n'
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--self-test',action='store_true');parser.add_argument('--suite',type=pathlib.Path)
    args=parser.parse_args()
    if args.self_test: unittest.main(argv=['b9'],verbosity=2)
    elif args.suite: print(json.dumps(suite_report(args.suite),ensure_ascii=False,indent=2))
    else: run()
