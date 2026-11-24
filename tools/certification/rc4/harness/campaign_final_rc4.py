"""0.4.0 certification on Minecraft 26.3 (rc.4 harness): same plan and oracle as rc.3/rc.3b.

Changes against rc.3b:
  - Runtime: test-server/base-runtime-26.3 (Purpur 26.3 build 2660, BACAP 1.21.1 world packs), identity in
    RC4_ARTIFACT_IDENTITY.json, run_server_v4.py; client: Prism instance "ATD 0.4.0 Cert 26.3" with the input driver
    built for 26.3 (driver-mod/atd-input-driver-26.3.jar).
  - New cases: interact_inside_abandoned_camp (26.3 structure, located by the #minecraft:abandoned_camp tag) and the
    known issue 6 audit (map into an item frame, golden dandelion on a tadpole inside a locked igloo: any criterion is
    FAIL_LEAK). The cartographer row must now show its lock message (known issue 5).
  - hidden_advancements.json rebuilt from the 26.3 world packs (tooling/harness-rc4/hidden_advancements.py).
Plan: N NORMAL pass1 -> presentation -> modes -> offhand -> vanilla reload (generation 2) -> reload;
N+1..N+4 EASY, HARD, CHAOS, Custom (vision=5) sampled thresholds. Status FINAL_STATUS_RC4.json.
Usage: campaign_final_rc4.py <plugin.jar> [first-runtime-number=51] [--only-normal|--skip-normal]"""
import os, sys
from pathlib import Path

os.environ.setdefault('ATD_LAUNCH', 'release')
import campaign
import campaign_final as F
from campaign_final_rc2c import with_backoff
from campaign_final_r5 import reload_to_generation_2


def presentation(name):
    F.log(f'{name} presentation checks')
    rows = campaign.presentation(name)
    counts = {}
    for r in rows:
        counts[r['verdict']] = counts.get(r['verdict'], 0) + 1
    F.status['runtimes'][name]['passes']['presentation'] = dict(counts=counts, finished=F.now())
    F.save()


def main(argv):
    flags = {a for a in argv if a.startswith('--')}
    args = [a for a in argv if not a.startswith('--')]
    plugin = Path(args[0]).resolve()
    first = int(args[1]) if len(args) > 1 else 51
    F.STATUS = F.root / 'FINAL_STATUS_RC4.json'
    if F.STATUS.exists():
        import json
        F.status.update(json.loads(F.STATUS.read_text(encoding='utf8')))
    F.save(started=F.now(), plugin=str(plugin), state='starting', rules='rc4', launch=os.environ['ATD_LAUNCH'])
    names = [f'campaign{i:02d}' for i in range(first, first + 5)]
    try:
        base = names[0]
        if '--skip-normal' not in flags:
            with_backoff(base, plugin, None, 10 * 3600, [
                lambda: F.run_pass(base, F.ALL, 'locked,allowed', 'pass1'),
                lambda: presentation(base),
                lambda: F.run_pass(base, F.ALL, 'creative,op,spectator', 'modes'),
                lambda: F.run_pass(base, F.OFFHAND, 'offhand_locked,offhand_allowed', 'offhand'),
                lambda: reload_to_generation_2(base),
                lambda: F.run_pass(base, F.ALL, 'locked,allowed', 'reload'),
            ])
        if '--only-normal' not in flags:
            for name, mode in zip(names[1:], ['easy', 'hard', 'chaos', str(F.root / 'configs' / 'custom_vision.toml')]):
                with_backoff(name, plugin, mode, 3 * 3600,
                             [lambda name=name, mode=mode: F.run_pass(name, F.ALL, 'locked,allowed', 'thresholds-' + Path(mode).stem,
                                                                      ('--sample', 'one-per-group'))])
        F.save(state='DONE', finished=F.now())
        F.log('FINAL_RC4_DONE')
    except Exception as error:
        F.save(state='ABORTED', error=str(error), aborted=F.now())
        F.log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
