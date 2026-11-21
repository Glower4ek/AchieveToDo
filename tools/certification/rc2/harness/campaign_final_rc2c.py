"""0.2.1 (rc.2) certification, round c: the full campaign again after the rc.2 independent review.

Review fixes in the frozen harness (HARNESS_RC2C.sha256):
  - EASY/HARD/CHAOS worlds are a fresh install started with the documented first-start option
    -Dachievetodo.progression.initialConfig=<preset> (campaign_modes); no freshConfig flag anywhere.
  - expect_hit: an aimed case whose client aimed at nothing is INVALID_FIXTURE_AIM (retried once per pass).
  - server_reach: vehicles, villagers and the wandering trader must leave a traced Player.interactOn return.
  - vehicles: server probe 'on vehicle'; villagers: server trade decision (interactOn swingSource=CLIENT).
  - put_in_bundle: the reverse route (item on cursor onto the bundle) is a case of its own.
Plan: N NORMAL pass1 -> modes -> offhand -> vanilla reload (generation 2) -> reload; N+1..N+4 EASY, HARD, CHAOS,
Custom (vision=5) thresholds. Login: up to 4 attempts per world (15, 30, 60 min). Status FINAL_STATUS_RC2C.json.
Usage: campaign_final_rc2c.py <plugin.jar> [first-runtime-number=31]"""
import os, sys
from pathlib import Path

os.environ.setdefault('ATD_LAUNCH', 'release')
import campaign_final as F
from campaign_final_rc2_resume import WAITS
from campaign_final_r5 import reload_to_generation_2
import time


def with_backoff(name, plugin, mode, auto_stop, passes):
    for attempt, wait in enumerate([0] + WAITS):
        if wait:
            F.log(f'{name}: login retry {attempt} in {wait // 60} min')
            time.sleep(wait)
            old = F.root / name
            if old.exists():
                old.rename(F.root / f'{name}-loginfail-{int(time.time())}')
        try:
            return F.runtime(name, plugin, mode, auto_stop, passes)
        except RuntimeError as error:
            if not str(error).startswith('LOGIN_FAILED'):
                raise
            F.status.setdefault('loginFailures', []).append(dict(runtime=name, at=F.now(), error=str(error)))
            F.save()
    raise RuntimeError(f'LOGIN_FAILED {name}: gave up after {len(WAITS) + 1} attempts')


def main(argv):
    plugin = Path(argv[0]).resolve()
    first = int(argv[1]) if len(argv) > 1 else 31
    F.STATUS = F.root / 'FINAL_STATUS_RC2C.json'
    F.save(started=F.now(), plugin=str(plugin), state='starting', rules='rc2c', launch=os.environ['ATD_LAUNCH'])
    names = [f'campaign{i:02d}' for i in range(first, first + 5)]
    try:
        base = names[0]
        with_backoff(base, plugin, None, 14 * 3600, [
            lambda: F.run_pass(base, F.ALL, 'locked,allowed', 'pass1'),
            lambda: F.run_pass(base, F.ALL, 'creative,op,spectator', 'modes'),
            lambda: F.run_pass(base, F.OFFHAND, 'offhand_locked,offhand_allowed', 'offhand'),
            lambda: reload_to_generation_2(base),
            lambda: F.run_pass(base, F.ALL, 'locked,allowed', 'reload'),
        ])
        for name, mode in zip(names[1:], ['easy', 'hard', 'chaos', str(F.root / 'configs' / 'custom_vision.toml')]):
            with_backoff(name, plugin, mode, 6 * 3600,
                         [lambda name=name, mode=mode: F.run_pass(name, F.ALL, 'locked,allowed', 'thresholds-' + Path(mode).stem)])
        F.save(state='DONE', finished=F.now())
        F.log('FINAL_RC2C_DONE')
    except Exception as error:
        F.save(state='ABORTED', error=str(error), aborted=F.now())
        F.log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
