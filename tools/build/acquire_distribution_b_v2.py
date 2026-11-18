"""Explicit ten-endpoint capture only; never called by verifier, Gradle or JUnit.

Successful transport is RAW_CAPTURE_ONLY_NOT_TRUSTED, never V2 input PASS.
Partial failures and bytes are retained without retrying a different identity.
"""
from pathlib import Path
from datetime import datetime, timezone
import argparse
import hashlib
import http.client
import json
import platform
import shutil
import ssl
import sys
import time
from urllib.parse import urlsplit

if __package__:
    from . import distribution_b_v2_contract as c
else:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
    from tools.build import distribution_b_v2_contract as c


def utc():
    return datetime.now(timezone.utc).isoformat()


def write_new(path, value):
    with path.open('x', encoding='utf-8', newline='\n') as stream:
        json.dump(value, stream, indent=2)


def new_root(path, source):
    path = Path(path)
    c.require(path.is_absolute() and not path.exists(), 'new absolute capture root required')
    c.safe_root(path.parent, source)
    path.mkdir()
    return c.safe_root(path, source)


def capture_one(endpoint, name, payload, evidence, maximum=10485760, timeout=30):
    parsed = urlsplit(endpoint)
    c.require(parsed.scheme == 'https' and parsed.netloc == 'api.modrinth.com'
              and not parsed.query and not parsed.fragment, 'fixed endpoint host/scheme')
    headers = {'Accept': 'application/json', 'Accept-Encoding': 'identity',
               'User-Agent': 'AchieveToDo-GateB-V2-OwnerA-Capture/1 (fixed-ID review capture)'}
    receipt = {'schema': 1, 'name': name, 'attempt': 1, 'requestURL': endpoint,
               'method': 'GET', 'requestHeaders': headers, 'startedUTC': utc(),
               'transport': 'Python http.client direct TLS; no environment proxies; no redirects',
               'pythonVersion': platform.python_version(), 'tlsValidation': True,
               'status': None, 'result': 'CAPTURE_FAILED', 'bodyBytes': 0,
               'bodySHA256': None, 'finalURL': endpoint, 'error': None}
    connection = http.client.HTTPSConnection(parsed.hostname, timeout=timeout,
                                            context=ssl.create_default_context())
    deadline = time.monotonic()+timeout
    def remaining():
        value = deadline-time.monotonic()
        c.require(value > 0, 'total request timeout')
        connection.timeout = value
        if connection.sock is not None:
            connection.sock.settimeout(value)
        return value
    try:
        remaining()
        connection.request('GET', parsed.path, headers=headers)
        remaining()
        response = connection.getresponse()
        receipt['status'] = response.status
        receipt['responseHeaders'] = {key: response.getheader(key) for key in
            ('Content-Type', 'Content-Encoding', 'ETag', 'Last-Modified', 'Date', 'Content-Length', 'Location')}
        length = response.getheader('Content-Length')
        if length is not None:
            c.require(length.isdecimal() and int(length) <= maximum, 'response length limit')
        chunks, total = [], 0
        while total <= maximum:
            remaining()
            chunk = response.read1(min(65536, maximum+1-total))
            if not chunk:
                break
            chunks.append(chunk)
            total += len(chunk)
        body = b''.join(chunks)
        remaining()
        receipt['bodyBytes'] = len(body)
        receipt['bodySHA256'] = hashlib.sha256(body).hexdigest()
        with (evidence/(name+'.response-body.bin')).open('xb') as stream:
            stream.write(body)  # Also retain error entities; never treat them as metadata.
        c.require(len(body) <= maximum, 'response byte limit')
        if length is not None:
            c.require(len(body) == int(length), 'truncated response')
        c.require(response.status == 200, 'HTTP status '+str(response.status))
        content_type = response.getheader('Content-Type', '').split(';', 1)[0].strip().lower()
        c.require(content_type == 'application/json', 'JSON media type required')
        c.require(response.getheader('Content-Encoding', 'identity').lower() == 'identity', 'identity content encoding required')
        with (payload/name).open('xb') as stream:
            stream.write(body)  # Original response entity bytes, no JSON serialization.
        c.strict_json(body)  # Validation does not rewrite captured raw bytes.
        receipt['result'] = 'RAW_CAPTURE_ONLY_NOT_TRUSTED'
        return receipt
    except Exception as error:
        receipt['error'] = type(error).__name__+': '+str(error)
        return receipt
    finally:
        receipt['finishedUTC'] = utc()
        connection.close()
        write_new(evidence/(name+'.transport.json'), receipt)


