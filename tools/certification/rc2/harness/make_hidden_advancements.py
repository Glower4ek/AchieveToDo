"""Hidden advancements (no "display"): datapack bookkeeping that is not progression and is not counted.
Leak oracle ignores their criteria. Built from the runtime datapacks (later packs override earlier ones).
Usage: make_hidden_advancements.py <runtime world dir>  -> hidden_advancements.json"""
import hashlib, json, sys, zipfile
from pathlib import Path

world = Path(sys.argv[1])
defs, sources = {}, {}
for pack in ('bacap.zip', 'bacap_override.zip'):
    p = world / 'datapacks' / pack
    sources[pack] = hashlib.sha256(p.read_bytes()).hexdigest()
    with zipfile.ZipFile(p) as z:
        for n in z.namelist():
            parts = n.split('/')
            if len(parts) >= 4 and parts[0] == 'data' and parts[2] == 'advancement' and n.endswith('.json'):
                adv = f'{parts[1]}:' + '/'.join(parts[3:])[:-5]
                try:
                    defs[adv] = 'display' in json.loads(z.read(n))
                except ValueError:
                    pass
hidden = sorted(a for a, shown in defs.items() if not shown)
out = Path(__file__).parent / 'hidden_advancements.json'
out.write_text(json.dumps(dict(sources=sources, hidden=hidden), indent=1), encoding='utf8')
print(len(hidden), 'hidden of', len(defs))
