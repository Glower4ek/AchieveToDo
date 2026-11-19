"""Stage 3: the whole final certification campaign in ONE process (single client session per runtime).

Usage: campaign_final.py <plugin.jar> [first-runtime-number]
Runtimes (fresh copies of test-server/base-runtime, natural overworld, online-mode, the given plugin):
  campaignNN   NORMAL: pass1 (all cases) -> modes (creative, op, spectator) -> offhand -> reload + pass1 again
  campaignNN+1 EASY, +2 HARD, +3 CHAOS, +4 Custom (configs/custom_vision.toml): all cases, locked/allowed
Each runtime: prepare -> natural world -> start server -> one authenticated client login -> passes ->
stop -> E6 replay. Progress and the outcome go to FINAL_STATUS.json; a failed login or a stopped server
ends the plan (no retry loop: repeated logins get the account throttled by Mojang, HTTP 429).
"""
import json, subprocess, sys, time, datetime
from pathlib import Path

import campaign, campaign_cases, campaign_replay, campaign_world

root = Path(__file__).parent
PRISM = r'E:\PrismLauncher\prismlauncher.exe'
INSTANCE = 'ATD Stage2B W4 Attempt01'
STATUS = root / 'FINAL_STATUS.json'
status = dict(started=None, plugin=None, runtimes={}, state='starting')

ALL = ','.join(sorted({c['ability'] for c in campaign_cases.CASES}))
OFFHAND = ','.join(sorted({c['ability'] for c in campaign_cases.CASES
                           if c['main'] != 'air' and c['ability'] != 'open_chest'
                           and not c['arena'].startswith(('landmark', 'feature'))
                           and any(a == 'click_use' or a.startswith('press key.use') for a in c['actions'])
                           and not any('slotclick' in a for a in c['actions'])}))


def now():
    return datetime.datetime.now(datetime.timezone.utc).isoformat(timespec='seconds')


def save(**kw):
    status.update(kw)
    STATUS.write_text(json.dumps(status, indent=2, ensure_ascii=False), encoding='utf8')


def log(msg):
    print(f'[{now()}] {msg}', flush=True)


def client_running():
    out = subprocess.run(['powershell', '-NoProfile', '-Command',
                          "(Get-Process javaw -ErrorAction SilentlyContinue | Measure-Object).Count"],
                         capture_output=True, text=True).stdout.strip()
    return out not in ('', '0')


def close_client():
    if not client_running():
        return
    campaign.INBOX.write_text('exit\n', encoding='utf8')
    for _ in range(60):
        if not client_running():
            return
        time.sleep(1)
    raise RuntimeError('client did not exit')


PRISM_LOG = Path.home() / 'AppData' / 'Roaming' / 'PrismLauncher' / 'logs' / 'PrismLauncher-0.log'
ACCOUNT = 'Glower4ek'


def prism_ready(timeout=180):
    """A cold `prismlauncher --launch` starts the game ~0.1 s after Prism starts, before Prism's account refresh
    (7-17 s) finishes, so the game joins with the stale Minecraft token and the session server rejects the join
    (LOGIN_FAILED). Start Prism first, wait for the account refresh, then --launch goes to the running Prism.
    A Prism that is already running keeps its accounts fresh on its own refresh schedule."""
    out = subprocess.run(['powershell', '-NoProfile', '-Command',
                          "(Get-Process prismlauncher -ErrorAction SilentlyContinue | Measure-Object).Count"],
                         capture_output=True, text=True).stdout.strip()
    if out not in ('', '0'):
        return
    started = time.time()
    subprocess.Popen([PRISM])
    for _ in range(timeout):
        time.sleep(1)
        if not PRISM_LOG.exists() or PRISM_LOG.stat().st_mtime < started:
            continue
        text = PRISM_LOG.read_text(encoding='utf8', errors='replace')
        mark = text.find(f'Processing account "{ACCOUNT}"')
        if mark < 0:
            continue
        segment = text[mark:].split('Processing account', 2)[1]   # this account's refresh only
        if 'Background account refresh succeeded' in segment:
            log(f'prism account {ACCOUNT} refreshed')
            return
        if 'Background account refresh failed' in segment:
            raise RuntimeError(f'LOGIN_FAILED prism refresh of {ACCOUNT} failed')
    raise RuntimeError(f'LOGIN_FAILED prism refresh of {ACCOUNT} timed out')


