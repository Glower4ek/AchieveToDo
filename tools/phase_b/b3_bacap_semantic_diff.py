#!/usr/bin/env python3
"""Read-only Phase B/B3 BACAP semantic analysis. Standard-library Python, local JDK.
No extraction into source trees, archive writes, downloads or product changes.
JSON objects sort keys; array order is preserved. Requirements also have a
separate logical AND-of-OR fingerprint, explicitly distinct from parsed equality.
Reports contain identities/hashes/structural facts, never upstream bodies.
"""
from __future__ import annotations
import argparse,collections,difflib,hashlib,json,os,re,subprocess,sys,types,zipfile
from pathlib import Path

OLD_HASH="8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70"
TARGET_HASH="c71d1aa1a84dbe00a3f85a42144b46214c4669a3cccf07ff66631d28f16a99b2"
EXCLUDED_ROOT="blazeandcave:bacap/root"
MISSING={"$b3_missing_field":True}
RESOURCE_TYPES={"function","predicate","tags","dialog"}
LOCAL_NS={"blazeandcave","bacap_rewards","bacap_fanpacks","minecraft"}
DISPOSITIONS={"NO_PRODUCT_CHANGE","PIN_UPDATE_REQUIRED","OVERRIDE_RESOURCE_UPDATE_REQUIRED","TRACKER_REVIEW_REQUIRED","TREE_GUI_REVIEW_REQUIRED","SEARCH_LINK_UPDATE_REQUIRED","SETUP_SCOREBOARD_REVIEW_REQUIRED","CONVERTER_REVIEW_REQUIRED","LOCALIZATION_REVIEW_REQUIRED","COMPANION_REVIEW_REQUIRED","TEST_SUCCESSOR_REQUIRED","DEFER_TO_PHASE_C_OR_D","UNKNOWN_REQUIRES_B4"}
def sha(b):return hashlib.sha256(b).hexdigest()
def canonical_json(v):return json.dumps(v,sort_keys=True,separators=(",",":"),ensure_ascii=False).encode()
def semhash(v):return sha(canonical_json(v))
def read_json(p):return json.loads(Path(p).read_text(encoding="utf-8-sig"))
def write_exclusive(p,v):
 with Path(p).open("x",encoding="utf-8",newline="\n") as f:json.dump(v,f,ensure_ascii=False,sort_keys=True,indent=None if Path(p).name=="b3_resource_graph_diff.json" else 2,separators=(",",":") if Path(p).name=="b3_resource_graph_diff.json" else None);f.write("\n")
def rid(s):return s if ":" in s else "minecraft:"+s
def json_parse(b,path,side,diagnostics):
 t=b.decode("utf-8-sig")
 try:return json.loads(t)
 except json.JSONDecodeError:
  allowed={"data/blazeandcave/predicate/third_line.json","data/blazeandcave/advancement/adventure/slenderman.json","data/blazeandcave/advancement/adventure/the_one_and_true_johnny.json"}
  if side=="old" and path in allowed and "\\'" in t:
   result=json.loads(t.replace("\\'","'"))
   diagnostics.append({"path":path,"side":side,"kind":"LEGACY_GSON_ACCEPTED_APOSTROPHE_ESCAPE","replacementCount":t.count("\\'"),"byteSha256":sha(b),"rule":"Repair only invalid backslash-apostrophe in the three pinned old entries for parsed comparison; bytes stay unchanged."})
   return result
  raise
def residentity(path):
 s=path.split("/")
 if len(s)<4 or s[0]!="data" or s[1] not in LOCAL_NS:return None
 typ=s[2]
 if typ not in RESOURCE_TYPES:return None
 sub="/".join(s[3:])
 if typ=="function" and not sub.endswith(".mcfunction"):return None
 if typ!="function" and not sub.endswith(".json"):return None
 value=sub.rsplit(".",1)[0];reg=None
 if typ=="tags":
  parts=value.split("/")
  depth=2 if parts[0]=="worldgen" else 1
  reg="/".join(parts[:depth]);value="/".join(parts[depth:])
 return {"namespace":s[1],"resourceType":typ,"registryType":reg,"resourceId":s[1]+":"+value}
def load_pack(path,side,expected_hash):
 p=Path(path);data=p.read_bytes()
 assert sha(data)==expected_hash,(side,"archive hash mismatch")
 pack={"path":p.as_posix(),"sha256":sha(data),"size":len(data),"adv":{},"resources":{},"diagnostics":[]}
 with zipfile.ZipFile(p) as z:
  seen=set()
  for entry in sorted(z.infolist(),key=lambda e:e.filename):
   n=entry.filename
   if entry.is_dir():continue
   assert n not in seen,("duplicate ZIP path",n);seen.add(n)
   b=z.read(entry)
   if n=="pack.mcmeta":
    meta=json.loads(b.decode("utf-8-sig"));pv=meta.get("pack",{})
    pack["packMetadata"]={"byteSha256":sha(b),"semanticSha256":semhash(meta),"topLevelFields":sorted(meta),"packFields":{k:v for k,v in pv.items() if k!="description"},"descriptionFingerprint":semhash(pv.get("description"))}
   m=re.fullmatch(r"data/([^/]+)/advancements?/(.+)\.json",n)
   if m:
    aid=m[1]+":"+m[2];assert aid not in pack["adv"]
    v=json_parse(b,n,side,pack["diagnostics"])
    pack["adv"][aid]={"path":n,"bytes":b,"json":v,"byteSha256":sha(b),"semanticSha256":semhash(v),"canonical":"display" in v and isinstance(v["display"],dict) and v["display"].get("hidden") is not True and aid!=EXCLUDED_ROOT}
   ident=residentity(n)
   if ident:
    row={**ident,"path":n,"byteSha256":sha(b),"size":len(b),"bytes":b}
    if ident["resourceType"]=="function":
     row["text"]=b.decode("utf-8-sig")
     row["normalizedSha256"]=sha("\n".join(line.rstrip() for line in row["text"].splitlines()).encode())
    else:
     row["json"]=json_parse(b,n,side,pack["diagnostics"]);row["normalizedSha256"]=semhash(row["json"])
    pack["resources"][n]=row
 return pack
def text_summary(v):
 if v is MISSING or v==MISSING:return {"present":False,"fingerprint":None,"translatableKeys":[],"literalTextFingerprints":[]}
 keys=[];literal=[]
 def visit(x):
  if isinstance(x,dict):
   if isinstance(x.get("translate"),str):keys.append(x["translate"])
   if isinstance(x.get("text"),str):literal.append(sha(x["text"].encode()))
   for k,t in x.items():
    if k not in ("text","translate"):visit(t)
  elif isinstance(x,list):
   for y in x:visit(y)
  elif isinstance(x,str):literal.append(sha(x.encode()))
 visit(v)
 return {"present":True,"fingerprint":semhash(v),"translatableKeys":sorted(set(keys)),"literalTextFingerprints":sorted(set(literal))}
def req_info(v):
 raw=v.get("requirements",[[k] for k in v.get("criteria",{})])
 # AND and OR are commutative/idempotent. Superset clauses are redundant.
 groups=sorted({tuple(sorted(set(g))) for g in raw})
 minimal=[list(g) for g in groups if not any(set(h)<set(g) for h in groups)]
 return {"declared": "requirements" in v,"orderedGroups":raw,"orderedSha256":semhash(raw),"logicalGroups":minimal,"logicalSha256":semhash(minimal)}
def json_refs(v,source,local_tag_map):
 refs=[]
 def walk(x,loc,parents):
  if isinstance(x,dict):
   if x.get("condition")=="minecraft:reference" and isinstance(x.get("name"),str):
    refs.append({"source":source,"location":loc+"/name","kind":"predicate","resourceId":rid(x["name"])})
   for k,y in sorted(x.items()):
    if k=="predicate" and isinstance(y,str) and re.fullmatch(r"(?:[a-z0-9_.-]+:)?[a-z0-9_./-]+",y):
     refs.append({"source":source,"location":loc+"/"+k,"kind":"predicate","resourceId":rid(y)})
    walk(y,loc+"/"+k,parents+[k])
  elif isinstance(x,list):
   for i,y in enumerate(x):walk(y,loc+"/"+str(i),parents)
  elif isinstance(x,str) and x.startswith("#"):
   token=x[1:]
   if re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_./-]+",token):
    candidates=local_tag_map.get(token,[])
    context="/".join(parents)
    hints=[("items","item"),("blocks","block"),("biomes","worldgen/biome"),("enchantments","enchantment"),("fluids","fluid"),("entity","entity_type"),("type","entity_type")]
    reg=next((reg for word,reg in hints if word in context),None)
    refs.append({"source":source,"location":loc,"kind":"registry_tag","resourceId":token,"registryType":reg,"localResourcePaths":sorted(candidates)})
 walk(v,"",[])
 return refs
def criterion_resource_ids(v):
 found=set()
 def visit(x):
  if isinstance(x,dict):
   for y in x.values():visit(y)
  elif isinstance(x,list):
   for y in x:visit(y)
  elif isinstance(x,str) and re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_./-]+",x):found.add(x)
 visit(v);return sorted(found)
def field_diff(a,b,tagmap):
 av=a["json"];bv=b["json"]
 change=lambda k:av.get(k,MISSING)!=bv.get(k,MISSING)
 flags={k:change(k) for k in ["parent","display","criteria","requirements","rewards","sends_telemetry_event"]}
 ad=av.get("display",{});bd=bv.get("display",{})
 flags["displayFields"]={k:ad.get(k,MISSING)!=bd.get(k,MISSING) for k in ["title","description","icon","frame","background","hidden","show_toast","announce_to_chat"]}
 flags["text"]={k:{"changed":flags["displayFields"][k],"old":text_summary(ad.get(k,MISSING)),"target":text_summary(bd.get(k,MISSING))} for k in ["title","description"]}
 ac=av.get("criteria",{});bc=bv.get("criteria",{});same=sorted(ac.keys()&bc.keys())
 flags["criteriaDetails"]={"namesAdded":sorted(bc.keys()-ac.keys()),"namesRemoved":sorted(ac.keys()-bc.keys()),"common":[{"name":k,"changed":ac[k]!=bc[k],"triggerChanged":ac[k].get("trigger",MISSING)!=bc[k].get("trigger",MISSING),"oldTrigger":ac[k].get("trigger"),"targetTrigger":bc[k].get("trigger"),"conditionsChanged":ac[k].get("conditions",MISSING)!=bc[k].get("conditions",MISSING),"oldConditionsSha256":semhash(ac[k].get("conditions",MISSING)),"targetConditionsSha256":semhash(bc[k].get("conditions",MISSING))} for k in same]}
 ar=json_refs(ac,a["path"],tagmap);br=json_refs(bc,b["path"],tagmap)
 refset=lambda rows:sorted({(r["kind"],r["resourceId"],r.get("registryType") or "") for r in rows})
 oldids=criterion_resource_ids(ac);newids=criterion_resource_ids(bc)
 flags["criteriaDetails"]["oldResourceIdentifiers"]=oldids
 flags["criteriaDetails"]["targetResourceIdentifiers"]=newids
 flags["criteriaDetails"]["resourceIdentifiersAdded"]=sorted(set(newids)-set(oldids))
 flags["criteriaDetails"]["resourceIdentifiersRemoved"]=sorted(set(oldids)-set(newids))
 flags["criteriaDetails"]["referencedResourcesChanged"]=refset(ar)!=refset(br) or oldids!=newids
 flags["criteriaDetails"]["oldReferences"]=[{"kind":k,"resourceId":i,"registryType":t or None} for k,i,t in refset(ar)]
 flags["criteriaDetails"]["targetReferences"]=[{"kind":k,"resourceId":i,"registryType":t or None} for k,i,t in refset(br)]
 ra=req_info(av);rb=req_info(bv)
 aset={tuple(g) for g in ra["logicalGroups"]};bset={tuple(g) for g in rb["logicalGroups"]}
 flags["requirementsDetails"]={"old":ra,"target":rb,"addedLogicalGroups":[list(g) for g in sorted(bset-aset)],"removedLogicalGroups":[list(g) for g in sorted(aset-bset)],"declaredGroupingOrOrderChanged":ra["orderedGroups"]!=rb["orderedGroups"],"completionGroupingSemanticsChanged":ra["logicalSha256"]!=rb["logicalSha256"],"note":"Grouping logic only; trigger/condition changes can alter completion independently."}
 arw=av.get("rewards",{});brw=bv.get("rewards",{})
 flags["rewardFields"]={k:arw.get(k,MISSING)!=brw.get(k,MISSING) for k in ["experience","function","recipes","loot"]}
 flags["rewardDetails"]={"oldFunction":arw.get("function"),"targetFunction":brw.get("function"),"oldSha256":semhash(arw),"targetSha256":semhash(brw),"otherFieldsChanged":sorted(k for k in (arw.keys()|brw.keys())-{"experience","function","recipes","loot"} if arw.get(k,MISSING)!=brw.get(k,MISSING))}
 known={"parent","display","criteria","requirements","rewards","sends_telemetry_event"}
 flags["otherTopLevelFields"]=sorted(k for k in (av.keys()|bv.keys())-known if change(k))
 flags["otherDisplayFields"]=sorted(k for k in (ad.keys()|bd.keys())-set(flags["displayFields"]) if ad.get(k,MISSING)!=bd.get(k,MISSING))
 return flags
def tree(pack):
 adv=pack["adv"];rows={};missing=[];cycles=[]
 for aid,a in sorted(adv.items()):
  chain=[];cur=aid;seen=set()
  while cur in adv:
   if cur in seen:
    cycles.append({"start":aid,"cycleIds":chain[chain.index(cur):]});break
   seen.add(cur);chain.append(cur);par=adv[cur]["json"].get("parent")
   if not par:cur=None;break
   if par not in adv:
    missing.append({"resourceId":cur,"missingParent":par,"namespace":par.split(":")[0]});chain.append(par);cur=par;break
   cur=par
  path=aid.split(":",1)[1]
  rows[aid]={"parent":a["json"].get("parent"),"root":chain[-1],"category":path.split("/")[0],"pathComponents":len(path.split("/")),"depth":len(chain)-1,"canonical":a["canonical"]}
 return {"nodes":rows,"missingParents":sorted({(r["resourceId"],r["missingParent"]):r for r in missing}.values(),key=lambda x:(x["resourceId"],x["missingParent"])),"cycles":cycles}
def sameclass(a,b):
 if a["byteSha256"]==b["byteSha256"]:return "BYTE_IDENTICAL"
 if a["semanticSha256"]==b["semanticSha256"]:return "SEMANTICALLY_IDENTICAL_BYTES_DIFFER"
 return "SEMANTICALLY_CHANGED"
def rename_analysis(old,new,official):
 ao=old["adv"];an=new["adv"];oldonly=sorted(ao.keys()-an.keys());newonly=sorted(an.keys()-ao.keys())
 candidates=[];byold=collections.defaultdict(list);bynew=collections.defaultdict(list)
 confirmed_pairs={
 ("blazeandcave:adventure/spear_fishing","blazeandcave:adventure/seafood_skewer"):"SPEAR_FISHING_RENAMED_SEAFOOD_SKEWER",
 ("blazeandcave:animal/birdkeeper","blazeandcave:biomes/birdkeeper"):"BIRDKEEPER_MOVED_ANIMALS_TO_BIOMES",
 ("blazeandcave:animal/chatterbox","blazeandcave:biomes/chatterbox"):"CHATTERBOX_MOVED_ANIMALS_TO_BIOMES",
 ("blazeandcave:statistics/two_by_two","blazeandcave:statistics/overpopulation"):"TWO_BY_TWO_OVERPOPULATION_NAME_SWAP",
 }
 def sig(v):
  disp=v.get("display",{})
  non={k:y for k,y in v.items() if k not in ["display","parent","rewards"]}
  crit=v.get("criteria",{}); criterionShapes=sorted(semhash(y) for y in crit.values())
  return {"title":semhash(disp.get("title")),"description":semhash(disp.get("description")),"icon":semhash(disp.get("icon")),"criteria":semhash(crit),"criteriaShape":semhash(criterionShapes),"requirements":req_info(v)["logicalSha256"],"nonDisplay":semhash(non),"nonFunctionRewards":semhash({k:y for k,y in v.get("rewards",{}).items() if k!="function"}),"parent":v.get("parent")}
 for x in oldonly:
  sx=sig(ao[x]["json"])
  for y in newonly:
   sy=sig(an[y]["json"]);e=[]
   for k in ["title","description","icon","criteria","criteriaShape","requirements","nonDisplay","nonFunctionRewards","parent"]:
    if sx[k]==sy[k]:e.append(k.upper()+"_IDENTITY")
   if x.split("/")[-1]==y.split("/")[-1]:e.append("BASENAME_IDENTITY")
   ratio=difflib.SequenceMatcher(None,canonical_json(ao[x]["json"].get("criteria",{})),canonical_json(an[y]["json"].get("criteria",{})),autojunk=False).ratio()
   if ratio>=0.85:e.append("NEAR_CRITERIA_STRUCTURE")
   exactcritical="CRITERIA_IDENTITY" in e or "CRITERIASHAPE_IDENTITY" in e
   title="TITLE_IDENTITY" in e;desc="DESCRIPTION_IDENTITY" in e;icon="ICON_IDENTITY" in e
   score=0.3*exactcritical+0.2*title+0.2*desc+0.08*icon+0.12*("BASENAME_IDENTITY" in e)+0.1*(ratio>=0.85)
   pair=(x,y);signal=confirmed_pairs.get(pair)
   proven=signal and signal in official.get("validatedSignals",[])
   # Do not infer from basename or generic criteria alone.
   if not proven and not ((title or desc) and (exactcritical or icon or ratio>=0.85) and score>=0.38):continue
   status="CONFIRMED_RENAME_OR_MOVE" if proven else ("HIGH_CONFIDENCE_CANDIDATE" if score>=0.68 and (title or desc) and (exactcritical or "BASENAME_IDENTITY" in e) else "AMBIGUOUS_CANDIDATE")
   if proven:e.append("OFFICIAL_SELECTED_CHANGELOG_"+signal)
   row={"oldId":x,"targetId":y,"classification":status,"confidenceScore":round(score,3),"evidenceCategories":e,"criteriaSimilarity":round(ratio,6),"progressMigrationRequired":False}
   candidates.append(row);byold[x].append(row);bynew[y].append(row)
 statusorder={"CONFIRMED_RENAME_OR_MOVE":0,"HIGH_CONFIDENCE_CANDIDATE":1,"AMBIGUOUS_CANDIDATE":2}
 candidates.sort(key=lambda r:(r["oldId"],statusorder[r["classification"]],-r["confidenceScore"],r["targetId"]))
 # Leave alternatives visible; never enforce one-to-one assignments.
 return {"officialEvidence":official,"pairs":candidates,"oldOnly":[{"resourceId":x,"classification":next((r["classification"] for r in candidates if r["oldId"]==x),"NO_MATCH"),"candidates":[r["targetId"] for r in candidates if r["oldId"]==x]} for x in oldonly],"targetOnly":[{"resourceId":x,"classification":min((r["classification"] for r in bynew[x]),key=lambda k:statusorder[k]) if bynew[x] else "NO_MATCH","candidates":[r["oldId"] for r in candidates if r["targetId"]==x]} for x in newonly],"summary":{**dict(collections.Counter(x["classification"] for x in candidates)),"unmatchedOldOnly":sum(not byold[x] for x in oldonly),"unmatchedTargetOnly":sum(not bynew[x] for x in newonly)}}
