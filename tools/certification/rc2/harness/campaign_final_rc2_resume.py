"""rc.2 resume: the rc.2 campaign aborted at campaign24 (CHAOS) after two LOGIN_FAILED in a row (Mojang session
server 503). campaign21-23 (NORMAL, EASY, HARD) are complete and kept. This runs the two remaining runtimes with
the same frozen harness (HARNESS_RC2.sha256) and the same pass, and merges them into FINAL_STATUS_RC2.json.
Orchestration only: it waits longer between login attempts (15, 30, 60 min) instead of aborting after one retry.
Usage: campaign_final_rc2_resume.py <plugin.jar>"""
import json, os, sys, time
from pathlib import Path

os.environ.setdefault('ATD_LAUNCH', 'release')
import campaign_final as F

WAITS = [900, 1800, 3600]
PLAN = [('campaign24', 'chaos'), ('campaign25', str(F.root / 'configs' / 'custom_vision.toml'))]


def runtime_with_backoff(name, plugin, mode):
    passes = [lambda: F.run_pass(name, F.ALL, 'locked,allowed', 'thresholds-' + Path(mode).stem)]
    for attempt, wait in enumerate([0] + WAITS):
        if wait:
            F.log(f'{name}: login retry {attempt} in {wait // 60} min')
            time.sleep(wait)
            old = F.root / name
            if old.exists():
                old.rename(F.root / f'{name}-loginfail-{int(time.time())}')
        try:
            return F.runtime(name, plugin, mode, 6 * 3600, passes)
        except RuntimeError as error:
            if not str(error).startswith('LOGIN_FAILED'):
                raise
            F.status.setdefault('loginFailures', []).append(dict(runtime=name, at=F.now(), error=str(error)))
            F.save()
    raise RuntimeError(f'LOGIN_FAILED {name}: gave up after {len(WAITS) + 1} attempts')


def main(argv):
    plugin = Path(argv[0]).resolve()
    F.STATUS = F.root / 'FINAL_STATUS_RC2.json'
    F.status.clear()
    F.status.update(json.loads(F.STATUS.read_text(encoding='utf8')))
    F.status.setdefault('abortedAttempts', {})['campaign24'] = dict(error=F.status.get('error'), at=F.status.get('aborted'))
    old = F.root / 'campaign24'
    if old.exists():
        old.rename(F.root / f'campaign24-aborted-login503')
    F.save(state='resuming', error=None, resumed=F.now())
    try:
        for name, mode in PLAN:
            runtime_with_backoff(name, plugin, mode)
        F.save(state='DONE', finished=F.now())
        F.log('FINAL_RC2_DONE')
    except Exception as error:
        F.save(state='ABORTED', error=str(error), aborted=F.now())
        F.log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