def start_runtime(name, plugin, mode, auto_stop):
    a = root / name
    args = [sys.executable, 'prepare_campaign.py', name, str(plugin), str(auto_stop)] + ([mode] if mode else [])
    subprocess.run(args, cwd=root, check=True)
    log(campaign_world.natural_overworld(a))
    subprocess.Popen([sys.executable, 'run_server.py', '.', 'runtime_args.json'], cwd=a,
                     stdout=open(a / 'server-runner.log', 'w'), stderr=subprocess.STDOUT)
    out = a / 'receipts' / 'output.log'
    for _ in range(600):
        if out.exists() and 'Done (' in out.read_text(encoding='utf8', errors='replace'):
            break
        time.sleep(1)
    else:
        raise RuntimeError('server did not start')
    text = out.read_text(encoding='utf8', errors='replace')
    config = next((l for l in text.splitlines() if 'Resolved progression config' in l), '?')
    log(f'{name} up: {config.strip()}')
    close_client()
    prism_ready()
    subprocess.Popen([PRISM, '--launch', INSTANCE, '--server', 'localhost:52704'])
    for _ in range(300):
        text = out.read_text(encoding='utf8', errors='replace')
        if 'Glower4ek joined the game' in text:
            break
        if 'Glower4ek' in text and 'lost connection' in text:
            break
        time.sleep(1)
    time.sleep(8)
    text = out.read_text(encoding='utf8', errors='replace')
    joined = 'Glower4ek joined the game' in text and 'online-mode=true' in (a / 'runtime' / 'server.properties').read_text()
    if not joined:
        client_log = campaign.CLIENT / 'logs' / 'latest.log'
        reason = [l for l in client_log.read_text(encoding='utf8', errors='replace').splitlines()
                  if 'status=' in l or 'Failed to log in' in l][-3:] if client_log.exists() else []
        raise RuntimeError(f'LOGIN_FAILED {name}: {reason}')
    return a


def stop_runtime(a):
    if not (a / 'receipts' / 'execution.json').exists():
        (a / 'receipts' / 'control.txt').write_text('stop\n', encoding='utf8')
        for _ in range(180):
            if (a / 'receipts' / 'execution.json').exists():
                break
            time.sleep(1)
    close_client()


def run_pass(name, only, phases, tag):
    log(f'{name} pass {tag} phases={phases}')
    code = campaign.main([name, '--only', only, '--phases', phases, '--tag', tag])
    rows = [json.loads(l) for l in (root / name / 'receipts' / 'campaign-results.jsonl').read_text(encoding='utf8').splitlines() if l.strip()]
    counts = {}
    for r in rows:
        if r.get('tag') == tag:
            counts[r['verdict']] = counts.get(r['verdict'], 0) + 1
    status['runtimes'][name]['passes'][tag] = dict(phases=phases, counts=counts, finished=now())
    save()
    if code == 2:
        raise RuntimeError(f'SERVER_STOPPED during {name} {tag}')


def runtime(name, plugin, mode, auto_stop, passes):
    status['runtimes'][name] = dict(mode=mode or 'normal', started=now(), passes={})
    save(state=f'running {name}')
    a = root / name
    try:
        start_runtime(name, plugin, mode, auto_stop)
        for p in passes:
            p()
    finally:
        if (a / 'receipts').exists():
            stop_runtime(a)   # also after a failed start/login: never leave the server or client running
    rc = campaign_replay.main([name])
    replay = json.loads((a / 'receipts' / 'REPLAY.json').read_text(encoding='utf8'))
    status['runtimes'][name].update(finished=now(), replay=dict(counts=replay['counts'], reproducedAll=replay['reproducedAll'], exit=rc))
    save()


def main(argv):
    plugin = Path(argv[0]).resolve()
    first = int(argv[1]) if len(argv) > 1 else 4
    save(started=now(), plugin=str(plugin), state='starting')
    names = [f'campaign{n:02d}' for n in range(first, first + 5)]
    try:
        base = names[0]
        runtime(base, plugin, None, 14 * 3600, [
            lambda: run_pass(base, ALL, 'locked,allowed', 'pass1'),
            lambda: run_pass(base, ALL, 'creative,op,spectator', 'modes'),
            lambda: run_pass(base, OFFHAND, 'offhand_locked,offhand_allowed', 'offhand'),
            lambda: (campaign.Server(root / base).send(['reload'], settle=30), log('reload sent')),
            lambda: run_pass(base, ALL, 'locked,allowed', 'reload'),
        ])
        for name, mode in zip(names[1:], ['easy', 'hard', 'chaos', str(root / 'configs' / 'custom_vision.toml')]):
            runtime(name, plugin, mode, 5 * 3600, [lambda name=name, mode=mode: run_pass(name, ALL, 'locked,allowed', 'thresholds-' + Path(mode).stem)])
        save(state='DONE', finished=now())
        log('FINAL_DONE')
    except Exception as error:
        save(state='ABORTED', error=str(error), aborted=now())
        log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