def official_evidence(repo,pin):
 row=next(r for r in pin["temporaryMetadataSnapshots"] if r["temporaryPath"].endswith("official_selected_version_metadata.json"))
 p=repo/row["temporaryPath"];assert sha(p.read_bytes())==row["sha256"]
 d=read_json(p);assert d["id"]=="Y2zZ5eSs";text=d["changelog"].lower()
 # Signals require named old/new labels and local sentence/line context.
 checks={
 "SPEAR_FISHING_RENAMED_SEAFOOD_SKEWER":lambda s:"spear fishing" in s and "seafood skewer" in s and "renam" in s,
 "BIRDKEEPER_MOVED_ANIMALS_TO_BIOMES":lambda s:"birdkeeper" in s and "biomes" in s and ("mov" in s or "animals" in s),
 "CHATTERBOX_MOVED_ANIMALS_TO_BIOMES":lambda s:"chatterbox" in s and "biomes" in s and ("mov" in s or "animals" in s),
 "TWO_BY_TWO_OVERPOPULATION_NAME_SWAP":lambda s:"two by two" in s and "overpopulation" in s and ("swap" in s or "switch" in s),
 }
 lines=text.splitlines();valid=[key for key,fn in checks.items() if any(fn(line) for line in lines)]
 return {"path":row["temporaryPath"],"sha256":row["sha256"],"versionId":d["id"],"source":"B1-retained official selected Modrinth metadata; no network","validatedSignals":sorted(valid),"textRedistributed":False}
def resource_diff(old,new):
 ro=old["resources"];rn=new["resources"];rows=[];rel=[]
 for p in sorted(ro.keys()|rn.keys()):
  a=ro.get(p);b=rn.get(p);meta=b or a
  cls="ADDED_PATH" if a is None else ("REMOVED_PATH" if b is None else ("UNCHANGED_PATH_IDENTICAL" if a["byteSha256"]==b["byteSha256"] else "UNCHANGED_PATH_CHANGED"))
  rows.append({k:meta[k] for k in ["path","namespace","resourceType","registryType","resourceId"]}|{"classification":cls,"oldByteSha256":a["byteSha256"] if a else None,"targetByteSha256":b["byteSha256"] if b else None,"oldNormalizedSha256":a["normalizedSha256"] if a else None,"targetNormalizedSha256":b["normalizedSha256"] if b else None,"parsedOrNormalizedIdentical":bool(a and b and a["normalizedSha256"]==b["normalizedSha256"]),"oldSize":a["size"] if a else None,"targetSize":b["size"] if b else None})
 byhash=collections.defaultdict(list)
 for p in sorted(rn.keys()-ro.keys()):
  b=rn[p];byhash[(b["resourceType"],b["normalizedSha256"])].append(p)
 for p in sorted(ro.keys()-rn.keys()):
  a=ro[p]
  for q in byhash.get((a["resourceType"],a["normalizedSha256"]),[]):
   rel.append({"oldPath":p,"targetPath":q,"evidence":"IDENTICAL_BYTES" if a["byteSha256"]==rn[q]["byteSha256"] else "NORMALIZED_CONTENT_HASH","semanticEquivalenceClaimed":False})
 # Also detect content moved while a delegating compatibility entry retains old path.
 for p in sorted(ro.keys()&rn.keys()):
  a=ro[p]
  if a["byteSha256"]==rn[p]["byteSha256"]:continue
  for q in byhash.get((a["resourceType"],a["normalizedSha256"]),[]):
   rel.append({"oldPath":p,"targetPath":q,"evidence":"NORMALIZED_CONTENT_RELOCATION_WITH_RETAINED_CHANGED_PATH","semanticEquivalenceClaimed":False})
 counts=collections.Counter((x["namespace"],x["resourceType"],x["classification"]) for x in rows)
 return rows,[{"namespace":n,"resourceType":t,"classification":c,"count":v} for (n,t,c),v in sorted(counts.items())],rel
def mask_quoted(text):
 # Keep command tokens while excluding quoted tellraw/click payloads.
 return re.sub(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'',lambda m:" "*len(m[0]),text)
def literal_arguments(text):
 m=re.search(r'\{(.*)\}\s*$',text)
 if not m:return {}
 return {k:v for k,q,v in re.findall(r'([A-Za-z0-9_]+)\s*:\s*(["\'])(.*?)\2',m[1])}
CALL_RE=re.compile(r"(?:^|(?<=\s))(?:schedule\s+)?function\s+(#?[A-Za-z0-9_$().:/-]+)")
PRED_RE=re.compile(r"\b(?:if|unless)\s+predicate\s+([A-Za-z0-9_.:/-]+)")
def function_graph(pack,path_sensitive_ids):
 funcs={r["resourceId"]:r for r in pack["resources"].values() if r["resourceType"]=="function"}
 tags={r["resourceId"]:r for r in pack["resources"].values() if r["resourceType"]=="tags" and r["registryType"]=="function"}
 edges=[];preds=[];macros=[];allcontexts=collections.defaultdict(dict);commands=collections.defaultdict(list)
 for fid,r in sorted(funcs.items()):
  for ln,line in enumerate(r["text"].splitlines(),1):
   t=line.lstrip()
   if not t or t.startswith("#"):continue
   macro=t.startswith("$");t=t[1:] if macro else t
   if macro:macros.append({"function":fid,"path":r["path"],"line":ln,"parameters":sorted(set(re.findall(r"\$\(([^)]+)\)",t)))})
   mask=mask_quoted(t)
   for m in CALL_RE.finditer(mask):
    raw=t[m.start():m.end()].split()[-1];is_tag=raw.startswith("#");target=rid(raw.lstrip("#"))
    dynamic="$(" in target
    e={"source":fid,"sourcePath":r["path"],"line":ln,"callee":target,"functionTag":is_tag,"dynamic":dynamic,"macroExpanded":False}
    edges.append(e);commands[fid].append(e)
    params=literal_arguments(t)
    if params and not dynamic and not is_tag:allcontexts[target][semhash(params)]=(params,fid,ln)
   for m in PRED_RE.finditer(mask):preds.append({"source":fid,"sourcePath":r["path"],"line":ln,"kind":"predicate","resourceId":rid(m[1])})
 # Bind concrete wrapper arguments to shared macro calls. Other with-storage/entity
 # argument sources remain explicitly unresolved; this is a best-effort graph.
 for fid,contexts in sorted(allcontexts.items()):
  if fid not in funcs:continue
  for ctx,(params,caller,callerline) in sorted(contexts.items()):
   for e in commands[fid]:
    if not e["dynamic"]:continue
    callee=re.sub(r"\$\(([^)]+)\)",lambda m:params.get(m[1],m[0]),e["callee"])
    if "$(" in callee:continue
    edges.append({**e,"callee":callee,"dynamic":False,"macroExpanded":True,"argumentSourceFunction":caller,"argumentSourceLine":callerline,"argumentFingerprint":ctx})
 for tid,r in sorted(tags.items()):
  for i,value in enumerate(r["json"].get("values",[])):
   v=value.get("id") if isinstance(value,dict) else value
   if not isinstance(v,str):continue
   edges.append({"source":"#"+tid,"sourcePath":r["path"],"line":None,"callee":rid(v.lstrip("#")),"functionTag":v.startswith("#"),"dynamic":False,"macroExpanded":False,"required":value.get("required",True) if isinstance(value,dict) else True,"tagMemberIndex":i})
 grouped={};keep=[]
 for e in edges:
  if not e["macroExpanded"]:keep.append(e);continue
  k=(e["source"],e["callee"],e["functionTag"],e["argumentFingerprint"])
  if k not in grouped:grouped[k]={**e,"macroExpandedSourceLines":[]}
  grouped[k]["macroExpandedSourceLines"].append(e["line"])
 for e in grouped.values():e["macroExpandedSourceLines"]=sorted(set(e["macroExpandedSourceLines"]))
 direct={}
 for e in keep:
  k=(e["source"],e["callee"],e["functionTag"],e["dynamic"],e.get("tagMemberIndex"))
  if k not in direct:direct[k]={**e,"sourceLines":[],"callSiteOccurrences":0}
  if e["line"] is not None:direct[k]["sourceLines"].append(e["line"])
  direct[k]["callSiteOccurrences"]+=1
 for e in direct.values():e["sourceLines"]=sorted(set(e["sourceLines"]))
 for e in grouped.values():e["callSiteOccurrences"]=len(e["macroExpandedSourceLines"])
 edges=list(direct.values())+list(grouped.values())
 for e in edges:
  if e["dynamic"]:e["resolution"]="DYNAMIC_UNRESOLVED";continue
  e["resolution"]="PACK_LOCAL" if e["callee"] in (tags if e["functionTag"] else funcs) else ("EXTERNAL_FANPACK_EXTENSION_HOOK_REVIEW_REQUIRED" if e["functionTag"] and e["callee"].startswith("bacap_fanpacks:") else ("OPTIONAL_ABSENT" if e.get("required") is False else "MISSING_STATIC_CALLEE"))
 rewards=[r["json"].get("rewards",{}).get("function") for r in pack["adv"].values()]
 seeds=set(rid(x) for x in rewards if isinstance(x,str))
 seeds|={f for f in funcs if f.startswith("blazeandcave:") and any(s in f for s in ["load","setup","update","config/","new_world","global_install"])}
 seeds|={f for f in funcs if f.endswith("/root") or f in path_sensitive_ids}
 seeds|={"#"+t for t in tags if t.startswith("bacap_fanpacks:") or t in ["minecraft:load","minecraft:tick"]}
 adjacency=collections.defaultdict(set)
 for e in edges:
  if not e["dynamic"]:adjacency[e["source"]].add(("#" if e["functionTag"] else "")+e["callee"])
 reached=set();stack=sorted(seeds)
 while stack:
  f=stack.pop()
  if f in reached:continue
  reached.add(f);stack.extend(sorted(adjacency.get(f,set())-reached))
 for e in edges:
  e["reachable"]=e["source"] in reached
  if e["macroExpanded"] and e["resolution"]=="MISSING_STATIC_CALLEE":e["resolution"]="CONDITIONAL_MACRO_RESOURCE_ABSENT_REVIEW"
 edges.sort(key=lambda x:(x["source"],x["line"] or 0,x["callee"],x.get("argumentFingerprint","")))
 return {"functions":funcs,"tags":tags,"edges":edges,"predicateReferences":preds,"seeds":sorted(seeds),"reachable":sorted(reached),"macroCommandLines":macros,"literalMacroContexts":sum(len(v) for v in allcontexts.values())}
def tag_map(pack):
 mp=collections.defaultdict(list)
 for r in pack["resources"].values():
  if r["resourceType"]=="tags" and r["registryType"]!="function":mp[r["resourceId"]].append(r["path"])
 return mp
def references(pack,graph,old):
 refs=[];tm=tag_map(pack);om=tag_map(old)
 predicates={r["resourceId"] for r in pack["resources"].values() if r["resourceType"]=="predicate"}
 for a in pack["adv"].values():refs.extend(json_refs(a["json"],a["path"],tm))
 for r in pack["resources"].values():
  if r["resourceType"] in ["predicate","dialog"]:refs.extend(json_refs(r["json"],r["path"],tm))
 refs.extend(graph["predicateReferences"])
 for f in graph["reachable"]:
  if f not in graph["functions"]:continue
  r=graph["functions"][f]
  for ln,line in enumerate(r["text"].splitlines(),1):
   mask=mask_quoted(line.lstrip("$"))
   # Actual command registry-tag arguments, not display text.
   for m in re.finditer(r"#([a-z0-9_.-]+:[a-z0-9_./-]+)",mask):
    if re.search(r"function\s*$",mask[:m.start()]):continue
    token=m[1]
    refs.append({"source":r["path"],"line":ln,"kind":"registry_tag","resourceId":token,"registryType":None,"localResourcePaths":sorted(tm.get(token,[]))})
 for r in pack["resources"].values():
  if r["resourceType"]!="tags" or r["registryType"]=="function":continue
  for i,v in enumerate(r["json"].get("values",[])):
   val=v.get("id") if isinstance(v,dict) else v
   if isinstance(val,str) and val.startswith("#"):refs.append({"source":r["path"],"location":"/values/"+str(i),"kind":"registry_tag","resourceId":rid(val[1:]),"registryType":r["registryType"],"localResourcePaths":sorted(tm.get(rid(val[1:]),[])),"required":v.get("required",True) if isinstance(v,dict) else True})
 for x in refs:
  if x["kind"]=="predicate":x["resolution"]="PACK_LOCAL" if x["resourceId"] in predicates else "MISSING_PACK_LOCAL_PREDICATE"
  else:
   local=x.get("localResourcePaths",[])
   x["oldPackLocalPaths"]=sorted(om.get(x["resourceId"],[]))
   x["resolution"]="PACK_LOCAL_REGISTRY_TAG" if local else ("REMOVED_OLD_PACK_TAG_REGISTRY_EXISTENCE_REVIEW" if x["oldPackLocalPaths"] else ("EXTERNAL_REGISTRY_TAG" if x["resourceId"].startswith("minecraft:") else ("OPTIONAL_ABSENT" if x.get("required") is False else "PACK_NAMESPACE_TAG_NOT_DEFINED_REVIEW")))
 # Registry existence cannot be inferred without Minecraft runtime registries;
 # avoid misclassifying vanilla registry tags as absent BACAP resources.
 return sorted(refs,key=lambda x:(x["source"],x.get("line",0),x.get("location",""),x["kind"],x["resourceId"]))
def clean_resource(r):
 return {k:v for k,v in r.items() if k not in ["bytes","text","json"]}
def path_id(p):
 r=residentity(p);return r["resourceId"] if r else None
def source_highlights(repo,path):
 t=(repo/path).read_text(encoding="utf-8-sig")
 return sorted(set(re.findall(r"/advancementssearch\s+highlight\s+([a-z0-9_.-]+:[a-z0-9_./-]+)",t)))
def summarize_rows(rows,key):return dict(sorted(collections.Counter(x[key] for x in rows).items()))