def run(source, original_root, payload_root, evidence_root):
    source = Path(source).absolute()
    p = c.policy(source)
    c.inherited_authority(source)
    original = c.safe_root(original_root, source)
    c.require({x.name for x in original.iterdir()} == {x['logicalName'] for x in p['originals']}, 'exact original fifteen root required')
    for row in p['originals']:
        c.check_identity(original, row)
    # Validate BOTH new roots before any creation or copy; never use old folders.
    payload_path, evidence_path = Path(payload_root), Path(evidence_root)
    c.require(payload_path.is_absolute() and evidence_path.is_absolute()
              and payload_path != evidence_path and not payload_path.exists() and not evidence_path.exists(), 'two separate NEW absolute roots required')
    c.require(not payload_path.is_relative_to(evidence_path) and not evidence_path.is_relative_to(payload_path), 'nested payload/evidence roots rejected')
    c.safe_root(payload_path.parent, source)
    c.safe_root(evidence_path.parent, source)
    payload = new_root(payload_path, source)
    evidence = new_root(evidence_path, source)
    for row in p['originals']:
        shutil.copyfile(c.safe_file(original, row['logicalName']), payload/row['logicalName'])
        c.check_identity(payload, row)
    receipts = []
    for row in p['sources']:
        for kind in ('project', 'version'):
            name = row[kind+'RawName']
            result = capture_one(row[kind+'Endpoint'], name, payload, evidence,
                                 p['maxResponseBytes'], p['captureTimeoutSeconds'])
            receipts.append(result)
            # Failure affects only that exact capture. Remaining approved fixed
            # endpoints may be independently observed; no version/fallback retry.
    complete = len(receipts) == 10 and all(x['result'] == 'RAW_CAPTURE_ONLY_NOT_TRUSTED' for x in receipts)
    summary = {'schema': 1, 'authority': c.AUTHORITY, 'scope': 'OWNER_A_CAPTURE_ONLY',
               'activated': False, 'trusted': False, 'ownerBRequired': True,
               'originalGate': 'STANDARD_INPUT_BLOCKED', 'completedResponses': sum(x['result'] == 'RAW_CAPTURE_ONLY_NOT_TRUSTED' for x in receipts),
               'requestedResponses': 10, 'payloadRoot': str(payload), 'evidenceRoot': str(evidence),
               'result': 'CAPTURE_COMPLETE_UNTRUSTED' if complete else 'CAPTURE_INCOMPLETE_BLOCKED',
               'receipts': [{'name': x['name'], 'result': x['result'], 'status': x['status'],
                             'bodyBytes': x['bodyBytes'], 'bodySHA256': x['bodySHA256']} for x in receipts]}
    write_new(evidence/'capture-summary.json', summary)
    return summary


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-root', type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument('--original-root', required=True)
    parser.add_argument('--payload-root', required=True)
    parser.add_argument('--evidence-root', required=True)
    args = parser.parse_args()
    result = run(args.source_root, args.original_root, args.payload_root, args.evidence_root)
    print(json.dumps(result, indent=2))
    return 0 if result['result'] == 'CAPTURE_COMPLETE_UNTRUSTED' else 1


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (ValueError, KeyError, TypeError, OSError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
