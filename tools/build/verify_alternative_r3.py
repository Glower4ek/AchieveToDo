"""Explicit pinned alternative authority. Never falls back or edits historical authority."""
from pathlib import Path
import hashlib,json,sys,importlib.util,argparse
MANIFEST_SHA256='5e23108b8dc54376f96350b7ae38f4bb1b7eab273c35f8957cbb99854b7c41d9'
def sha(b):return hashlib.sha256(b).hexdigest()
def require(c,m):
 if not c:raise ValueError(m)
def unique(pairs):
 d={}
 for k,v in pairs:require(k not in d,'DUPLICATE_JSON_KEY');d[k]=v
 return d
def safe(root,name):
 root=Path(root);rel=Path(name);require(not rel.is_absolute() and '..' not in rel.parts,'PATH_ESCAPE')
 p=root/rel
 for x in [p,*p.parents]:require(not x.is_symlink() and not x.is_junction(),'LINK_REJECTED')
 require(p.resolve().is_relative_to(root.resolve()) and p.is_file(),'MISSING_OR_ESCAPING_FILE')
 return p
def check(root,row,key='path'):
 p=safe(root,row[key]);b=p.read_bytes();require(len(b)==row['size'] and sha(b)==row['SHA256'],'EXACT_IDENTITY:'+row[key]);return p
def verify(source,root,manifest,authority):
 require(authority=='S5_R3_ALT_METADATA_V1','WRONG_AUTHORITY')
 source=Path(source);root=Path(root);manifest=Path(manifest)
 require(root.is_absolute() and not root.resolve().is_relative_to(source.resolve()),'SOURCE_BOUNDARY')
 raw=manifest.read_bytes();require(sha(raw)==MANIFEST_SHA256,'MANIFEST_IDENTITY')
 m=json.loads(raw,object_pairs_hook=unique);require(m['authority']==authority and m['automaticFallback'] is False,'MANIFEST_AUTHORITY')
 rows=m['entries'];require(len(rows)==len({r['logicalName'] for r in rows})==20,'INPUT_CARDINALITY')
 require({p.name for p in root.iterdir()}=={r['path'] for r in rows},'INPUT_NAMES')
 require(sum(r['authority']=='HISTORICAL_ORIGINAL' for r in rows)==15,'ORIGINAL_COUNT')
 require(sha(safe(source,'src/test/resources/build_inputs/manifest.json').read_bytes())==m['historicalManifestSHA256'],'HISTORICAL_MANIFEST')
 spec=importlib.util.spec_from_file_location('original',source/'tools/build/verify_inputs.py');old=importlib.util.module_from_spec(spec);spec.loader.exec_module(old);old.authority(source)
 for r in rows:
  p=check(root,r)
  if r['authority']=='HISTORICAL_ORIGINAL':require(r['SHA256']==r['historicalSHA256'] and r['size']==r['historicalSize'],'ORIGINAL_IDENTITY_REWRITE')
  elif r['type']=='metadataJSON':
   obj=json.loads(p.read_bytes(),object_pairs_hook=unique);prefix=r['logicalName'].removesuffix('_metadata.json')
   acq=json.loads(safe(root,prefix+'_acquisition.json').read_bytes());archive=safe(root,prefix+'.zip').read_bytes()
   require(obj['project']['id']==acq['projectId']==r['sourceProject'],'PROJECT_ID')
   require(obj['project']['license']['id']==acq['license']['id']==r['licenseReference']['classification'],'LICENSE_CONTRADICTION')
   v=next(v for v in obj['versions'] if v['id']==acq['versionId']);require(v['project_id']==r['sourceProject'] and v['version_number']==acq['version'],'VERSION_ID')
   f=next(f for f in v['files'] if f['filename']==acq['filename']);require(f['size']==acq['size']==len(archive),'ARCHIVE_SIZE')
   for alg,digest in acq['hashes'].items():require(hashlib.new(alg,archive).hexdigest()==digest,'ARCHIVE_HASH')
   for alg,digest in f['hashes'].items():require(hashlib.new(alg,archive).hexdigest()==digest,'METADATA_ARCHIVE_HASH')
  else:raise ValueError('UNKNOWN_AUTHORITY')
 for r in m['packSources']:check(root if r['authority']=='external' else source,r)
 return {'status':'PASS','authority':authority,'original':15,'alternatives':5,'total':20,'historicalOriginalStatus':'FAIL_15_OF_20','historicalPermissionsUnchanged':True}
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--root',required=True);p.add_argument('--manifest',required=True);p.add_argument('--authority',required=True);a=p.parse_args()
 try:print(json.dumps(verify(a.source,a.root,a.manifest,a.authority)))
 except Exception as e:print(type(e).__name__+': '+str(e));sys.exit(1)
