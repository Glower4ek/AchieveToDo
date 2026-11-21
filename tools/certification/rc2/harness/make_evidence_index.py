"""Index of the raw W5 evidence that stays outside git (runtime folders, ~300 MB of server/driver logs).

For every final runtime (campaign11-18) and every aborted attempt kept for transparency, records the
SHA-256 and size of each receipts file (output.log, journals, results, REPLAY.json, ...), the plugin
hash from PRELAUNCH.json and the E6 replay counts. Anyone holding the evidence folder can re-verify it
against this index; anyone holding only git can see exactly which bytes the certificates refer to.
Usage: make_evidence_index.py <w5-campaign-folder> <out.json> [final runtime names ... (default campaign11-18)]"""
import hashlib, json, sys
from pathlib import Path

FINAL = [f'campaign{n}' for n in range(11, 19)]


def sha(p):
    h = hashlib.sha256()
    with open(p, 'rb') as f:
        for b in iter(lambda: f.read(1 << 20), b''):
            h.update(b)
    return h.hexdigest()


def runtime(d):
    rec = d / 'receipts'
    files = {f.name: dict(sha256=sha(f), bytes=f.stat().st_size) for f in sorted(rec.iterdir()) if f.is_file()} if rec.exists() else {}
    pre = json.loads((d / 'PRELAUNCH.json').read_text(encoding='utf8')) if (d / 'PRELAUNCH.json').exists() else {}
    rep = json.loads((rec / 'REPLAY.json').read_text(encoding='utf8')) if (rec / 'REPLAY.json').exists() else None
    return dict(plugin=pre.get('plugin', {}).get('sha256'), server=pre.get('server', {}).get('sha256'),
                replay=dict(counts=rep['counts'], reproducedAll=rep['reproducedAll']) if rep else None, receipts=files)


def main(argv):
    root, out = Path(argv[0]), Path(argv[1])
    final = argv[2:] or FINAL
    prefixes = sorted({n[:-1] for n in final})   # campaign1, campaign3 ...
    index = dict(final={n: runtime(root / n) for n in final},
                 aborted={d.name: runtime(d) for pre in prefixes for d in sorted(root.glob(pre + '*-*')) if d.is_dir()})
    out.write_text(json.dumps(index, indent=1), encoding='utf8')
    print(f"{len(index['final'])} final runtimes, {len(index['aborted'])} aborted attempts -> {out}")


if __name__ == '__main__':
    main(sys.argv[1:])