def contract_probe(old_path,target_path,b2_inventory_path):
    """Copyright-safe, read-only static BACAP scoreboard/configuration probe.
    
    No commands are executed. JSON reports contain identifiers, source locations,
    hashes and structural facts only. Selectors/macros remain symbolic.
    """
    
    from collections import Counter, defaultdict
    import hashlib
    import json
    from pathlib import Path
    import re
    import zipfile
    
    PROGRESSION = (
        'bac_advancements', 'bac_advfirst', 'bac_advancements_team',
        'bac_advfirst_team_sum', 'bac_advfirst_sum', 'bac_advfirst_team',
    )
    CONFIG = ('bac_settings', 'bac_created', 'bac_points', 'bac_dont_count')
    SETTING_PLAYERS = ('adv_score', 'extra_reward', 'extra_trophy', 'intro_msg',
                       'reward', 'exp', 'trophy', 'coop', 'checking')
    POINT_OBJECTIVES = ('bac_advancements_points', 'bac_advancements_team_points')
    OBJ_DEF = re.compile(r'\bscoreboard objectives add (\S+) (\S+)')
    OBJ_CMD = re.compile(r'\bscoreboard objectives (?:remove|modify) (\S+)')
    DISPLAY = re.compile(r'\bscoreboard objectives setdisplay (\S+) (\S+)')
    PLAYER_CMD = re.compile(r'\bscoreboard players (set|add|remove|get|reset|enable) (\S+) (\S+)(?: (-?\d+))?')
    OPERATION = re.compile(r'\bscoreboard players operation (\S+) (\S+) (\S+) (\S+) (\S+)')
    COND = re.compile(r'\b(if|unless) score (\S+) (\S+)')
    COMPARE = re.compile(r'\b(?:if|unless) score (\S+) (\S+) (?:<|<=|=|>=|>) (\S+) (\S+)')
    STORE = re.compile(r'\bstore (?:result|success) score (\S+) (\S+)')
    FUNC = re.compile(r'\b(?:function|schedule function) (#?[a-z0-9_]+:[a-z0-9_./$()\-]+)')
    ADV = re.compile(r'advancements=\{([^}]+)\}')
    ADV_ITEM = re.compile(r'([a-z0-9_]+:[a-z0-9_./\-]+)=(true|false)')
    
    
    def sha(data):
        return hashlib.sha256(data).hexdigest()
    
    
    def read_functions(path):
        with zipfile.ZipFile(path) as z:
            return {p: z.read(p) for p in sorted(z.namelist())
                    if re.fullmatch(r'data/[^/]+/functions?/.+\.mcfunction', p)}
    
    
    def _id(path):
        parts = path.split('/')
        return parts[1] + ':' + '/'.join(parts[3:])[:-11]
    
    
    def _scan(functions):
        objectives = defaultdict(lambda: {'definitions': [], 'usageCount': 0,
                                          'locations': defaultdict(list), 'fakePlayers': set(),
                                          'writeNumericValues': defaultdict(set)})
        named_players = defaultdict(lambda: {'usageCount': 0, 'locations': defaultdict(list),
                                            'setValues': set()})
        calls = {}
        methods = {}
    
        def record(obj, path, number, kind, holder=None, value=None):
            if not obj or not re.fullmatch(r'[A-Za-z0-9_.:$()\-]+', obj):
                return
            data = objectives[obj]
            data['usageCount'] += 1
            data['locations'][path].append({'line': number, 'kind': kind})
            if holder and not holder.startswith('@') and ':' not in holder:
                data['fakePlayers'].add(holder)
                player = named_players[(obj, holder)]
                player['usageCount'] += 1
                player['locations'][path].append(number)
                if kind == 'set' and value is not None:
                    player['setValues'].add(value)
            if value is not None and kind in ('set', 'add', 'remove'):
                data['writeNumericValues'][kind].add(value)
    
        for path, byte_data in functions.items():
            text = byte_data.decode('utf-8-sig')
            lines = text.splitlines()
            path_calls = []
            increments = Counter()
            completion_ids = set()
            macro_invocations = []
            macro_lines = []
            for number, line in enumerate(lines, 1):
                active = line.strip()
                if not active or active.startswith('#'):
                    continue
                original_line = line
                # Prose/help text may contain illustrative scoreboard commands.
                # Exclude it from objective syntax while retaining genuine JSON
                # score components separately. Embedded clickable commands are not
                # treated as executed initialization or count increment behavior.
                if re.match(r'^\$?(?:tellraw|title|say|tell)\b', active):
                    line = ''
                else:
                    line = re.split(r'\brun (?:tellraw|title|say|tell)\b', line, maxsplit=1)[0]
                if active.startswith('$'):
                    macro_lines.append(number)
                for match in OBJ_DEF.finditer(line):
                    obj, criterion = match.groups()
                    objectives[obj]['definitions'].append({'path': path, 'line': number,
                                                          'criterion': criterion})
                    record(obj, path, number, 'definition')
                for match in OBJ_CMD.finditer(line):
                    record(match[1], path, number, 'objective_operation')
                for match in DISPLAY.finditer(line):
                    record(match[2], path, number, 'display:' + match[1])
                for match in PLAYER_CMD.finditer(line):
                    kind, holder, obj, num = match.groups()
                    value = int(num) if num is not None else None
                    record(obj, path, number, kind, holder, value)
                    if kind == 'add':
                        increments[(obj, value)] += 1
                for match in OPERATION.finditer(line):
                    holder, obj, op, source_holder, source_obj = match.groups()
                    record(obj, path, number, 'operation_target:' + op, holder)
                    record(source_obj, path, number, 'operation_source:' + op, source_holder)
                for match in COND.finditer(line):
                    condition, holder, obj = match.groups()
                    record(obj, path, number, 'condition:' + condition, holder)
                for match in COMPARE.finditer(line):
                    record(match[4], path, number, 'comparison_source', match[3])
                for match in STORE.finditer(line):
                    record(match[2], path, number, 'store', match[1])
                for selector_scores in re.finditer(r'scores=\{([^}]+)\}', line):
                    for obj in re.findall(r'([A-Za-z0-9_.$()\-]+)\s*=', selector_scores[1]):
                        record(obj, path, number, 'selector_filter')
                for score_component in re.finditer(r'"score"\s*:\s*\{([^}]+)\}', original_line):
                    obj_m = re.search(r'"objective"\s*:\s*"([^"]+)"', score_component[1])
                    holder_m = re.search(r'"name"\s*:\s*"([^"]+)"', score_component[1])
                    if obj_m:
                        record(obj_m[1], path, number, 'text_component', holder_m[1] if holder_m else None)
                for match in FUNC.finditer(line):
                    path_calls.append({'line': number, 'target': match[1],
                                       'dynamic': '$(' in match[1]})
                for match in ADV.finditer(line):
                    completion_ids.update(m[0] for m in ADV_ITEM.findall(match[1]))
                # The only retained macro argument values are resource IDs and tiers.
                if 'advancement_made_macro' in line:
                    arguments = dict(re.findall(r'(adv_id|reward_id|tier):"([^\"]+)"', line))
                    macro_invocations.append({'line': number, 'arguments': arguments})
            calls[path] = path_calls
            methods[path] = {
                'sha256': sha(byte_data),
                'normalizedCommandsSha256': sha('\n'.join(l.strip() for l in lines
                                                          if l.strip() and not l.lstrip().startswith('#')).encode()),
                'lineCount': len(lines),
                'activeCommandCount': sum(bool(l.strip()) and not l.lstrip().startswith('#') for l in lines),
                'macroLines': macro_lines,
                'macroInvocations': macro_invocations,
                'calls': path_calls,
                'increments': [{'objective': obj, 'amount': amount, 'commandCount': count}
                               for (obj, amount), count in sorted(increments.items(), key=lambda x: str(x[0]))],
                'completionSelectorIds': sorted(completion_ids),
            }
        objectives_out = {}
        for obj in sorted(objectives):
            d = objectives[obj]
            objectives_out[obj] = {
                'definitions': d['definitions'], 'usageCount': d['usageCount'],
                'locations': [{'path': path, 'references': refs} for path, refs in sorted(d['locations'].items())],
                'fakePlayers': sorted(d['fakePlayers']),
                'writeNumericValues': {k: sorted(v) for k, v in sorted(d['writeNumericValues'].items())},
            }
        players_out = {}
        for (obj, holder), data in sorted(named_players.items()):
            players_out.setdefault(obj, {})[holder] = {
                'usageCount': data['usageCount'], 'setValues': sorted(data['setValues']),
                'locations': [{'path': p, 'lines': sorted(set(n))} for p, n in sorted(data['locations'].items())]}
        return {'objectives': objectives_out, 'fakePlayers': players_out, 'functions': methods}
    
    
    def _function_fact(path, old, target):
        a, b = old['functions'].get(path), target['functions'].get(path)
        status = ('NO_MAIN_ARCHIVE_RESOURCE' if not a and not b else
                  'ADDED_PATH' if not a else 'REMOVED_PATH' if not b else
                  'UNCHANGED_PATH_IDENTICAL' if a['sha256'] == b['sha256'] else 'UNCHANGED_PATH_CHANGED')
        return {'path': path, 'resourceId': _id(path), 'classification': status, 'old': a, 'target': b}
    
    
    def _contract(obj, old, target):
        a, b = old['objectives'].get(obj), target['objectives'].get(obj)
        return {'objective': obj, 'oldPresent': bool(a), 'targetPresent': bool(b),
                'old': a, 'target': b,
                'definitionCriteriaChanged': ({d['criterion'] for d in a['definitions']} if a else set()) !=
                                             ({d['criterion'] for d in b['definitions']} if b else set()),
                'b4Disposition': 'SETUP_SCOREBOARD_REVIEW_REQUIRED'}
    
    
    def analyze(oldpath, targetpath, b2_inventory_path):
        old_functions = read_functions(oldpath)
        target_functions = read_functions(targetpath)
        old = _scan(old_functions)
        target = _scan(target_functions)
        old_defined = {name for name, data in old['objectives'].items() if data['definitions']}
        target_defined = {name for name, data in target['objectives'].items() if data['definitions']}
        added_defined = sorted(target_defined - old_defined)
        removed_defined = sorted(old_defined - target_defined)
        objective_summaries = [_contract(obj, old, target)
                               for obj in sorted(set(old['objectives']) | set(target['objectives']))]
        fake_diff = []
        for obj in sorted(set(old['fakePlayers']) | set(target['fakePlayers'])):
            a, b = old['fakePlayers'].get(obj, {}), target['fakePlayers'].get(obj, {})
            fake_diff.append({'objective': obj, 'added': sorted(set(b) - set(a)),
                              'removed': sorted(set(a) - set(b)),
                              'initializationChanged': [holder for holder in sorted(set(a) & set(b))
                                                        if a[holder]['setValues'] != b[holder]['setValues']],
                              'old': a, 'target': b})
    
        paths = sorted(p for p in set(old_functions) | set(target_functions)
                       if re.search(r'/(?:score_add|first_score_add|first_team_score_add|update_score|update_points|advancement_made_macro|new_world|start_timers|global_install|load|setup_[^/]+)\.mcfunction$', p)
                       or re.search(r'/config/(?:scoreboard_|update_|coop_|item_rewards_|exp_rewards_|trophies_|msg_hidden)', p))
        function_facts = [_function_fact(p, old, target) for p in paths]
        old_update = old['functions']['data/bacap_rewards/function/update_score.mcfunction']
        new_update = target['functions']['data/blazeandcave/function/config/update_score.mcfunction']
        counted_a, counted_b = set(old_update['completionSelectorIds']), set(new_update['completionSelectorIds'])
        tally = {'oldSubstantivePath': 'data/bacap_rewards/function/update_score.mcfunction',
                 'targetSubstantivePath': 'data/blazeandcave/function/config/update_score.mcfunction',
                 'oldCompletionSelectorCount': len(counted_a), 'targetCompletionSelectorCount': len(counted_b),
                 'selectorIdsRemoved': sorted(counted_a - counted_b), 'selectorIdsAdded': sorted(counted_b - counted_a),
                 'legacyEntryPointRetainedAsDelegate': True,
                 'rawIncrementAmountUnchanged': all(d['amount'] == 1 for d in new_update['increments']
                                                  if d['objective'] == 'bac_advancements'),
                 'targetAlsoInvokesPointsUpdate': any(c['target'] == 'blazeandcave:config/update_points'
                                                    for c in new_update['calls']),
                 'old': old_update, 'target': new_update,
                 'b4Disposition': 'SETUP_SCOREBOARD_REVIEW_REQUIRED'}
    
        setup_wrapper = []
        b2path = Path(b2_inventory_path)
        if b2path.exists():
            b2 = json.loads(b2path.read_text(encoding='utf-8'))
            atd_supplied = {_id(f['resourcePath']) for pack in b2['resourcePacks']['packs'] for f in pack['files']
                            if f['resourcePath'].endswith('.mcfunction')}
            for pack in b2['resourcePacks']['packs']:
                for file in pack['files']:
                    category = file['functionClass']
                    if category in ('PACK_METADATA', 'REWARD_MESSAGES_LOCALIZATION'):
                        continue
                    path = file['resourcePath']
                    facts = _function_fact(path, old, target)
                    source = Path(file['path']).read_text(encoding='utf-8')
                    call_ids = sorted(set(FUNC.findall(source)))
                    known = {_id(p) for p in target_functions}
                    facts.update({'atdPath': file['path'], 'pack': pack['name'], 'functionClass': category,
                                  'atdSha256': file['sha256'], 'atdDirectCalls': call_ids,
                                  'atdMissingDirectTargetCalls': [i for i in call_ids if not i.startswith('#')
                                                                  and '$(' not in i and i not in known],
                                  'atdSuppliedDirectCallsAbsentFromTargetArchive': [i for i in call_ids
                                                                                   if i not in known and i in atd_supplied],
                                  'unresolvedMainAndAtdDirectCalls': [i for i in call_ids if not i.startswith('#')
                                                                     and '$(' not in i and i not in known and i not in atd_supplied],
                                  'companionScope': pack['name'] not in ('bacap_override', 'bacap_cooperative_mode',
                                                                        'bacap_rewards_item', 'bacap_rewards_experience',
                                                                        'bacap_rewards_trophy'),
                                  'b4Disposition': 'OVERRIDE_RESOURCE_UPDATE_REQUIRED' if category in ('REWARD_WRAPPER', 'ROOT_CATEGORY_OR_MILESTONE_REWARD')
                                  else 'SETUP_SCOREBOARD_REVIEW_REQUIRED'})
                    setup_wrapper.append(facts)
    
        macro_files = [{'path': path, 'sha256': facts['sha256'], 'macroInvocations': facts['macroInvocations']}
                       for path, facts in target['functions'].items() if facts['macroInvocations']]
        macro_tiers = Counter(a['arguments'].get('tier', 'UNKNOWN') for f in macro_files for a in f['macroInvocations'])
        points = {
            'addedWeightedPointObjectives': [o for o in POINT_OBJECTIVES if o in added_defined],
            'pointTierDefaults': {holder: row['setValues'] for holder, row in
                                 target['fakePlayers'].get('bac_points', {}).items() if row['setValues']},
            'rawObjectivesRemainSeparate': all(o in target_defined for o in PROGRESSION),
            'sixAtdProgressionDefinitionCriteriaRemain': all(not _contract(o, old, target)['definitionCriteriaChanged']
                                                           for o in PROGRESSION),
            'rawScoreAddEntryPointByteIdentical': old['functions']['data/bacap_rewards/function/score_add.mcfunction']['sha256'] ==
                                               target['functions']['data/bacap_rewards/function/score_add.mcfunction']['sha256'],
            'macroWrapperCount': len(macro_files), 'macroTierCounts': dict(sorted(macro_tiers.items())),
            'macroWrapperIdentities': macro_files,
            'rawCountingTierExclusionObjective': 'bac_dont_count',
            'rawCountingExclusionDefaults': {holder: row['setValues'] for holder, row in
                                           target['fakePlayers'].get('bac_dont_count', {}).items() if row['setValues']},
            'targetMacroRawCountGatedByTierExclusion': True,
            'fanpackUpdatePointsHookAdded': '#bacap_fanpacks:update_points',
            'atdUsesWeightedPoints': False,
            'b4Disposition': 'SETUP_SCOREBOARD_REVIEW_REQUIRED',
            'deferred': 'Ability threshold/balance/pacing changes remain DEFER_TO_PHASE_C_OR_D.'}
    
        return {
            'analysis': 'READ_ONLY_STATIC_SCOREBOARD_AND_CONFIG_CONTRACTS',
            'limitations': ['Static command scan, no Minecraft execution.',
                           'Selectors and macros remain symbolic; scoreboard arithmetic is not simulated.',
                           'Undefined objectives may be created by fanpacks or commands; not automatically dangling.',
                           'Likely rename evidence is identifier/criterion comparison, not equivalence proof.'],
            'summary': {'oldDefinedObjectives': len(old_defined), 'targetDefinedObjectives': len(target_defined),
                        'addedDefinedObjectives': len(added_defined), 'removedDefinedObjectives': len(removed_defined),
                        'oldReferencedObjectives': len(old['objectives']),
                        'targetReferencedObjectives': len(target['objectives']),
                        'atdSetupAndRootWrapperFilesAccounted': len(setup_wrapper)},
            'addedDefinedObjectives': added_defined, 'removedDefinedObjectives': removed_defined,
            'likelyObjectiveRenameCandidates': [{'old': a, 'target': b, 'confidence': 'AMBIGUOUS_CANDIDATE',
                                                'evidence': ['RELATED_IDENTIFIER_STEM', 'DEFINITION_CRITERION_REVIEW_REQUIRED']}
                                               for a, b in [('bac_apple_eaten', 'bac_apple_today'),
                                                            ('bac_1000th_item', 'bac_1000th_item_dummy')]
                                               if a in removed_defined and b in added_defined],
            'objectiveContracts': objective_summaries,
            'fakePlayerContracts': fake_diff,
            'sixAtdProgressionObjectives': [_contract(o, old, target) for o in PROGRESSION],
            'setupContractObjectives': [_contract(o, old, target) for o in CONFIG],
            'setupFakePlayerContracts': [{'player': holder,
                                         'old': old['fakePlayers'].get('bac_settings', {}).get(holder),
                                         'target': target['fakePlayers'].get('bac_settings', {}).get(holder)}
                                        for holder in SETTING_PLAYERS],
            'functionBehaviorFacts': function_facts,
            'updateScoreBehavior': tally,
            'weightedPointsVsRawCount': points,
            'atdSetupAndRootWrappers': setup_wrapper,
        }
    
    return analyze(old_path,target_path,b2_inventory_path)

