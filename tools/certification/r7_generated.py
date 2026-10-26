"""Fail-closed R7 archive authority. Payloads never come from envelope templates.

Minecraft restoration reuses the admitted historical ZIP envelope only; every
compressed stream must come from the independently checked transformation. The
separate canonical output has a new identity. B10 changes only Properties comments
and key ordering under an exact six-key ASCII contract, after real conversion.
"""
from pathlib import Path
import argparse,base64,hashlib,json,struct,zipfile,zlib
from r7_bootstrap import require,digest,relative,unique

MARKER='achievetodo_compatibility/compat_26_2.properties'
VERSIONS={'minecraft':'S5_R7_MINECRAFT_ENVELOPE_V1','b10':'S5_R7_B10_DETERMINISTIC_V1'}

def compressed(data,i):
    require(data[i.header_offset:i.header_offset+4]==b'PK\x03\x04','BAD_LOCAL_HEADER')
    a,b=struct.unpack_from('<HH',data,i.header_offset+26)
    start=i.header_offset+30+a+b
    return data[start:start+i.compress_size],start

def metadata(i):
    return {k:getattr(i,k) for k in ['compress_type','flag_bits','create_system','create_version','extract_version','external_attr','internal_attr']}

def properties(data):
    require(len(data)<1024,'MARKER_TOO_LARGE')
    text=data.decode('ascii');out={}
    for line in text.splitlines():
        if line.startswith('#') or not line:continue
        require('=' in line and '\\' not in line and not line.startswith(('!',' ')), 'MALFORMED_MARKER')
        k,v=line.split('=',1);require(k not in out,'DUPLICATE_MARKER_KEY');out[k]=v
    return out

def load(path,expected,kind):
    data=Path(path).read_bytes();require(digest(data)==expected,'AUTHORITY_TAMPER')
    a=json.loads(data,object_pairs_hook=unique)
    require(a['authority']==VERSIONS[kind] and a['kind']==kind,'AUTHORITY_VERSION')
    require(a['generatorSHA256']==digest(Path(__file__).read_bytes()),'GENERATOR_REVISION')
    require(a['zlib']==zlib.ZLIB_VERSION and a['zlibRuntime']==zlib.ZLIB_RUNTIME_VERSION,'COMPRESSION_TOOLCHAIN')
    return a

def validate(path,a):
    data=Path(path).read_bytes()
    with zipfile.ZipFile(path) as z:
        names=z.namelist();require(len(names)==len(set(names))==len({n.casefold() for n in names}),'DUPLICATE_ENTRY')
        require(set(names)==set(a['entries']) and not z.comment,'ENTRY_INVENTORY_OR_COMMENT')
        require(z.testzip() is None,'ZIP_CRC')
        for n in names:
            relative(n[:-1] if n.endswith('/') else n)
            require(not n.upper().endswith(('.SF','.RSA','.DSA','.EC')),'SIGNED_ARCHIVE_REFUSED')
            i=z.getinfo(n);r=a['entries'][n];b=z.read(n)
            require(metadata(i)==r['metadata'] and not i.comment,'UNSUPPORTED_METADATA:'+n)
            if a['kind']=='b10' and n==MARKER:
                require(properties(b)==a['properties'],'MARKER_SEMANTICS')
            else:require(len(b)==r['bytes'] and digest(b)==r['sha256'],'PAYLOAD_IDENTITY:'+n)
            # Only directory time fields may vary in a fresh Loom envelope.
            if a['kind']=='minecraft' and n.endswith('/'):
                require(len(i.extra)==36 and i.extra[:12]==bytes.fromhex('0a0020000000000001001800'),'UNSUPPORTED_DIRECTORY_EXTRA')
                require(i.date_time[0]>=1980,'UNSUPPORTED_DIRECTORY_TIME')
            else:
                require(list(i.date_time)==r['date'] and i.extra.hex()==r['extra'],'UNSUPPORTED_FILE_TIME_OR_EXTRA:'+n)
            stream,_=compressed(data,i)
            if a['kind']=='minecraft':require(digest(stream)==r['compressedSHA256'],'COMPRESSION_STREAM_IDENTITY:'+n)
        return data

def generate(path,contract,expected,output,kind,restore=False):
    a=load(contract,expected,kind);data=validate(path,a)
    output=Path(output);require(not output.exists(),'OUTPUT_MUST_BE_NEW')
    require(output.resolve()!=Path(path).resolve(),'SOURCE_OVERWRITE')
    output.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(path) as z:
        if restore:
            require(kind=='minecraft','RESTORE_ONLY_MINECRAFT')
            chunks=[]
            for row in a['envelope']:
                if 'literal' in row:chunks.append(base64.b64decode(row['literal'],validate=True))
                else:chunks.append(compressed(data,z.getinfo(row['stream']))[0])
            b=b''.join(chunks);require(digest(b)==a['historical']['sha256'] and len(b)==a['historical']['bytes'],'RESTORED_IDENTITY')
            with output.open('xb') as f:f.write(b)
        else:
            with output.open('xb') as f,zipfile.ZipFile(f,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as out:
                for n in sorted(z.namelist()):
                    b=z.read(n)
                    if kind=='b10' and n==MARKER:b=''.join(k+'='+v+'\n' for k,v in sorted(a['properties'].items())).encode('ascii')
                    i=zipfile.ZipInfo(n,(1980,1,1,0,0,0));i.create_system=3;i.external_attr=(0o40755 if n.endswith('/') else 0o100644)<<16;i.compress_type=zipfile.ZIP_DEFLATED
                    out.writestr(i,b,compresslevel=9)
    return {'authority':a['authority'],'input':digest(data),'output':{'bytes':output.stat().st_size,'sha256':digest(output.read_bytes())},'restoredHistoricalEnvelope':restore,'entryPayloadsPreserved':kind=='minecraft','runtimeSerializerChanged':False}

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for n in ['input','contract','contract-sha256','output','kind']:p.add_argument('--'+n,required=True)
    p.add_argument('--restore-historical-envelope',action='store_true');a=p.parse_args()
    print(json.dumps(generate(a.input,a.contract,a.contract_sha256,a.output,a.kind,a.restore_historical_envelope),indent=2))
