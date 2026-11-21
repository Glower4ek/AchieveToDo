"""Prepare a disposable campaign runtime from test-server/base-runtime on a given plugin build.

Usage: prepare_campaign.py <campaignNN|attemptNN> <plugin.jar> [autoStopSeconds] [easy|normal|hard|chaos|custom.toml]
Server JVM arguments come from attempt04/runtime_args.json (product flags, world identity) with paths
rebased onto the new runtime. The server-side trace: oracle uses certification/trace-agent (it replaces
Codex's lost causal-agent.jar); packet-witness.jar already ships in the base runtime's plugins.
"""
import hashlib, json, re, shutil, socket, sys
from pathlib import Path

root = Path(__file__).parent
base = root.parent.parent / 'test-server' / 'base-runtime'
trace_agent = root.parent / 'trace-agent' / 'trace-agent.jar'
TRACE_TARGETS = 'net/minecraft/world/item/Item.use,net/minecraft/world/item/FireworkRocketItem.use'

name, plugin = sys.argv[1], Path(sys.argv[2])
auto_stop = int(sys.argv[3]) if len(sys.argv) > 3 else 3600
assert re.fullmatch(r'(attempt|campaign)[0-9]{2,}', name)
assert trace_agent.is_file(), 'build certification/trace-agent first (sh build.sh)'
a = root / name
a.mkdir()
d = a / 'runtime'
shutil.copytree(base, d, ignore=shutil.ignore_patterns('logs', 'session.lock', 'w4-trace-probe.jar', 'desktop.ini'))
with socket.socket() as s:
    s.bind(('127.0.0.1', 52704))  # fails fast if another server still holds the port
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
ident = json.loads((root / 'STAGE2B_W5_ARTIFACT_IDENTITY.json').read_text())
assert sha(d / 'purpur-26.2-2620.jar') == ident['server']['sha256']
shutil.copy2(plugin, d / 'plugins' / 'achievetodo.jar')
o = a / 'observer-work'
o.mkdir()
shutil.copy2(trace_agent, o / 'trace-agent.jar')
shutil.copy2(root / 'run_server_v3.py', a / 'run_server.py')

template = json.loads((root / 'attempt04' / 'runtime_args.json').read_text())
args = []
for v in template['args']:
    if v.startswith('-Dachievetodo.bacap.directory='):
        v = '-Dachievetodo.bacap.directory=' + str(d / 'stage2b-w4-attempt01' / 'datapacks')
    elif v.startswith('-javaagent:') and 'causal-agent' in v:
        continue
    args.append(v)
# After the product agent, so the product's early returns are traced too.
args.append(f'-javaagent:{o / "trace-agent.jar"}={TRACE_TARGETS}')
(a / 'runtime_args.json').write_text(json.dumps({'product': template['product'], 'args': args, 'autoStopSeconds': auto_stop}, indent=2))
(a / 'PRELAUNCH.json').write_text(json.dumps({
    'plugin': {'source': str(plugin), 'sha256': sha(d / 'plugins' / 'achievetodo.jar'), 'bytes': (d / 'plugins' / 'achievetodo.jar').stat().st_size},
    'server': ident['server'], 'baseRuntime': str(base),
    'observer': {'trace-agent.jar': sha(o / 'trace-agent.jar'), 'packet-witness.jar': sha(d / 'plugins' / 'packet-witness.jar')},
    'traceTargets': TRACE_TARGETS,
}, indent=2))
if len(sys.argv) > 4:
    import campaign_modes
    print('mode', campaign_modes.set_mode(a, sys.argv[4]))
print('prepared', a, sha(d / 'plugins' / 'achievetodo.jar'))
