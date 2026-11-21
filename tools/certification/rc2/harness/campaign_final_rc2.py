"""0.2.1 (rc.2) certification: the full campaign on the release build, launched the way an operator does
(prepare_campaign ATD_LAUNCH=release: only -javaagent:plugins/achievetodo.jar plus the observer trace agent).

One plan, one rule set (the r6 harness: all equip routes, landmark fixture fix, real reload):
  N    NORMAL: pass1 (all cases) -> modes (creative, op, spectator) -> offhand -> vanilla reload, generation 2
       required -> reload pass (all cases)
  N+1  EASY, N+2 HARD, N+3 CHAOS, N+4 Custom (vision=5): all cases locked/allowed
Every runtime must log ATD_LAUNCH_PROFILE=release and ATD_GAMEPLAY_MODE=CERTIFICATION (campaign_final.start_runtime).
Status in FINAL_STATUS_RC2.json.
Usage: campaign_final_rc2.py <plugin.jar> [first-runtime-number=21]"""
import os, sys
from pathlib import Path

os.environ.setdefault('ATD_LAUNCH', 'release')
import campaign_final as F
from campaign_final_r2 import runtime_with_retry
from campaign_final_r5 import reload_to_generation_2


def main(argv):
    plugin = Path(argv[0]).resolve()
    first = int(argv[1]) if len(argv) > 1 else 21
    F.STATUS = F.root / 'FINAL_STATUS_RC2.json'
    F.save(started=F.now(), plugin=str(plugin), state='starting', rules='r6', launch=os.environ['ATD_LAUNCH'])
    names = [f'campaign{i:02d}' for i in range(first, first + 5)]
    try:
        base = names[0]
        runtime_with_retry(base, plugin, None, 14 * 3600, [
            lambda: F.run_pass(base, F.ALL, 'locked,allowed', 'pass1'),
            lambda: F.run_pass(base, F.ALL, 'creative,op,spectator', 'modes'),
            lambda: F.run_pass(base, F.OFFHAND, 'offhand_locked,offhand_allowed', 'offhand'),
            lambda: reload_to_generation_2(base),
            lambda: F.run_pass(base, F.ALL, 'locked,allowed', 'reload'),
        ])
        for name, mode in zip(names[1:], ['easy', 'hard', 'chaos', str(F.root / 'configs' / 'custom_vision.toml')]):
            runtime_with_retry(name, plugin, mode, 6 * 3600,
                               [lambda name=name, mode=mode: F.run_pass(name, F.ALL, 'locked,allowed', 'thresholds-' + Path(mode).stem)])
        F.save(state='DONE', finished=F.now())
        F.log('FINAL_RC2_DONE')
    except Exception as error:
        F.save(state='ABORTED', error=str(error), aborted=F.now())
        F.log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
