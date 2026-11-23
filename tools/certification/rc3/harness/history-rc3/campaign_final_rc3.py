"""0.3.0 certification (rc.3 harness, the faster approach agreed with the owner on 2026-11-22).

Changes against the rc.2c harness:
  - Event-based synchronisation (Server.sync): a marker command confirms the server executed the setup, instead of
    fixed 2.0 s / 0.8 s settle times. Waits that cover world effects (hunger 8.5 s, fixture awards 2.5 s) stay.
  - New cases for the 0.2.1 E4 findings (shears routes, other tool types, armadillo brushing, chest/legs slots,
    off-hand boots) and for 0.3.0 changes: the shelf denial must name the shelf (overlay_text); a locked End portal
    or gateway must not grant its enter_block advancement (parity_criteria removed, any grant is FAIL_LEAK).
  - Presentation checks: chat messages of BACAP advancements and abilities must carry their hover text.
  - EASY, HARD, CHAOS and Custom run one representative case per group (--sample one-per-group). The guards are the
    same code in every preset; the per-preset thresholds are covered by the progression unit tests.
  - Launch check: ATD_GAMEPLAY_MODE=RELEASE and ATD_PRODUCT_GAMEPLAY_READY=true.
Plan: N NORMAL pass1 -> presentation -> modes -> offhand -> vanilla reload (generation 2) -> reload;
N+1..N+4 EASY, HARD, CHAOS, Custom (vision=5) sampled thresholds. Status FINAL_STATUS_RC3.json.
Usage: campaign_final_rc3.py <plugin.jar> [first-runtime-number=41] [--only-normal|--skip-normal]"""
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
    first = int(args[1]) if len(args) > 1 else 41
    F.STATUS = F.root / 'FINAL_STATUS_RC3.json'
    if F.STATUS.exists():
        import json
        F.status.update(json.loads(F.STATUS.read_text(encoding='utf8')))
    F.save(started=F.now(), plugin=str(plugin), state='starting', rules='rc3', launch=os.environ['ATD_LAUNCH'])
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
        F.log('FINAL_RC3_DONE')
    except Exception as error:
        F.save(state='ABORTED', error=str(error), aborted=F.now())
        F.log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