def converter_probe(old_path,target_path,repo_root,temp_root):
    """B3 analysis: run unchanged published conversion sources on ignored ZIP copies.
    
    No network, Gradle invocation, product writes or Minecraft commands are used.
    The entry-level output is compared with an instrumented diagnostic COPY. JSON
    family attribution is inclusive: a parent adapter may report nested changes.
    """
    import argparse, hashlib, json, os, re, shutil, subprocess, zipfile
    from pathlib import Path
    
    JAVA_PROBE = r'''
    package com.diskree.achievetodo.client;
    import com.google.gson.*;
    import java.nio.file.*;
    import java.nio.charset.StandardCharsets;
    import java.security.*;
    import java.util.*;
    import java.util.zip.*;
    import java.lang.reflect.*;
    import java.util.regex.Pattern;
    
    public class B3ConverterProbe {
      static TreeMap<String,TreeSet<String>> candidates = new TreeMap<>(), changes = new TreeMap<>();
      static String entry;
      static void trace(String family, boolean changed) {
        candidates.computeIfAbsent(family,k->new TreeSet<>()).add(entry);
        if (changed) changes.computeIfAbsent(family,k->new TreeSet<>()).add(entry);
      }
      static Object call(Class<?> c,String method,Class<?>[] types,Object...args) throws Exception {
        Method m=c.getDeclaredMethod(method,types); m.setAccessible(true);
        try { return m.invoke(null,args); } catch(InvocationTargetException e) { throw new RuntimeException(e.getCause()); }
      }
      static String converted(Class<?> c,String kind,String input) throws Exception {
        Object r=call(c,kind,new Class<?>[]{String.class},input);
        Method text=r.getClass().getDeclaredMethod("text"); text.setAccessible(true);
        return (String)text.invoke(r);
      }
      static String hash(String s) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
      }
      static void diff(JsonElement a,JsonElement b,String path,List<String> out) {
        if (Objects.equals(a,b)) return;
        if(a!=null && b!=null && a.isJsonObject() && b.isJsonObject()) {
          var keys=new TreeSet<String>(); keys.addAll(a.getAsJsonObject().keySet());keys.addAll(b.getAsJsonObject().keySet());
          for(String k:keys)diff(a.getAsJsonObject().get(k),b.getAsJsonObject().get(k),path+"/"+k.replace("~","~0").replace("/","~1"),out);
        } else if(a!=null && b!=null && a.isJsonArray() && b.isJsonArray() && a.getAsJsonArray().size()==b.getAsJsonArray().size()) {
          for(int i=0;i<a.getAsJsonArray().size();i++)diff(a.getAsJsonArray().get(i),b.getAsJsonArray().get(i),path+"/"+i,out);
        } else out.add(path);
      }
      static final String[] PF={"HIDE_ADDITIONAL_TOOLTIP","DYED_COLOR_WITH_TOOLTIP","ENCHANTMENTS_WITH_TOOLTIP","STORED_ENCHANTMENTS_WITH_TOOLTIP","ENCHANTMENTS_LEVELS","STORED_ENCHANTMENTS_LEVELS","UNBREAKABLE_WITH_TOOLTIP","TRIM_WITH_TOOLTIP"};
      static final String[] PR={"","dyed_color=$1","enchantments=$1","stored_enchantments=$1","enchantments=$1","stored_enchantments=$1","unbreakable={}","trim={$1}"};
      static void functionFamilies(String text) throws Exception {
        for(String original:text.split("\\R",-1)) {
          String line=original;
          for(int i=0;i<PF.length;i++) {
            Field f=ExternalPackCompatibility.class.getDeclaredField(PF[i]);f.setAccessible(true);Pattern p=(Pattern)f.get(null);
            if(p.matcher(line).find()){
              String next=p.matcher(line).replaceAll(PR[i]);
              if(i==0)next=next.replace("[,","[").replace(",]","]").replace(",,",",");
              trace("function_"+PF[i].toLowerCase(Locale.ROOT),!next.equals(line));line=next;
            }
          }
          if(line.contains("minecraft:chain")){String next=line.replace("minecraft:chain","minecraft:iron_chain");trace("function_chain_id",!next.equals(line));line=next;}
          if(line.contains("time query daytime")){String next=line.replace("time query daytime","time of minecraft:overworld query minecraft:day");trace("function_daytime_query",!next.equals(line));line=next;}
          String next=(String)call(ExternalPackCompatibility.class,"rewriteLegacyGameruleLine",new Class<?>[]{String.class},line);
          if(line.contains("gamerule"))trace("function_gamerule",!next.equals(line));line=next;
          next=LegacyItemText.migrateCommand(line);
          String diagnostic=InstrumentedItemText.migrateCommand(line); if(!diagnostic.equals(next))throw new IllegalStateException("Item diagnostic disagrees");
          if(line.matches(".*(?:custom_name|item_name|lore)=.*"))trace("function_legacy_item_text",!next.equals(line));
        }
      }
      public static void main(String[] args)throws Exception {
        Path source=Path.of(args[0]),output=Path.of(args[1]); Files.createDirectories(output.getParent());
        JsonObject report=new JsonObject();JsonArray entries=new JsonArray();
        int inputJson=0,inputFn=0,changedJson=0,changedFn=0,parseFailures=0,idempotenceFailures=0,diagnosticDisagreements=0,chatCandidates=0,chatChanged=0;
        try(ZipFile zip=new ZipFile(source.toFile())) {
          var names=zip.stream().filter(e->!e.isDirectory()).map(ZipEntry::getName).sorted().toList();
          for(String path:names){
            boolean json=path.endsWith(".json"),fn=path.endsWith(".mcfunction");if(!json&&!fn)continue;
            entry=path;String text=new String(zip.getInputStream(zip.getEntry(path)).readAllBytes(),StandardCharsets.UTF_8);
            if(json)inputJson++;else inputFn++;
            JsonObject row=new JsonObject();row.addProperty("path",path);row.addProperty("kind",json?"json":"mcfunction");row.addProperty("oldByteSha256",hash(text));
            JsonElement parsed=null;boolean parseFail=false;
            if(json)try{parsed=JsonParser.parseString(text);}catch(RuntimeException e){parseFail=true;parseFailures++;}
            String kind=json?"convertJson":"convertFunction";
            String result;
            try{result=converted(ExternalPackCompatibility.class,kind,text);}catch(Exception e){row.addProperty("failure",e.getCause()==null?e.getClass().getName():e.getCause().getClass().getName());entries.add(row);continue;}
            if(json){String diagnostic=converted(InstrumentedCompatibility.class,kind,text);if(!diagnostic.equals(result))diagnosticDisagreements++;}
            else functionFamilies(text);
            row.addProperty("changed",!text.equals(result));row.addProperty("targetByteSha256",hash(result));row.addProperty("parseFailure",parseFail);
            if(!text.equals(result)){if(json)changedJson++;else changedFn++;}
            boolean semanticChanged=false;var paths=new ArrayList<String>();
            if(json&&!parseFail){JsonElement converted=JsonParser.parseString(result);semanticChanged=!parsed.equals(converted);diff(parsed,converted,"",paths);}
            row.addProperty("semanticChanged",semanticChanged);JsonArray pa=new JsonArray();paths.forEach(pa::add);row.add("changedJsonPointers",pa);
            String second=converted(ExternalPackCompatibility.class,kind,result);row.addProperty("idempotent",second.equals(result));if(!second.equals(result))idempotenceFailures++;
            boolean guardedChat=fn&&(path.startsWith("data/bacap_rewards/function/msg/")||path.startsWith("data/bacap_rewards/function/")&&path.endsWith("/root.mcfunction")||text.contains("advancementssearch highlight"));
            if(guardedChat){chatCandidates++;StringBuilder out=new StringBuilder();String[] lines=text.split("\\R",-1);boolean changed=false;for(int i=0;i<lines.length;i++){boolean lineGuard=path.startsWith("data/bacap_rewards/function/")||path.startsWith("data/blazeandcave/function/")&&lines[i].contains("/advancementssearch highlight ");String modern=lineGuard?LegacyChatText.migrateCommand(lines[i]):lines[i];String diagnostic=lineGuard?InstrumentedChatText.migrateCommand(lines[i]):lines[i];if(!modern.equals(diagnostic))throw new IllegalStateException("Chat diagnostic disagrees");changed|=!modern.equals(lines[i]);if(i>0)out.append('\n');out.append(modern);}if(changed)chatChanged++;row.addProperty("runtimeChatShimChanged",changed);row.addProperty("runtimeChatShimSha256",changed?hash(out.toString()):hash(text));}
            entries.add(row);
          }
        }
        JsonArray families=new JsonArray();for(var e:candidates.entrySet()){JsonObject f=new JsonObject();f.addProperty("family",e.getKey());JsonArray cp=new JsonArray();e.getValue().forEach(cp::add);JsonArray ap=new JsonArray();changes.getOrDefault(e.getKey(),new TreeSet<>()).forEach(ap::add);f.add("candidateFiles",cp);f.add("actuallyChangedFiles",ap);f.addProperty("candidateCount",cp.size());f.addProperty("changedCount",ap.size());f.addProperty("alreadyModernOrNoop",cp.size()-ap.size());families.add(f);}
        for(String family:PF){String name="function_"+family.toLowerCase(Locale.ROOT);if(!candidates.containsKey(name)){JsonObject f=new JsonObject();f.addProperty("family",name);f.add("candidateFiles",new JsonArray());f.add("actuallyChangedFiles",new JsonArray());f.addProperty("candidateCount",0);f.addProperty("changedCount",0);f.addProperty("alreadyModernOrNoop",0);families.add(f);}}
        report.add("entries",entries);report.add("families",families);JsonObject summary=new JsonObject();summary.addProperty("jsonFiles",inputJson);summary.addProperty("functionFiles",inputFn);summary.addProperty("changedJsonFiles",changedJson);summary.addProperty("changedFunctionFiles",changedFn);summary.addProperty("parseFailures",parseFailures);summary.addProperty("idempotenceFailures",idempotenceFailures);summary.addProperty("diagnosticDisagreements",diagnosticDisagreements);summary.addProperty("runtimeChatCandidateFiles",chatCandidates);summary.addProperty("runtimeChatChangedFiles",chatChanged);report.add("summary",summary);
        ExternalPackCompatibility.copyForWorld(source,output.resolveSibling("converted-"+source.getFileName()),ExternalPack.BACAP);
        Files.writeString(output,new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(report)+"\n");
      }
    }
    '''
    
    WRAP_METHODS = {
        'normalizeAdvancementBackground':'background',
        'normalizeEntityType':'entity_type',
        'normalizeEntityPredicateValue':'entity_tags',
        'normalizeFrozenLlamaCarpetNbt':'llama_carpet_nbt',
        'transformItemEnchantmentPredicateArray':'enchantment_predicates',
        'transformContextAwarePredicateValue':'context_aware_predicate',
        'transformDamagePredicateObject':'damage',
        'transformKillingBlowPredicateObject':'killing_blow',
        'transformLegacyDamageTypeObject':'damage_type',
        'transformEntityPredicateObject':'entity_predicate',
        'transformPlayerTypeSpecificObject':'player_type_specific',
        'transformRaiderTypeSpecificObject':'raider_type_specific',
        'transformGenericTypeSpecificObject':'generic_type_specific',
        'transformLightningTypeSpecificObject':'lightning_type_specific',
        'transformLegacyTypeSpecificComponents':'entity_variant_components',
        'transformLegacyEntityStruckValue':'entity_struck',
        'suppressVanillaAnnouncementForBacapRewards':'vanilla_announcement_suppression',
    }
    
    def instrument(source):
        source=source.replace('ExternalPackCompatibility','InstrumentedCompatibility')
        wrappers=[]
        for name,family in WRAP_METHODS.items():
            pat=r'(    private static (?:@NotNull )?(\w+) '+name+r'\(([^\n]*)\) \{)'
            m=re.search(pat,source)
            if not m: raise RuntimeError('Missing expected production method '+name)
            signature,typ,params=m.groups(); args=[p.strip().split()[-1] for p in params.split(',')];first=next(p.strip().split()[-1] for p in params.split(',') if re.search(r'Json(?:Element|Object|Array)\b',p)) if typ!='boolean' else args[0]
            source=source[:m.start()]+signature.replace(name+'(',name+'_impl(')+source[m.end():]
            start=f'    private static {typ} {name}({params}) {{\n'
            call=f'{name}_impl({", ".join(args)})'
            if typ=='boolean':
                body=f'        boolean r={call}; B3ConverterProbe.trace("json_{family}",r); return r;\n'
            else:
                before=f'        JsonElement before={first}.deepCopy();\n'
                result='r.element()' if typ=='TransformResult' else 'r'
                body=before+f'        {typ} r={call}; B3ConverterProbe.trace("json_{family}",!before.equals({result})); return r;\n'
            wrappers.append(start+body+'    }\n')
        # Chain function is overloaded; wrap only the JsonElement variant.
        needle='private static @NotNull JsonElement rewriteChainIds(@NotNull JsonElement element) {'
        assert needle in source
        source=source.replace(needle,needle.replace('rewriteChainIds(','rewriteChainIds_impl('))
        wrappers.append('    private static JsonElement rewriteChainIds(JsonElement element) { JsonElement before=element.deepCopy(); JsonElement r=rewriteChainIds_impl(element); B3ConverterProbe.trace("json_chain_id",!before.equals(r)); return r; }\n')
        return source[:source.rfind('}')]+''.join(wrappers)+'}\n'
    
    def sha(data):return hashlib.sha256(data).hexdigest()
    
    def run_probe(repo,old,target,temp,jdk=None):
        repo=Path(repo).resolve();temp=Path(temp).resolve();temp.mkdir(parents=True,exist_ok=True)
        if not temp.is_relative_to((repo/'build/tmp').resolve()):raise ValueError('Probe output must remain beneath ignored build/tmp')
        java=Path(jdk)/'bin/java.exe' if jdk else Path(shutil.which('java'))
        javac=java.with_name('javac.exe')
        modules=Path.home()/'.gradle/caches/modules-2/files-2.1'
        # Pick one cached binary per artifact and prefer highest numeric version.
        jars=[]
        for group in sorted(modules.iterdir()):
            if not group.is_dir():continue
            for artifact in sorted(group.iterdir()):
                if not artifact.is_dir():continue
                available=[p for p in artifact.rglob('*.jar') if not p.name.endswith(('-sources.jar','-javadoc.jar'))]
                if available:
                    def key(p): return tuple(int(x) if x.isdigit() else -1 for x in re.split('[.-]',p.relative_to(artifact).parts[0])),p.name
                    jars.append(max(available,key=key))
        minecraft=sorted((repo/'.gradle/loom-cache/minecraftMaven').rglob('*-26.2.jar'))
        if not minecraft:raise RuntimeError('No existing mapped 26.2 Minecraft jar')
        mc=next((p for p in minecraft if '84afe0508c' in str(p)),minecraft[0])
        src=temp/'probe_src';classes=temp/'probe_classes';src.mkdir(exist_ok=True);classes.mkdir(exist_ok=True)
        java_package=Path('com/diskree/achievetodo/client');p=src/java_package;p.mkdir(parents=True,exist_ok=True)
        evidence=[]
        for name in ['ExternalPackCompatibility','LegacyItemText','LegacyChatText']:
            path=repo/'src/main/java'/java_package/(name+'.java');data=path.read_bytes();(p/(name+'.java')).write_bytes(data)
            evidence.append({'path':path.relative_to(repo).as_posix(),'sourceSha256':sha(data),'copiedSourceSha256':sha((p/(name+'.java')).read_bytes()),'byteIdentical':True})
        converted=instrument((p/'ExternalPackCompatibility.java').read_text(encoding='utf-8'))
        (p/'InstrumentedCompatibility.java').write_text(converted,encoding='utf-8')
        item=(p/'LegacyItemText.java').read_text(encoding='utf-8').replace('LegacyItemText','InstrumentedItemText')
        item=item.replace('Tag value = TagParser.create(NbtOps.INSTANCE).parseFully(raw);','B3ConverterProbe.trace("item_snbt_parse", false);\n                Tag value = TagParser.create(NbtOps.INSTANCE).parseFully(raw);')
        item=item.replace('} catch (Exception ignored) {','} catch (Exception ignored) { B3ConverterProbe.trace("item_snbt_parse_failure", true);')
        (p/'InstrumentedItemText.java').write_text(item,encoding='utf-8')
        chat=(p/'LegacyChatText.java').read_text(encoding='utf-8').replace('LegacyChatText','InstrumentedChatText')
        chat=chat.replace('JsonElement component = JsonParser.parseString(raw);','B3ConverterProbe.trace("runtime_chat_json_parse", false);\n            JsonElement component = JsonParser.parseString(raw);')
        chat=chat.replace('catch (RuntimeException ignored) { return command; }','catch (RuntimeException ignored) { B3ConverterProbe.trace("runtime_chat_json_parse_failure", true); return command; }')
        (p/'InstrumentedChatText.java').write_text(chat,encoding='utf-8')
        (p/'B3ConverterProbe.java').write_text(JAVA_PROBE,encoding='utf-8')
        stubs={
          'com/diskree/achievetodo/client/ExternalPack.java':'package com.diskree.achievetodo.client; public enum ExternalPack { BACAP; public String getFileName(){return "bacap.zip";} public String getSha1(){return "45b8bb0076bbf5b92fde7dc9590c6686937abbc0";} public static ExternalPack mapFromFileName(String s){return "bacap.zip".equals(s)?BACAP:null;} }',
          'com/diskree/achievetodo/client/Utils.java':'package com.diskree.achievetodo.client; import java.nio.file.*; import java.security.*; import java.util.*; public class Utils { public static String calculateSHA1(Path p){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(p)));}catch(Exception e){return "";}} }',
          'com/diskree/achievetodo/server/Constants.java':'package com.diskree.achievetodo.server; public class Constants { public static class NbtKey { public static final String LEVEL_CONFIG_NAME="achievetodoConfigName";} }',
          'org/jetbrains/annotations/NotNull.java':'package org.jetbrains.annotations; import java.lang.annotation.*; @Target({ElementType.TYPE_USE,ElementType.METHOD,ElementType.PARAMETER}) public @interface NotNull {}',
        }
        for path,text in stubs.items():dest=src/path;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(text,encoding='utf-8')
        # The Windows java launcher decodes @argfile paths using a native charset;
        # use ASCII workspace copies so a Cyrillic home path cannot lose libraries.
        libraries=temp/'probe_libraries';libraries.mkdir(exist_ok=True)
        needed={'gson','datafixerupper','brigadier','fastutil','guava','failureaccess','slf4j-api','log4j-api','log4j-core','commons-lang3','commons-io','joml','authlib','logging'}
        selected=[]
        for jar in jars:
            if jar.parent.parent.parent.name in needed or jar.parent.parent.parent.name.startswith('netty-'):
                dest=libraries/jar.name
                if not dest.exists() or sha(dest.read_bytes())!=sha(jar.read_bytes()):shutil.copyfile(jar,dest)
                selected.append(dest)
        cp=os.pathsep.join(str(p) for p in [classes,mc]+selected+[repo/'src/main/resources'])
        def argquote(s):return '"'+str(s).replace('\\','/').replace('"','\\"')+'"'
        args=temp/'javac.args';args.write_text('\n'.join(['-encoding','UTF-8','-cp',argquote(cp),'-d',argquote(classes)]+[argquote(p) for p in sorted(src.rglob('*.java'))]),encoding='utf-8')
        proc=subprocess.run([str(javac),'@'+str(args)],cwd=temp,capture_output=True,text=True,encoding='utf-8',errors='replace')
        if proc.returncode:raise RuntimeError(proc.stdout+proc.stderr)
        results={}
        for label,archive in [('old',Path(old)),('target',Path(target))]:
            source=temp/(label+'-input.zip');shutil.copyfile(archive,source)
            output=temp/(label+'_converter_probe.json')
            args=temp/(label+'_java.args');args.write_text('\n'.join(['-cp',argquote(cp),'com.diskree.achievetodo.client.B3ConverterProbe',argquote(source),argquote(output)]),encoding='utf-8')
            proc=subprocess.run([str(java),'@'+str(args)],cwd=temp,capture_output=True,text=True,encoding='utf-8',errors='replace')
            if proc.returncode:raise RuntimeError(proc.stdout+proc.stderr)
            result=json.loads(output.read_text(encoding='utf-8'));result['archiveSha256']=sha(archive.read_bytes())
            rows=result['entries'];summary=result['summary']
            summary['semanticChangedJsonFiles']=sum(r.get('semanticChanged',False) for r in rows)
            summary['serializationOnlyJsonFiles']=sum(r.get('changed',False) and r['kind']=='json' and not r.get('semanticChanged',False) for r in rows)
            summary['conversionExceptions']=sum('failure' in r for r in rows)
            with zipfile.ZipFile(output.with_name('converted-'+source.name)) as converted_zip:
                mismatches=[r['path'] for r in rows if 'failure' not in r and sha(converted_zip.read(r['path']))!=r['targetByteSha256']]
            summary['actualWorldCopyPayloadMismatches']=len(mismatches)
            summary['actualWorldCopyPayloadMismatchPaths']=mismatches
            known={'json_'+name for name in WRAP_METHODS.values()}|{'json_chain_id','function_chain_id','function_daytime_query','function_legacy_item_text','function_gamerule','item_snbt_parse_failure','runtime_chat_json_parse_failure'}
            existing={f['family'] for f in result['families']}
            for name in sorted(known-existing):result['families'].append({'family':name,'candidateFiles':[],'actuallyChangedFiles':[],'candidateCount':0,'changedCount':0,'alreadyModernOrNoop':0})
            result['families'].sort(key=lambda r:r['family'])
            summary['itemSnbtParseFailureFiles']=next(f['candidateCount'] for f in result['families'] if f['family']=='item_snbt_parse_failure')
            summary['runtimeChatJsonParseFailureFiles']=next(f['candidateCount'] for f in result['families'] if f['family']=='runtime_chat_json_parse_failure')
            for f in result['families']:
                diagnostic=f['family'].startswith(('item_snbt_','runtime_chat_json_'))
                f['diagnosticOnly']=diagnostic
                f['candidateDefinition']='Published parser invoked within file; failure families record swallowed exceptions.' if diagnostic else ('Runtime method invoked within file; JSON parent counts include nested adapter effects.' if f['family'].startswith('json_') else 'Published regular expression/assignment/command token matched a line.')
                f['possibleObsoleteConversion']=not diagnostic and f['changedCount']==0
                f['parseFailureCount']=summary['parseFailures']
                f['possibleNewUnsupportedConstruct']='No codec or command execution performed; new constructs need static review.'
            results[label]=result
        evidence={'actualPublishedSources':evidence,'instrumentedSourceSha256':sha(converted.encode()),'harnessSha256':sha(JAVA_PROBE.encode()),'minecraftJar':mc.relative_to(repo).as_posix(),'minecraftJarSha256':sha(mc.read_bytes()),'javaVersion':subprocess.run([str(java),'-version'],capture_output=True,text=True).stderr.strip(),'cachedLibraryIdentities':[{'filename':p.name,'sha256':sha(p.read_bytes())} for p in sorted(selected)],'stubbedClasses':sorted(stubs),'limitations':['NBT/JSON transformation uses real cached Minecraft and Gson classes. ExternalPack, Utils and Constants metadata only are stubs.','Instrumented copy is diagnostic only; unchanged published converter is authoritative and output equality is checked per JSON entry.','JSON method-family counts are inclusive of nested child adapter effects, not mutually exclusive additive counts.','Candidate count for JSON adapters means runtime method invocation, not proof of an obsolete input feature.','Minecraft command/codec validity is not executed; unsupported-construct detection requires static review.','Marker emitted in ignored target copy still names historical source pin; it is not an accepted production target pin.','Marker properties include generated timestamp and are excluded from deterministic evidence.']}
        evidence['diagnosticItemSourceSha256']=sha(item.encode())
        evidence['diagnosticChatSourceSha256']=sha(chat.encode())
        evidence['stubSourceSha256']={path:sha(text.encode()) for path,text in sorted(stubs.items())}
        results['methodology']=evidence
        output=temp/'converter_analysis_raw.json';output.write_text(json.dumps(results,sort_keys=True,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        return results
    
    def analyze_converter(old_path,target_path,repo_root,temp_root):
        """Self-contained deterministic metadata API for the durable B3 analyzer."""
        result=run_probe(repo_root,old_path,target_path,temp_root)
        result['target']['reviewFindings']=[
          {'type':'ALREADY_MODERN_SERIALIZATION_ONLY','paths':[r['path'] for r in result['target']['entries'] if r.get('changed',False) and r['kind']=='json' and not r.get('semanticChanged',False)],'disposition':'CONVERTER_REVIEW_REQUIRED','reason':'Modern retained entity/damage fields set internal changed=true even when parsed structures remain identical.'},
          {'type':'ENCHANTMENT_SINGLETON_ARRAY_REWRITE','paths':next(f['actuallyChangedFiles'] for f in result['target']['families'] if f['family']=='json_enchantment_predicates'),'disposition':'CONVERTER_REVIEW_REQUIRED','reason':'Official target still has scalar direct enchantment selectors; published adapter changes structure. Necessity/equivalence is not inferred from matching text.'},
          {'type':'UNBREAKABLE_TOOLTIP_REWRITE','paths':next(f['actuallyChangedFiles'] for f in result['target']['families'] if f['family']=='function_unbreakable_with_tooltip'),'disposition':'CONVERTER_REVIEW_REQUIRED','reason':'Published converter removes legacy show_in_tooltip field on target trophy commands; parser/tooltip intent needs B4 review.'},
          {'type':'BACAP_ANNOUNCEMENT_POLICY','paths':next(f['actuallyChangedFiles'] for f in result['target']['families'] if f['family']=='json_vanilla_announcement_suppression'),'disposition':'CONVERTER_REVIEW_REQUIRED','reason':'1244 target advancement displays with bacap_rewards hooks receive announce_to_chat=false; this is ATD message policy, not a Minecraft schema port.'},
          {'type':'UNSUPPORTED_CONSTRUCT_ASSESSMENT','paths':[],'disposition':'CONVERTER_REVIEW_REQUIRED','reason':'No JSON/command conversion exceptions occurred. Native 26.2 command/codec acceptance and new trigger/component coverage are not certified by an offline text conversion.'},
        ]
        result['currentMarker']='compat_26_2_r16'
        result['markerChanged']=False
        return result
    return analyze_converter(old_path,target_path,repo_root,temp_root)



def structural_pointers(a,b,location=""):
 if a==b:return []
 if isinstance(a,dict) and isinstance(b,dict):
  return [p for k in sorted(a.keys()|b.keys()) for p in structural_pointers(a.get(k,MISSING),b.get(k,MISSING),location+"/"+k)]
 if isinstance(a,list) and isinstance(b,list) and len(a)==len(b):
  return [p for i,(x,y) in enumerate(zip(a,b)) for p in structural_pointers(x,y,location+"/"+str(i))]
 return [location or "/"]
def tag_members(v):
 return [{"id":x.get("id"),"required":x.get("required",True)} if isinstance(x,dict) else {"id":x,"required":True} for x in v.get("values",[])]
def enrich_json_resource(row,old,new):
 a=old["resources"].get(row["path"]);b=new["resources"].get(row["path"])
 av=a["json"] if a else {};bv=b["json"] if b else {}
 result={**row,"changedJsonPointers":structural_pointers(av,bv)}
 if row["resourceType"]=="tags":
  am=tag_members(av);bm=tag_members(bv)
  aset={canonical_json(x) for x in am};bset={canonical_json(x) for x in bm}
  result.update(oldMembers=am,targetMembers=bm,addedMembers=[json.loads(x) for x in sorted(bset-aset)],removedMembers=[json.loads(x) for x in sorted(aset-bset)],membershipOrderChanged=am!=bm,replaceChanged=av.get("replace",False)!=bv.get("replace",False))
 return result

def dialog_command_references(pack):
 rows=[]
 def visit(v,path,loc):
  if isinstance(v,dict):
   for k,x in sorted(v.items()):
    if k in {"command","template"} and isinstance(x,str):
     command=x.lstrip("/")
     mask=mask_quoted(command)
     for m in CALL_RE.finditer(mask):
      raw=m[0].split()[-1]
      rows.append({"path":path,"location":loc+"/"+k,"callee":rid(raw.lstrip("#")),"functionTag":raw.startswith("#"),"argumentKeys":sorted(literal_arguments(command)),"argumentFingerprint":semhash(literal_arguments(command)),"kind":"DEFERRED_DIALOG_ACTION","b4Disposition":["SETUP_SCOREBOARD_REVIEW_REQUIRED"]})
    visit(x,path,loc+"/"+k)
  elif isinstance(v,list):
   for i,x in enumerate(v):visit(x,path,loc+"/"+str(i))
 for p,r in sorted(pack["resources"].items()):
  if r["resourceType"]=="dialog":visit(r["json"],p,"")
 return rows

def run(args):
 repo=Path(args.repo_root).resolve();os.chdir(repo)
 old_path=Path(args.old_zip);target_path=Path(args.target_zip);out=Path(args.output_dir).resolve();temp=Path(args.temp_dir).resolve()
 assert temp.is_relative_to(repo/"build/tmp"),"Temporary conversion must remain under ignored repository build/tmp"
 assert out.is_relative_to(repo/"build/tmp") or out==repo/"reference/phase_b","Analysis output must stay in B3 evidence or ignored temp"
 outputs=["B3_BACAP_SEMANTIC_DIFF.md","b3_advancement_diff.json","b3_resource_graph_diff.json","b3_atd_impact_matrix.json"]
 assert not any((out/n).exists() for n in outputs),"Refusing to overwrite existing reports"
 assert subprocess.check_output(["git","check-ignore",str(temp.relative_to(repo))]).strip(),"Temporary area must be ignored"
 b1inv=read_json(args.b1_inventory);b2=read_json(args.b2_map);b2occ=read_json(args.b2_occurrences);pin=read_json(Path(args.b1_inventory).parent/"b1_official_bacap_pin.json")
 for row in b2occ["scanCoverage"]["scannedFiles"]:assert sha((repo/row["path"]).read_bytes())==row["sha256"],("B2 production input drift",row["path"])
 baseline={"branch":subprocess.check_output(["git","branch","--show-current"]).decode().strip(),"commit":subprocess.check_output(["git","rev-parse","HEAD"]).decode().strip(),"tagCommit":subprocess.check_output(["git","rev-parse","v0.1.5.4^{commit}"]).decode().strip()}
 assert baseline=={"branch":"phase-b-bacap-26.2","commit":"b261b02cd03b4aeae2835c63f982c9aa53c46eed","tagCommit":"b261b02cd03b4aeae2835c63f982c9aa53c46eed"}

 protected_records={p.relative_to(repo).as_posix():sha(p.read_bytes()) for p in sorted((repo/"reference/phase_b").iterdir()) if p.name in {"OPERATING_CONTRACT.md","b0_baseline.json","B1_OFFICIAL_BACAP_PIN.md","b1_official_bacap_pin.json","b1_target_inventory.json","B2_ATD_INTEGRATION_MAP.md","b2_atd_integration_map.json","b2_bacap_occurrences.json"}}
 assert len(protected_records)==8
 old=load_pack(old_path,"old",OLD_HASH);new=load_pack(target_path,"target",TARGET_HASH)
 assert len(old["adv"])==1229 and len(new["adv"])==1332
 # B1 target per-file inventory is independently checked below.
 ot=tree(old);nt=tree(new);assert not nt["cycles"],"Target advancement cycle"
 assert not [x for x in nt["missingParents"] if x["missingParent"].split(":")[0] in LOCAL_NS],"Target missing internal parent"
 common=sorted(old["adv"].keys()&new["adv"].keys());removed=sorted(old["adv"].keys()-new["adv"].keys());added=sorted(new["adv"].keys()-old["adv"].keys())
 atdref=collections.defaultdict(list)
 for i,x in enumerate(b2occ["occurrences"],1):
  if x["productionRelevant"] and x["token"] in (old["adv"].keys()|new["adv"].keys()):atdref[x["token"]].append({"occurrenceNumber":i,"path":x["path"],"line":x["line"],"kind":x["kind"],"classification":x["classification"]})
 official=official_evidence(repo,pin);renames=rename_analysis(old,new,official)
 for pair in renames["pairs"]:
  a=ot["nodes"][pair["oldId"]];b=nt["nodes"][pair["targetId"]]
  pair["treeFacts"]={"old":a,"target":b,"changed":{k:a[k]!=b[k] for k in ["parent","root","category","depth","pathComponents"]}}
  pair["rewardFunctionIds"]={"old":old["adv"][pair["oldId"]]["json"].get("rewards",{}).get("function"),"target":new["adv"][pair["targetId"]]["json"].get("rewards",{}).get("function")}
 rby=collections.defaultdict(list)
 for r in renames["pairs"]:rby[r["oldId"]].append(r);rby[r["targetId"]].append(r)
 advrecords=[]
 for aid in sorted(old["adv"].keys()|new["adv"].keys()):
  a=old["adv"].get(aid);b=new["adv"].get(aid);aa=ot["nodes"].get(aid);bb=nt["nodes"].get(aid)
  cls=sameclass(a,b) if a and b else ("OLD_ONLY" if a else "TARGET_ONLY")
  tc={k:bool(aa and bb and aa[k]!=bb[k]) for k in ["parent","root","category","pathComponents","depth"]}
  tc["treeMoved"]=tc["parent"] or tc["root"]
  fields=field_diff(a,b,tag_map(new)) if cls=="SEMANTICALLY_CHANGED" else None
  dis=set()
  if cls in ["OLD_ONLY","TARGET_ONLY","SEMANTICALLY_CHANGED"]:dis.add("TEST_SUCCESSOR_REQUIRED")
  if a and not b and atdref[aid]:dis.update(["OVERRIDE_RESOURCE_UPDATE_REQUIRED","SEARCH_LINK_UPDATE_REQUIRED"])
  if cls=="TARGET_ONLY" and b["canonical"]:dis.update(["OVERRIDE_RESOURCE_UPDATE_REQUIRED","LOCALIZATION_REVIEW_REQUIRED"])
  if fields and fields["display"]:dis.add("LOCALIZATION_REVIEW_REQUIRED")
  if any(tc.values()) and atdref[aid]:dis.add("TREE_GUI_REVIEW_REQUIRED")
  advrecords.append({"resourceId":aid,"oldPresent":bool(a),"targetPresent":bool(b),"oldCanonical":a["canonical"] if a else False,"targetCanonical":b["canonical"] if b else False,"oldByteSha256":a["byteSha256"] if a else None,"targetByteSha256":b["byteSha256"] if b else None,"oldSemanticSha256":a["semanticSha256"] if a else None,"targetSemanticSha256":b["semanticSha256"] if b else None,"classification":cls,"fieldChanges":fields,"oldParent":aa["parent"] if aa else None,"targetParent":bb["parent"] if bb else None,"oldRoot":aa["root"] if aa else None,"targetRoot":bb["root"] if bb else None,"oldTree":aa,"targetTree":bb,"treeChanges":tc,"renameCandidateInfo":{"classification":next((r["classification"] for r in rby[aid]),"NO_MATCH"),"pairs":rby[aid]} if not (a and b) else None,"atdReferences":atdref[aid],"b4Disposition":sorted(dis or {"NO_PRODUCT_CHANGE"})})
 armap={x["resourceId"]:x for x in advrecords}
 canold={k for k,x in old["adv"].items() if x["canonical"]};cannew={k for k,x in new["adv"].items() if x["canonical"]}
 transitions={"CANONICAL_IN_BOTH":sorted(canold&cannew),"BECAME_CANONICAL":sorted((cannew-canold)&set(common)),"CEASED_CANONICAL":sorted((canold-cannew)&set(common)),"OLD_CANONICAL_ID_REMOVED":sorted(canold&set(removed)),"NEW_CANONICAL_ID_ADDED":sorted(cannew&set(added))}
 assert len(canold)==1152 and len(cannew)==1242
 assert 1152-len(transitions["OLD_CANONICAL_ID_REMOVED"])-len(transitions["CEASED_CANONICAL"])+len(transitions["BECAME_CANONICAL"])+len(transitions["NEW_CANONICAL_ID_ADDED"])==1242
 setcounts={"common":len(common),"oldOnly":len(removed),"targetOnly":len(added),**summarize_rows([x for x in advrecords if x["classification"] not in ["OLD_ONLY","TARGET_ONLY"]],"classification")}
 assert len(common)+len(removed)==1229 and len(common)+len(added)==1332
 fieldcounts={k:sum(bool(x["fieldChanges"] and x["fieldChanges"][k]) for x in advrecords) for k in ["parent","display","criteria","requirements","rewards","sends_telemetry_event","otherTopLevelFields"]}
 displaycounts={k:sum(bool(x["fieldChanges"] and x["fieldChanges"]["displayFields"][k]) for x in advrecords) for k in ["title","description","icon","frame","background","hidden","show_toast","announce_to_chat"]}
 logicchanges=sum(bool(x["fieldChanges"] and x["fieldChanges"]["requirementsDetails"]["completionGroupingSemanticsChanged"]) for x in advrecords)
 pathsens=[(i,x) for i,x in enumerate(b2occ["occurrences"],1) if x["productionRelevant"] and x["classification"]=="PATH_SENSITIVE"]
 psids={x["token"].lstrip("#") for _,x in pathsens if ":" in x["token"]}
 og=function_graph(old,psids);ng=function_graph(new,psids)
 resource_rows,resource_counts,relocations=resource_diff(old,new);resmap={r["path"]:r for r in resource_rows}
 newrefs=references(new,ng,old);oldrefs=references(old,og,old)
 contracts=contract_probe(str(old_path),str(target_path),args.b2_map)
 converter=converter_probe(str(old_path),str(target_path),str(repo),str(temp/"converter"))
 oldtriggers={c.get("trigger") for a in old["adv"].values() for c in a["json"].get("criteria",{}).values()}
 newtriggers={c.get("trigger") for a in new["adv"].values() for c in a["json"].get("criteria",{}).values()}
 converter["target"]["staticInputFeatureReview"]={"newTriggerIdentifiers":sorted(newtriggers-oldtriggers),"newTriggerFiles":[{"resourceId":aid,"path":a["path"],"newTriggers":sorted({c.get("trigger") for c in a["json"].get("criteria",{}).values()}-oldtriggers)} for aid,a in sorted(new["adv"].items()) if any(c.get("trigger") not in oldtriggers for c in a["json"].get("criteria",{}).values())],"newDialogPaths":sorted(p for p in new["resources"] if new["resources"][p]["resourceType"]=="dialog"),"macroCommandLines":len(ng["macroCommandLines"]),"disposition":"CONVERTER_REVIEW_REQUIRED","assessment":"New engine features passed through offline adapter without exceptions; native codec/command validity remains B8/B10, unsupported semantics are not claimed from text alone."}
 impact=build_impact(repo,b2,b2occ,armap,old,new,ot,nt,og,ng,resmap,renames,pathsens,contracts,converter)
 missingpred=[r for r in newrefs if r["resolution"]=="MISSING_PACK_LOCAL_PREDICATE"]
 missingtags=[r for r in newrefs if r["resolution"] in {"PACK_NAMESPACE_TAG_NOT_DEFINED_REVIEW","REMOVED_OLD_PACK_TAG_REGISTRY_EXISTENCE_REVIEW"}]
 if missingpred or missingtags:impact["b4ReviewQueue"].append({"id":"B4-PREDICATE-TAG-CONTRACTS","b4Disposition":["UNKNOWN_REQUIRES_B4"],"facts":{"predicateReferences":missingpred,"registryTagReferences":missingtags},"review":"Review truly missing pack predicates or formerly pack-owned tags; vanilla registry tags are not datapack dangling by default.","implementationPerformed":False})
 identities={"analysisTool":{"path":"tools/phase_b/b3_bacap_semantic_diff.py","sha256":sha(Path(__file__).read_bytes())},"publishedBaseline":baseline,"protectedB0B1B2Hashes":protected_records,"old":{"version":"1.18.1","archive":old_path.as_posix(),"sha256":old["sha256"],"size":old["size"]},"target":{"version":"1.21","versionId":"Y2zZ5eSs","archive":target_path.as_posix(),"sha256":new["sha256"],"size":new["size"]},"inputRecords":[{"path":Path(p).as_posix(),"sha256":sha(Path(p).read_bytes())} for p in [args.b1_inventory,args.b2_map,args.b2_occurrences]],"copyright":"IDs/hashes/paths/structural metadata only; raw archives and upstream resource bodies excluded.","normalization":"Sort JSON object keys, preserve all arrays; separately canonicalize logical AND-of-OR requirements; function text normalized only LF/trailing line whitespace, never claimed semantic equivalence.","legacyParseDiagnostics":old["diagnostics"]+new["diagnostics"]}
 graph={"resourcePathDiff":resource_rows,"resourceCounts":resource_counts,"packMetadataDiff":{"old":old["packMetadata"],"target":new["packMetadata"],"structuralFieldsChanged":old["packMetadata"]["packFields"]!=new["packMetadata"]["packFields"],"b4Disposition":["OVERRIDE_RESOURCE_UPDATE_REQUIRED"]},"relocationCandidates":relocations,"functionCallGraphSummary":graph_diff(og,ng),"functionTagChanges":[enrich_json_resource(r,old,new) for r in resource_rows if r["resourceType"]=="tags" and r["registryType"]=="function"],"predicateChanges":[enrich_json_resource(r,old,new) for r in resource_rows if r["resourceType"]=="predicate"],"tagChanges":[enrich_json_resource(r,old,new) for r in resource_rows if r["resourceType"]=="tags" and r["registryType"]!="function"],"danglingTargetReferences":{"staticFunctionCallees":[e for e in ng["edges"] if e["resolution"]=="MISSING_STATIC_CALLEE"],"conditionalMacroMissingResources":[e for e in ng["edges"] if e["resolution"]=="CONDITIONAL_MACRO_RESOURCE_ABSENT_REVIEW"],"fanpackExtensionHooks":[e for e in ng["edges"] if e["resolution"]=="EXTERNAL_FANPACK_EXTENSION_HOOK_REVIEW_REQUIRED"],"predicates":[r for r in newrefs if r["resolution"]=="MISSING_PACK_LOCAL_PREDICATE"],"packNamespaceRegistryTags":[r for r in newrefs if r["resolution"]=="PACK_NAMESPACE_TAG_NOT_DEFINED_REVIEW"],"removedOldPackTagsRegistryReview":[r for r in newrefs if r["resolution"]=="REMOVED_OLD_PACK_TAG_REGISTRY_EXISTENCE_REVIEW"]},"predicateAndRegistryTagReferences":{"old":oldrefs,"target":newrefs},"dialogCommandReferences":dialog_command_references(new),"scoreboardContracts":contracts,"converterAnalysis":converter}
 base={"schemaVersion":1,"phase":"B","stage":"B3","status":"PHASE_B_B3_DIFFED","identities":identities}
 adv={**base,"setCounts":setcounts,"idSets":{"COMMON_ID":common,"OLD_ONLY":removed,"TARGET_ONLY":added},"canonicalRule":"display present AND hidden != true AND ID != blazeandcave:bacap/root","canonicalTransitions":transitions,"canonicalCounts":{k:len(v) for k,v in transitions.items()}|{"old":1152,"target":1242,"netOnly":90},"fieldChangeCounts":fieldcounts,"displayFieldChangeCounts":displaycounts,"completionGroupingSemanticChanges":logicchanges,"treeGraph":{"oldMissingParents":ot["missingParents"],"targetMissingParents":nt["missingParents"],"oldCycles":ot["cycles"],"targetCycles":nt["cycles"]},"renameAnalysis":renames,"advancements":advrecords}
 graph={**base,**graph};impact={**base,**impact}
 verify_reconciliation(adv,graph,impact,b1inv,b2,pathsens,new)
 for p,h in protected_records.items():assert sha((repo/p).read_bytes())==h,p
 assert sha(old_path.read_bytes())==OLD_HASH and sha(target_path.read_bytes())==TARGET_HASH
 assert not subprocess.check_output(["git","diff","--raw"]).strip()
 assert not subprocess.check_output(["git","diff","--cached","--raw"]).strip()
 out.mkdir(parents=True,exist_ok=True)
 for filename,obj in zip(outputs[1:],[adv,graph,impact]):write_exclusive(out/filename,obj)
 with (out/outputs[0]).open("x",encoding="utf-8",newline="\n") as f:f.write(markdown_report(adv,graph,impact))
 print(json.dumps({"setCounts":setcounts,"canonicalCounts":adv["canonicalCounts"],"fieldChangeCounts":fieldcounts,"renameSummary":renames["summary"],"impactSummary":impact["summary"],"resourceCounts":resource_counts,"status":"PHASE_B_B3_DIFFED"},sort_keys=True))
def graph_diff(og,ng):
 oldedges={(e["source"],e["callee"],e["functionTag"]) for e in og["edges"] if not e["dynamic"]}
 newedges={(e["source"],e["callee"],e["functionTag"]) for e in ng["edges"] if not e["dynamic"]}
 def side(g):
  missing=[e for e in g["edges"] if e["resolution"]=="MISSING_STATIC_CALLEE"]
  return {"functions":len(g["functions"]),"functionTags":len(g["tags"]),"aggregatedEdges":len(g["edges"]),"callOccurrences":sum(e.get("callSiteOccurrences",1) for e in g["edges"]),"directStaticCallOccurrences":sum(e.get("callSiteOccurrences",1) for e in g["edges"] if not e["macroExpanded"] and not e["dynamic"]),"macroCommandLines":len(g["macroCommandLines"]),"unresolvedDynamicSites":sum(e.get("callSiteOccurrences",1) for e in g["edges"] if e["dynamic"]),"unresolvedDynamicPatterns":sum(e["dynamic"] for e in g["edges"]),"literalMacroArgumentContexts":g["literalMacroContexts"],"resolvedMacroCallOccurrences":sum(e.get("callSiteOccurrences",1) for e in g["edges"] if e["macroExpanded"]),"conditionalMacroAbsentOccurrences":sum(e.get("callSiteOccurrences",1) for e in g["edges"] if e["resolution"]=="CONDITIONAL_MACRO_RESOURCE_ABSENT_REVIEW"),"conditionalMacroAbsentUniqueCallees":len({e["callee"] for e in g["edges"] if e["resolution"]=="CONDITIONAL_MACRO_RESOURCE_ABSENT_REVIEW"}),"reachableNodes":len(g["reachable"]),"missingStaticOccurrences":len(missing),"missingStaticUniqueCallees":sorted({e["callee"] for e in missing}),"fanpackExtensionHooks":sorted({e["callee"] for e in g["edges"] if e["resolution"]=="EXTERNAL_FANPACK_EXTENSION_HOOK_REVIEW_REQUIRED"})}
 return {"old":side(og),"target":side(ng),"newCalleeEdges":[{"source":s,"callee":t,"functionTag":tag} for s,t,tag in sorted(newedges-oldedges)],"removedCalleeEdges":[{"source":s,"callee":t,"functionTag":tag} for s,t,tag in sorted(oldedges-newedges)],"oldGraph":{"roots":og["seeds"],"reachable":og["reachable"],"edges":og["edges"],"macroLines":og["macroCommandLines"]},"targetGraph":{"roots":ng["seeds"],"reachable":ng["reachable"],"edges":ng["edges"],"macroLines":ng["macroCommandLines"]},"limitations":["Static over-approximation ignores execute guards and does not execute commands.","Quoted JSON/English text is masked so embedded chat click commands are not executed-call edges.","Literal macro arguments are bound for callee resolution; storage/entity/block-sourced arguments remain explicit dynamic sites.","Unknown vanilla registry existence is not inferred from datapack absence; fanpack extension hooks remain separate review contracts."]}
def build_impact(repo,b2,b2occ,armap,old,new,ot,nt,og,ng,resmap,renames,pathsens,contracts,converter):
 rewardrows=[];wrapperrows=[];setuprows=[]
 mainpaths=set();allhl=[];parentrefs=[];orderedchildren=[]
 renameby=collections.defaultdict(list)
 for x in renames["pairs"]:renameby[x["oldId"]].append(x)
 for pack in b2["resourcePacks"]["packs"]:
  for f in pack["files"]:
   group=f["functionClass"];path=f["path"];up=f["resourcePath"]
   if group not in {"REWARD_MESSAGES_LOCALIZATION","ROOT_CATEGORY_OR_MILESTONE_REWARD","REWARD_WRAPPER","CONFIGURATION_SETUP","REWARD_OPTION_SETUP","COOPERATIVE_SETUP"}:continue
   rr=resmap.get(up)
   hl=source_highlights(repo,path)
   for aid in hl:
    a=armap.get(aid);allhl.append({"path":path,"advancementId":aid,"oldPresent":bool(a and a["oldPresent"]),"targetPresent":bool(a and a["targetPresent"]),"classification":"UNCHANGED_ID" if a and a["targetPresent"] else ("REMOVED_ADVANCEMENT_ID" if a and a["oldPresent"] else "ATD_OR_COMPANION_ID_NOT_IN_MAIN"),"renameCandidates":renameby.get(aid,[]),"b4Disposition":["SEARCH_LINK_UPDATE_REQUIRED"] if a and a["oldPresent"] and not a["targetPresent"] else (["COMPANION_REVIEW_REQUIRED"] if "bacap_override"!=pack["name"] else ["NO_PRODUCT_CHANGE"])})
   fieldimpact=[]
   for aid in hl:
    a=armap.get(aid)
    if a and a["fieldChanges"] and (a["fieldChanges"]["displayFields"]["title"] or a["fieldChanges"]["displayFields"]["description"]):
     fieldimpact.append({"advancementId":aid,"title":a["fieldChanges"]["text"]["title"],"description":a["fieldChanges"]["text"]["description"]})
   iscomp=pack["name"] in {"bacap_hardcore_override","bacap_terralith_override","bacap_amplified_nether_override","bacap_nullscape_override"}
   row={"path":path,"pack":pack["name"],"upstreamPath":up,"comparisonScope":"MAIN_ARCHIVE_ONLY_COMPANION_UNRESOLVED" if iscomp else "MAIN_ARCHIVE","oldMainPresent":up in old["resources"],"targetMainPresent":up in new["resources"],"upstreamPathClassification":rr["classification"] if rr else "ATD_ONLY_OR_COMPANION_RESOURCE","highlightIds":hl,"removedHighlightIds":[a for a in hl if a in armap and armap[a]["oldPresent"] and not armap[a]["targetPresent"]],"titleDescriptionImpact":fieldimpact,"byteSha256":f["sha256"]}
   if group=="REWARD_MESSAGES_LOCALIZATION":
    if not iscomp:mainpaths.add(up)
    row["b4Disposition"]=["COMPANION_REVIEW_REQUIRED"] if iscomp else (["OVERRIDE_RESOURCE_UPDATE_REQUIRED"] if not row["targetMainPresent"] or fieldimpact or row["removedHighlightIds"] else ["NO_PRODUCT_CHANGE"])
    if row["b4Disposition"]==["NO_PRODUCT_CHANGE"] and rr and rr["classification"]=="UNCHANGED_PATH_CHANGED" and not rr["parsedOrNormalizedIdentical"]:
     row["b4Disposition"]=["UNKNOWN_REQUIRES_B4"]
    if fieldimpact:row["b4Disposition"].append("LOCALIZATION_REVIEW_REQUIRED")
    rewardrows.append(row)
   elif group in {"ROOT_CATEGORY_OR_MILESTONE_REWARD","REWARD_WRAPPER"}:
    row["b4Disposition"]=["COMPANION_REVIEW_REQUIRED"] if iscomp else ["OVERRIDE_RESOURCE_UPDATE_REQUIRED"] if rr and rr["classification"]=="UNCHANGED_PATH_CHANGED" else ["UNKNOWN_REQUIRES_B4"]
    wrapperrows.append(row)
   else:
    row["b4Disposition"]=["SETUP_SCOREBOARD_REVIEW_REQUIRED"]
    setuprows.append(row)
 # Main upstream messages lacking base overlay; keep companion ownership separate.
 missingmsgs=[]
 for p,r in sorted(new["resources"].items()):
  if r["resourceType"]=="function" and r["namespace"]=="bacap_rewards" and r["resourceId"].split(":",1)[1].startswith("msg/") and p not in mainpaths:
   missingmsgs.append({"resourceId":r["resourceId"],"path":p,"oldMainPresent":p in old["resources"],"companionOverridePaths":[x["path"] for x in rewardrows if x["pack"]!="bacap_override" and x["upstreamPath"]==p],"b4Disposition":["OVERRIDE_RESOURCE_UPDATE_REQUIRED","LOCALIZATION_REVIEW_REQUIRED"]})
 trackers=[]
 for binding in b2["productionCouplings"]["trackers"]:
  aid=binding["advancementId"];a=armap.get(aid);fc=a["fieldChanges"] if a else None
  objs=binding["scoreboardObjectives"]
  objectivefacts=[]
  for obj in objs:
   objectivefacts.append({"objective":obj,"oldDefined":any(r["objective"]==obj and r["old"]["definitions"] for r in contracts["objectiveContracts"]),"removedDefinition":obj in contracts["removedDefinedObjectives"],"newReplacementCandidates":[r for r in contracts["likelyObjectiveRenameCandidates"] if obj==r["old"]]})
  sem=bool(a and a["classification"]=="SEMANTICALLY_CHANGED")
  relevant=bool(fc and (fc["criteria"] or fc["requirements"] or fc["requirementsDetails"]["completionGroupingSemanticsChanged"]))
  special=binding["name"] in {"ON_A_RAIL","HALF_HEART_LIFE"}
  review=not a or not a["targetPresent"] or relevant or any(x["removedDefinition"] for x in objectivefacts) or special
  trackers.append({"binding":binding,"oldPresent":bool(a and a["oldPresent"]),"targetPresent":bool(a and a["targetPresent"]),"sameIdClassification":a["classification"] if a else None,"criterionRequirementsRelevantChange":relevant,"criterionChanges":fc["criteriaDetails"] if fc else None,"requirementsChanges":fc["requirementsDetails"] if fc else None,"objectiveFacts":objectivefacts,"statEntityObjectiveSemantics":"STATIC_CRITERIA_OR_OBJECTIVE_CHANGE_REVIEW" if relevant or any(x["removedDefinition"] for x in objectivefacts) else "NO_STATIC_BOUND_CRITERION_CHANGE_PROVED","specialSemanticReview":special,"b4ReviewRequired":review,"b4Disposition":["TRACKER_REVIEW_REQUIRED"] if review else ["NO_PRODUCT_CHANGE"],"goalRadiusEntitiesChangedByB3":False})
 # Derive exact 3 ordered maps and28children directly from released production.
 source=repo/"src/main/java/com/diskree/achievetodo/injection/mixin/main/PlacedAdvancementMixin.java"
 for m in re.finditer(r'customChildrenOrderMap\.put\("([^"]+)",\s*List\.of\((.*?)\)\)',source.read_text(),re.S):
  parent=m[1];children=re.findall(r'"([^"]+)"',m[2]);parentrefs.append({"parentId":parent,"children":children})
  for aid in children:
   a=armap.get(aid)
   orderedchildren.append({"resourceId":aid,"orderedParentId":parent,"targetPresent":bool(a and a["targetPresent"]),"oldParent":a["oldParent"] if a else None,"targetParent":a["targetParent"] if a else None,"treeChanges":a["treeChanges"] if a else None,"pathDepthAssumptionChanged":bool(a and a["targetTree"] and a["targetTree"]["pathComponents"]!=2),"b4Disposition":["TREE_GUI_REVIEW_REQUIRED"] if not a or not a["targetPresent"] or any(a["treeChanges"].values()) else ["NO_PRODUCT_CHANGE"]})
 allgui=[]
 # Explicit B2 source IDs, including samples/components, preserved as separate scope.
 for aid in b2["productionCouplings"]["explicitBACAPIds"]:
  a=armap.get(aid)
  if not a:
   allgui.append({"resourceId":aid,"oldPresent":False,"targetPresent":False,"status":"NON_ADVANCEMENT_OR_COMPANION_REFERENCE","b4Disposition":["COMPANION_REVIEW_REQUIRED"]});continue
  facts=a["treeChanges"]
  allgui.append({"resourceId":aid,"oldPresent":a["oldPresent"],"targetPresent":a["targetPresent"],"treeChanges":facts,"targetPathDepth":a["targetTree"]["pathComponents"] if a["targetTree"] else None,"b4Disposition":["TREE_GUI_REVIEW_REQUIRED"] if not a["targetPresent"] or any(facts.values()) else ["NO_PRODUCT_CHANGE"]})
 knowncategories={x.lower() for x in b2["searchAndGuiCoupling"]["fixedTabs"]}
 unmatchedcategories=[{"resourceId":a,"category":v["category"],"pathComponents":v["pathComponents"],"rootId":v["root"],"b4Disposition":["TREE_GUI_REVIEW_REQUIRED"]} for a,v in nt["nodes"].items() if v["canonical"] and (v["category"].lower() not in knowncategories or v["pathComponents"]!=2)]
 # B2 HUSBANDRY vs upstream 'animal' is inherited classification, not newly broken.
 oldexceptions={a for a,v in ot["nodes"].items() if v["canonical"] and (v["category"].lower() not in knowncategories or v["pathComponents"]!=2)}
 for row in unmatchedcategories:row["alreadyOutsideCategoryRuleOld"]=row["resourceId"] in oldexceptions
 # Every PATH_SENSITIVE occurrence must retain an explicit accounting record.
 pathaccounts=[]
 for num,x in pathsens:
  token=x["token"];bare=token.lstrip("#");record={"occurrenceNumber":num,"path":x["path"],"line":x["line"],"token":token,"status":None,"b4Disposition":None}
  if x["kind"]=="SCOREBOARD_HOLDER_CONTRACT":
   generic=token.startswith("@") or token.startswith("$(")
   record.update(status="PROVEN_GENERIC_SELECTOR_OR_DYNAMIC_HOLDER" if generic else "MAPPED_SCOREBOARD_CONTRACT",finding="scoreboardContracts",b4Disposition=["NO_PRODUCT_CHANGE"] if generic else ["SETUP_SCOREBOARD_REVIEW_REQUIRED"])
  elif x["kind"]=="CATEGORY_ROOT_HASH_INPUT":
   record.update(status="MAPPED_ROOT_HASH_AND_WRAPPER",finding="rootWrappers",b4Disposition=["OVERRIDE_RESOURCE_UPDATE_REQUIRED"])
  elif x["kind"]=="COMPATIBILITY_TAG_DEFINITION":
   record.update(status="MAPPED_LEGACY_TAG_CONVERTER_REVIEW",finding="compatibilityConverter",b4Disposition=["CONVERTER_REVIEW_REQUIRED"])
  elif x["kind"]=="RESOURCE_DEFINITION":
   f=next((f for p in b2["resourcePacks"]["packs"] for f in p["files"] if f["path"]==x["path"]),None)
   rr=resmap.get(f["resourcePath"]) if f else None
   record.update(status="MAPPED_RESOURCE_PATH_DIFF" if rr else "ATD_ONLY_OR_COMPANION_RESOURCE_REVIEW",finding=f["resourcePath"] if f else "companionSurfaces",b4Disposition=["OVERRIDE_RESOURCE_UPDATE_REQUIRED"] if rr else ["COMPANION_REVIEW_REQUIRED"])
  elif bare in ng["functions"] or bare in ng["tags"]:
   record.update(status="MAPPED_FUNCTION_GRAPH",finding=bare,b4Disposition=["NO_PRODUCT_CHANGE"])
  elif bare in og["functions"] or bare in og["tags"]:
   record.update(status="REMOVED_FUNCTION_OR_TAG_REVIEW",finding=bare,b4Disposition=["OVERRIDE_RESOURCE_UPDATE_REQUIRED"])
  elif token.startswith("#") and any(r["resourceId"]==bare and r["resourceType"]=="tags" and r["registryType"]!="function" for r in resmap.values()):
   record.update(status="MAPPED_REGISTRY_TAG_DIFF",finding=bare,b4Disposition=["CONVERTER_REVIEW_REQUIRED"])
  elif bare.startswith("bac_") and ":" not in bare:
   record.update(status="MAPPED_SCOREBOARD_CONTRACT",finding="scoreboardContracts",b4Disposition=["SETUP_SCOREBOARD_REVIEW_REQUIRED"])
  elif token.startswith("#minecraft:"):
   record.update(status="REGISTRY_TAG_NOT_PACK_PATH",finding=bare,b4Disposition=["CONVERTER_REVIEW_REQUIRED"] if any(r["resourceId"]==bare and r["classification"]=="REMOVED_PATH" for r in resmap.values()) else ["NO_PRODUCT_CHANGE"])
  elif token.startswith("#bacap_fanpacks:"):
   record.update(status="MAPPED_FANPACK_EXTENSION_HOOK",finding="functionCallGraphSummary",b4Disposition=["COMPANION_REVIEW_REQUIRED"])
  elif bare in {"blazeandcave:setup_cooperative_mode","blazeandcave:setup_item_rewards","blazeandcave:setup_experience_rewards","blazeandcave:setup_trophy_rewards"}:
   record.update(status="MAPPED_ATD_SUPPLIED_SETUP_FUNCTION",finding="setupConfiguration",b4Disposition=["SETUP_SCOREBOARD_REVIEW_REQUIRED"])
  elif bare in {"bacap_rewards:exp/end/desolation","bacap_rewards:msg/end/desolation","bacap_rewards:reward/end/desolation"}:
   record.update(status="MAPPED_COMPANION_ONLY_DESOLATION_CONTRACT",finding="companionSurfaces",b4Disposition=["COMPANION_REVIEW_REQUIRED"])
  elif bare.endswith(":") or bare in {"bacap_rewards","bacap_fanpacks","blazeandcave"}:
   record.update(status="PROVEN_GENERIC_NAMESPACE_PREFIX",finding="compatibilityConverter",b4Disposition=["NO_PRODUCT_CHANGE"])
  else:
   record.update(status="EXPLICIT_B4_REVIEW_QUEUE",finding="PATH_SENSITIVE:"+bare,b4Disposition=["UNKNOWN_REQUIRES_B4"])
  pathaccounts.append(record)
 # Translation-key inventories only: no string values or source research.
 def keys(pack):
  result=collections.defaultdict(set)
  def visit(v,path):
   if isinstance(v,dict):
    if isinstance(v.get("translate"),str):result[v["translate"]].add(path)
    for k,y in v.items():visit(y,path)
   elif isinstance(v,list):
    for y in v:visit(y,path)
  for a in pack["adv"].values():visit(a["json"],a["path"])
  for r in pack["resources"].values():
   if "json" in r:visit(r["json"],r["path"])
   elif r["resourceType"]=="function":
    for m in re.finditer(r'"translate"\s*:\s*"((?:\\.|[^"\\])*)"',r["text"]):
     try:k=json.loads('"'+m[1]+'"')
     except json.JSONDecodeError:continue
     result[k].add(r["path"])
  return result
 generatedlinks=[]
 for path in sorted((repo/"src/main/generated").rglob("*.mcfunction")):
  for aid in source_highlights(repo,path.relative_to(repo).as_posix()):
   generatedlinks.append({"path":path.relative_to(repo).as_posix(),"advancementId":aid,"classification":"ATD_INTERNAL_ID","b4Disposition":["NO_PRODUCT_CHANGE"]})
 oldkeys=keys(old);newkeys=keys(new)
 runtimekeys=set(read_json(repo/"src/main/resources/assets/minecraft/lang/ru_ru.json"))|set(read_json(repo/"src/main/resources/assets/achievetodo/lang/ru_ru.json"))
 loc={"architecture":"Existing native language dictionaries/translatable components, no B6 source research/merge","oldKeys":len(oldkeys),"targetKeys":len(newkeys),"addedKeys":sorted(newkeys.keys()-oldkeys.keys()),"removedKeys":sorted(oldkeys.keys()-newkeys.keys()),"targetKeysNotInCurrentAtdNativeRu":sorted(newkeys.keys()-runtimekeys),"coverageCaveat":"Missing from ATD files does not imply untranslated vanilla key; B7 must account vanilla resources. Literal text components are hashed, not reproduced.","b4Disposition":["LOCALIZATION_REVIEW_REQUIRED"],"keyPaths":[{"key":k,"oldPaths":sorted(oldkeys.get(k,[])),"targetPaths":sorted(newkeys.get(k,[]))} for k in sorted(oldkeys.keys()|newkeys.keys())]}
 queue=[]
 def q(id,classification,facts,detail):
  queue.append({"id":id,"b4Disposition":[classification],"facts":facts,"review":detail,"implementationPerformed":False})
 q("B4-MAIN-PIN","PIN_UPDATE_REQUIRED",{"oldVersion":"1.18.1","targetVersion":"1.21","targetVersionId":"Y2zZ5eSs"},"Update main official download/source identities through original enum/acquisition mechanism after B4 approval; content update alone no marker bump.")
 q("B4-MESSAGES","OVERRIDE_RESOURCE_UPDATE_REQUIRED",{"mainRemovedPaths":sum(x["pack"]=="bacap_override" and not x["targetMainPresent"] for x in rewardrows),"missingTargetMessageOverrides":len(missingmsgs)},"Review all missing/renamed/changed main message keys and layout; companion messages remain separately unresolved.")
 q("B4-SEARCH","SEARCH_LINK_UPDATE_REQUIRED",{"removedIds":sorted({x["advancementId"] for x in allhl if x["classification"]=="REMOVED_ADVANCEMENT_ID"})},"Follow official new IDs and conservative rename evidence, without progress migration.")
 q("B4-PACK-METADATA","OVERRIDE_RESOURCE_UPDATE_REQUIRED",{"oldUpstream":old["packMetadata"]["packFields"],"targetUpstream":new["packMetadata"]["packFields"],"builtinOverridePackFormats":61},"Review bundled override metadata against26.2 compatibility ranges; source pack is now modern. No automatic converter marker revision follows metadata/content version.")
 q("B4-ROOT-MACROS","OVERRIDE_RESOURCE_UPDATE_REQUIRED",{"mainWrappersChanged":sum(x["pack"]=="bacap_override" and x["upstreamPathClassification"]=="UNCHANGED_PATH_CHANGED" for x in wrapperrows)},"Old root/milestone wrappers shadow new macro/reward/count/points behavior; preserve original ATD layering while matching necessary upstream contracts.")
 q("B4-TRACKERS","TRACKER_REVIEW_REQUIRED",{"bindings":86,"reviewRequired":sum(x["b4ReviewRequired"] for x in trackers),"removedAdvancementBindings":[x["binding"]["advancementId"] for x in trackers if not x["targetPresent"]]},"Review changed criteria/goals/objective contracts, including removed bac_apple_eaten/bac_1000th_item and ON_A_RAIL/HALF_HEART_LIFE. No edits to values in B3.")
 q("B4-TREE","TREE_GUI_REVIEW_REQUIRED",{"orderedChildren":28,"reviewRequired":sum(x["b4Disposition"]!=["NO_PRODUCT_CHANGE"] for x in orderedchildren)},"Review ordered child parents/categories/path assumptions and unknown-category fallback only where target requires.")
 q("B4-CONFIG","SETUP_SCOREBOARD_REVIEW_REQUIRED",contracts["summary"],"Review new point/tier defaults omitted by ATD setup, bac_dont_count initialization, macro wrappers, relocated update_score; retain raw-count selection.")
 q("B4-CONVERTER","CONVERTER_REVIEW_REQUIRED",converter.get("target",{}).get("summary",{}),"Review actual modern input rewrites, serialization-only positives and remaining narrow compatibility scope; do not infer marker change from version.")
 q("B4-UPSTREAM-DANGLING","UNKNOWN_REQUIRES_B4",{"missingStaticCallees":sorted({e["callee"] for e in ng["edges"] if e["resolution"]=="MISSING_STATIC_CALLEE"})},"Determine reachability/required narrow compatibility handling for upstream stale trophy paths and advancement/loser_hurt; Terralith hooks are separate companion concerns.")
 q("B4-MACRO-OPTIONAL-RESOURCES","UNKNOWN_REQUIRES_B4",{"resolvedConditionalAbsentPaths":len({e["callee"] for e in ng["edges"] if e["resolution"]=="CONDITIONAL_MACRO_RESOURCE_ABSENT_REVIEW"})},"Shared macro refers conditionally to reward/exp/trophy resources absent for some advancements. Distinguish guard/no-content conventions and runtime behavior from direct stale paths; do not blanket-create reward files.")
 q("B4-FANPACK","COMPANION_REVIEW_REQUIRED",{"undefinedHooks":sorted({e["callee"] for e in ng["edges"] if e["resolution"]=="EXTERNAL_FANPACK_EXTENSION_HOOK_REVIEW_REQUIRED"})},"Document extension hooks and companion requirements; no companion pin resolution here.")
 q("B4-RU","LOCALIZATION_REVIEW_REQUIRED",{"addedKeys":len(loc["addedKeys"]),"removedKeys":len(loc["removedKeys"])},"B6 research all usable Russian sources/permissions and fill native keys; no new translation architecture.")
 q("B4-CERT","TEST_SUCCESSOR_REQUIRED",{"targetAll":1332,"targetCanonical":1242},"Create Phase B successors; never replace historical Phase A/FINAL19/publication oracles.")
 if any(x["status"]=="EXPLICIT_B4_REVIEW_QUEUE" for x in pathaccounts):
  q("B4-PATH-UNRESOLVED","UNKNOWN_REQUIRES_B4",{"occurrences":sum(x["status"]=="EXPLICIT_B4_REVIEW_QUEUE" for x in pathaccounts)},"All unresolved B2 PATH_SENSITIVE tokens remain individually listed for B4; no silent omission.")
 summary={"rewardMessages":len(rewardrows),"mainRewardMessages":sum(x["pack"]=="bacap_override" for x in rewardrows),"companionRewardMessages":sum(x["pack"]!="bacap_override" for x in rewardrows),"mainMessagePathsStillPresent":sum(x["pack"]=="bacap_override" and x["targetMainPresent"] for x in rewardrows),"mainMessagePathsAbsent":sum(x["pack"]=="bacap_override" and not x["targetMainPresent"] for x in rewardrows),"targetMessagesWithoutMainOverride":len(missingmsgs),"trackerBindings":len(trackers),"trackerReviewRequired":sum(x["b4ReviewRequired"] for x in trackers),"orderedGuiChildren":len(orderedchildren),"pathSensitiveOccurrencesAccounted":len(pathaccounts),"pathSensitiveStatuses":summarize_rows(pathaccounts,"status"),"searchRemovedUniqueIds":len({x["advancementId"] for x in allhl if x["classification"]=="REMOVED_ADVANCEMENT_ID"})}
 return {"summary":summary,"externalPins":[{**x,"b4Disposition":["PIN_UPDATE_REQUIRED"] if x["enum"]=="BACAP" else ["COMPANION_REVIEW_REQUIRED"],"changedByB3":False} for x in b2["acquisitionFlow"]["externalPins"]],"rewardMessages":{"overrides":rewardrows,"targetMessagesWithoutMainOverride":missingmsgs},"rootWrappers":wrapperrows,"setupConfiguration":{"overrides":setuprows,"upstreamContracts":{"report":"b3_resource_graph_diff.json","section":"scoreboardContracts","summary":contracts["summary"]}},"trackerBindings":trackers,"treeGui":{"categories":b2["searchAndGuiCoupling"]["fixedTabs"],"orderedParentMaps":parentrefs,"orderedChildren":orderedchildren,"explicitB2AdvancementIds":allgui,"targetCanonicalOutsideFixedCategoryOrDepthRule":unmatchedcategories,"note":"Inherited 'animal' versus HUSBANDRY classification exceptions are retained, not assumed new failures."},"searchLinks":{"references":allhl,"generatedAbilityLinks":generatedlinks},"compatibilityConverter":{"report":"b3_resource_graph_diff.json","section":"converterAnalysis","currentMarker":converter["currentMarker"],"markerChanged":False,"targetSummary":converter["target"]["summary"],"targetReviewFindings":converter["target"]["reviewFindings"],"staticInputFeatureReview":converter["target"]["staticInputFeatureReview"]},"localizationInputs":loc,"companionSurfaces":[{**x,"b4Disposition":["COMPANION_REVIEW_REQUIRED"],"mainArchiveComparisonOnly":True,"resolvedByB3":False} for x in b2["companionIntegration"]],"testSuccessors":{"historicalFileRoles":b2["testCertificationCoupling"]["fileRoleCounts"],"newTargetTestsNeeded":b2["testCertificationCoupling"]["futureCoverage"],"b4Disposition":["TEST_SUCCESSOR_REQUIRED"],"historicalEvidenceUnchanged":True},"deferredAbilityBalance":b2["deferredToLaterPhases"],"pathSensitiveAccounting":pathaccounts,"b4ReviewQueue":queue,"b4Started":False}
def verify_reconciliation(adv,graph,impact,b1inv,b2,pathsens,new):
 assert len(adv["advancements"])==len(set(x["resourceId"] for x in adv["advancements"]))==1339
 assert adv["setCounts"]=={"common":1222,"oldOnly":7,"targetOnly":110,"BYTE_IDENTICAL":594,"SEMANTICALLY_IDENTICAL_BYTES_DIFFER":4,"SEMANTICALLY_CHANGED":624}
 assert adv["fieldChangeCounts"]=={"parent":34,"display":55,"criteria":574,"requirements":25,"rewards":27,"sends_telemetry_event":0,"otherTopLevelFields":0}
 assert adv["completionGroupingSemanticChanges"]==106
 assert adv["setCounts"]["common"]+adv["setCounts"]["oldOnly"]==1229
 assert adv["setCounts"]["common"]+adv["setCounts"]["targetOnly"]==1332
 assert sum(adv["setCounts"].get(k,0) for k in ["BYTE_IDENTICAL","SEMANTICALLY_IDENTICAL_BYTES_DIFFER","SEMANTICALLY_CHANGED"])==adv["setCounts"]["common"]
 assert len(adv["renameAnalysis"]["oldOnly"])==adv["setCounts"]["oldOnly"]
 assert len(adv["renameAnalysis"]["targetOnly"])==adv["setCounts"]["targetOnly"]
 assert len(impact["trackerBindings"])==86
 assert len(impact["rewardMessages"]["overrides"])==1187
 assert len(impact["treeGui"]["orderedChildren"])==28
 assert len(impact["treeGui"]["orderedParentMaps"])==3
 assert len(impact["treeGui"]["categories"])==17
 assert not graph["danglingTargetReferences"]["predicates"]
 assert not graph["danglingTargetReferences"]["packNamespaceRegistryTags"]
 assert len(impact["rootWrappers"])==17 and len(impact["setupConfiguration"]["overrides"])==10
 assert len(impact["pathSensitiveAccounting"])==len(pathsens)
 assert all(x["status"] and x["b4Disposition"] for x in impact["pathSensitiveAccounting"])
 assert len({x["occurrenceNumber"] for x in impact["pathSensitiveAccounting"]})==len(pathsens)
 assert all(set(x["b4Disposition"])<=DISPOSITIONS for x in impact["pathSensitiveAccounting"])
 assert len(b1inv["advancements"])==1332
 for r in b1inv["advancements"]:
  a=new["adv"][r["resourceId"]];assert a["path"]==r["path"] and a["byteSha256"]==r["sha256"] and a["canonical"]==r["canonical"]
 for side in ["old","target"]:
  summary=graph["converterAnalysis"][side]["summary"]
  assert all(summary[k]==0 for k in ["conversionExceptions","diagnosticDisagreements","actualWorldCopyPayloadMismatches","idempotenceFailures"]),summary
def markdown_report(a,g,i):
 b=a["identities"];s=a["setCounts"];c=a["canonicalCounts"];r=a["renameAnalysis"];f=a["fieldChangeCounts"];p=i["summary"]
 text="# B3 — Complete BACAP Semantic Diff\n\nStatus: **PHASE_B_B3_DIFFED**. Analysis only; B4 has not started.\n\n"
 text+="## 1. Exact input identities\n\n"
 for side in ["old","target"]:text+="- "+side+": BACAP "+b[side]["version"]+", "+b[side]["archive"]+"; SHA256 "+b[side]["sha256"]+".\n"
 text+="\nTarget Modrinth version Y2zZ5eSs. All eight B0/B1/B2 records are preserved. The tool is read-only and writes only evidence/ignored probe outputs. Reports contain identities/hashes/structural facts, not upstream resource bodies.\n\n"
 text+="Reproduce from the repository root using Python and the existing cached JDK/Minecraft/Gson:\n\n"
 text+="    python tools/phase_b/b3_bacap_semantic_diff.py --old-zip reference/phase_a_preservation/files/final/bacap.zip --target-zip \"build/tmp/phase_b_b1/BlazeandCave's Advancements Pack 1.21.zip\" --b1-inventory reference/phase_b/b1_target_inventory.json --b2-map reference/phase_b/b2_atd_integration_map.json --b2-occurrences reference/phase_b/b2_bacap_occurrences.json --output-dir build/tmp/phase_b_b3/reproduction --temp-dir build/tmp/phase_b_b3/reproduction_probe\n\n"
 text+="Output directories must not already contain these reports. No network/Gradle/commands in Minecraft are executed. Object keys normalize; all arrays preserve order. Separate requirement fingerprints canonicalize AND-of-OR logic. Two old advancement JSON files and one old predicate JSON file contain non-strict backslash-apostrophe escapes; a narrowly recorded Gson-compatible repair is applied only to parsed comparison, with original byte hashes preserved.\n\n"
 text+="## 2. Advancement ID sets\n\n| Classification | Count |\n|---|---:|\n"
 for k,v in s.items():text+="| "+k+" | "+str(v)+" |\n"
 text+="\n1222+7=1229 old;1222+110=1332 target. Every common ID has one exclusive byte/semantic classification; one record per union ID (1339).\n\n"
 text+="## 3. Canonical reconciliation\n\n1152 − "+str(c["OLD_CANONICAL_ID_REMOVED"])+" removed − "+str(c["CEASED_CANONICAL"])+" ceased + "+str(c["BECAME_CANONICAL"])+" became + "+str(c["NEW_CANONICAL_ID_ADDED"])+" added =1242. Canonical in both: "+str(c["CANONICAL_IN_BOTH"])+". No rename inference enters this arithmetic. +90 is net only;110 IDs were added overall and97 of them are canonical.\n\n"
 text+="## 4. Same-ID semantic changes\n\n624 common advancements differ in parsed JSON. Per-ID hashes, changed fields, criterion/trigger/condition identifiers, requirement groups, reward-path facts and tree metadata are in b3_advancement_diff.json. Literal text is represented by fingerprints; translatable keys are preserved as identifiers.\n\n"
 text+="## 5. Rename/move candidates\n\n"
 for k,v in r["summary"].items():text+="- "+k+": "+str(v)+".\n"
 text+="\nConfirmed pairs require named official changelog evidence from the B1-retained selected-version snapshot, plus structural signals. Hash/name/criteria similarities alone remain candidate evidence, not forced one-to-one mapping. All old-only and target-only entries have a candidate or NO_MATCH record. Old Adventure spear_fishing must not be confused with the new Weaponry advancement sharing its basename.\n\n"
 for x in r["pairs"]:
  if x["classification"] in ["CONFIRMED_RENAME_OR_MOVE","HIGH_CONFIDENCE_CANDIDATE"]:text+="- "+x["oldId"]+" -> "+x["targetId"]+": "+x["classification"]+".\n"
 text+="\n## 6. Tree/category relationships\n\nNo internal target missing parents or cycles. Parent changes: "+str(f["parent"])+". Root/category/depth transitions are recorded per ID. Seventeen ATD categories, three ordered parent maps and all28ordered child IDs are accounted. Inherited animal/HUSBANDRY and technical path exceptions are distinguished from new target facts; unknown-category fallback is recorded without GUI edits.\n\n"
 text+="## 7. Criteria/requirements/rewards fields\n\n| Field | Changed common IDs |\n|---|---:|\n"
 for k,v in f.items():text+="| "+k+" | "+str(v)+" |\n"
 text+="\nEffective defaulted AND-of-OR grouping changes: "+str(a["completionGroupingSemanticChanges"])+". This includes criteria-driven default groups even when an explicit requirements field did not change. Raw array ordering remains separately fingerprinted; trigger/condition changes affect completion independently.\n\n"
 text+="## 8. Resource paths and static graphs\n\n| Resource type | Identical | Changed | Removed | Added |\n|---|---:|---:|---:|---:|\n"
 for typ in ["function","predicate","tags","dialog"]:
  counts=collections.Counter()
  for row in g["resourceCounts"]:
   if row["resourceType"]==typ:counts[row["classification"]]+=row["count"]
  text+="| "+typ+" | "+str(counts["UNCHANGED_PATH_IDENTICAL"])+" | "+str(counts["UNCHANGED_PATH_CHANGED"])+" | "+str(counts["REMOVED_PATH"])+" | "+str(counts["ADDED_PATH"])+" |\n"
 text+="\nPack metadata changes from old pack_format61 to target min/max[107,1]; descriptions are hash-only. Dialog action references: "+str(len(g["dialogCommandReferences"]))+". Relocation candidates: "+str(len(g["relocationCandidates"]))+". Matching text/hash is evidence of possible relocation, not proof of semantic equivalence. Function graph masks quoted chat text, recognizes direct/execute/schedule and tag calls, and binds literal macro arguments; runtime-storage-derived calls remain dynamic. Target graph summary: "+json.dumps({k:(len(v) if isinstance(v,list) else v) for k,v in g["functionCallGraphSummary"]["target"].items()},sort_keys=True)+".\n\n"
 text+="Undefined fanpack extension hooks, conditional macro reward paths, companion hooks, stale trophy-category calls and potential upstream dangling calls remain separately visible for B4. Missing macro expansions are guard-dependent static review items, not established runtime errors. Registry tags are distinct from predicate/function files; vanilla registry existence is not inferred from archive absence. See b3_resource_graph_diff.json for all paths, edges, memberships, missing refs and resolved macro contexts.\n\n"
 text+="## 9. Reward-message/override impact\n\nAll1187override messages accounted:1148main+39companion. Main target paths present:"+str(p["mainMessagePathsStillPresent"])+", absent:"+str(p["mainMessagePathsAbsent"])+". Target message resources without base ATD override:"+str(p["targetMessagesWithoutMainOverride"])+". Removed Search IDs:"+str(p["searchRemovedUniqueIds"])+". Per-file keys/hash/highlight impacts are recorded. Companion-only main-archive absence is not a compatibility verdict. All17root/milestone wrappers and10setup/configuration files are accounted.\n\n"
 text+="## 10. Tracker/GUI/Search impact\n\nAll86tracker bindings retained unchanged; "+str(p["trackerReviewRequired"])+" need B4 review by static evidence/special scope. ON_A_RAIL and HALF_HEART_LIFE are explicitly flagged. Criteria/stat/entity/goal/objective relevance is recorded; no numerical edits/rebalance. All28ordered GUI children and every B2 production PATH_SENSITIVE occurrence are accounted individually: "+str(p["pathSensitiveOccurrencesAccounted"])+". Generic selectors/namespaces are distinguished from mapped resources and explicit unresolved B4 items.\n\n"
 text+="## 11. Scoreboard/configuration contracts\n\nSix original raw-count objectives retain their definitions. Defined objectives87->110,25added/2removed (bac_apple_eaten,bac_1000th_item). Weighted bac_advancements_points/bac_advancements_team_points are separate. Shared advancement_made_macro wraps1256rewards and gates raw increments by bac_dont_count; hidden tier defaults excluded. New point defaults are absent from ATD's old new_world override. Raw score_add is byte-identical, but that does not prove identical behavior through new macro wrappers. update_score substantive body relocates to blazeandcave:config/update_score with retained old delegate; selector counts1178->1268 are reconstruction contracts, not canonical counts. Do not replace raw ATD progression with weighted points.\n\n"
 text+="## 12. Actual converter target-input probe\n\nThe unchanged published Java converter/legacy shims are compiled from byte-identical temporary copies against cached Minecraft/Gson; diagnostic instrumentation is a separate copy cross-checked against authoritative output. Conversion runs only on ignored archive copies; B1 ZIP stays unchanged. Source/harness/dependency hashes and per-family paths/counts are recorded. Candidate runtime adapter visits are not automatically obsolete structures; family attribution includes nested effects.\n\n"
 text+="Target actual summary: "+json.dumps(g["converterAnalysis"]["target"]["summary"],sort_keys=True)+".\n\n"
 text+="| Converter family | Candidate files | Changed files |\n|---|---:|---:|\n"
 for family in g["converterAnalysis"]["target"]["families"]:
  text+="| "+family["family"]+" | "+str(family["candidateCount"])+" | "+str(family["changedCount"])+" |\n"
 text+="\n"
 text+="Modern no-op/serialization-only matches, actual parsed changes, idempotence, parse failures and static unsupported-construct candidates are distinguished. Probe metadata stubs supply historical pin/enum values only; temporary marker is not a newly accepted production pin. No marker bump or converter implementation change is made.\n\n"
 text+="## 13. Concrete B4 review queue\n\n"
 for q in i["b4ReviewQueue"]:text+="- **"+q["id"]+"** ("+", ".join(q["b4Disposition"])+"): "+q["review"]+"\n"
 text+="\nLocalization input keys are inventoried without source research/merge; companions remain unresolved and no new pack was downloaded. Historical tests/publication snapshots require Phase B successors rather than rewritten Phase A/FINAL19 evidence.\n\n"
 text+="## 14. Phase C/D deferrals and safety\n\nChanging ability thresholds,1152custom-threshold clamp,balance/pacing or terminal progression remains DEFER_TO_PHASE_C_OR_D. No migration of old completed progress is required.\n\nBranch phase-b-bacap-26.2 and HEAD b261b02cd03b4aeae2835c63f982c9aa53c46eed remain unchanged. Product Java/resources/localization/tests,archives,all eight B0/B1/B2 records,compat_26_2_r16 and production pins unchanged. No companion pin/download,commit,push,tag,GitHub mutation or B4 execution. Only one analysis tool and four B3 durable reports are added; probes are ignored temporary files.\n\n**PHASE_B_B3_DIFFED**\n"
 return text


def main():
 ap=argparse.ArgumentParser(description=__doc__)
 ap.add_argument("--old-zip",required=True);ap.add_argument("--target-zip",required=True)
 ap.add_argument("--b1-inventory",required=True);ap.add_argument("--b2-map",required=True);ap.add_argument("--b2-occurrences",required=True)
 ap.add_argument("--repo-root",default=".");ap.add_argument("--output-dir",required=True);ap.add_argument("--temp-dir",required=True)
 run(ap.parse_args())
if __name__=="__main__":main()
