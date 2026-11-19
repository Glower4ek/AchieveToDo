"""Stage 3, r3 resume: the r3 run aborted at its last runtime (campaign15, Custom vision=5) because the Mojang
session server answered 429 on both login attempts. This runs only that runtime again with the same frozen r3
harness (HARNESS_R3.sha256) and the same pass as campaign_final_r3, and merges the result into
FINAL_STATUS_R3.json. Orchestration only: no verdict rule changes.
Usage: campaign_final_r3_resume.py <plugin.jar> [cooldown-seconds=1200]"""
import json, sys, time
from pathlib import Path

import campaign_final as F
from campaign_final_r2 import runtime_with_retry

NAME = 'campaign15'


def main(argv):
    plugin = Path(argv[0]).resolve()
    cooldown = int(argv[1]) if len(argv) > 1 else 1200
    F.STATUS = F.root / 'FINAL_STATUS_R3.json'
    F.status.clear()
    F.status.update(json.loads(F.STATUS.read_text(encoding='utf8')))
    aborted = F.status['runtimes'].pop(NAME, None)
    F.status.setdefault('abortedAttempts', {})[NAME] = dict(runtime=aborted, error=F.status.get('error'), at=F.status.get('aborted'))
    old = F.root / NAME
    if old.exists():
        old.rename(F.root / f'{NAME}-aborted-login429')
    F.save(state=f'resume cooldown {cooldown}s', error=None, resumed=F.now())
    F.log(f'resume {NAME}: cooldown {cooldown}s before login')
    time.sleep(cooldown)
    mode = str(F.root / 'configs' / 'custom_vision.toml')
    try:
        runtime_with_retry(NAME, plugin, mode, 5 * 3600,
                           [lambda: F.run_pass(NAME, F.ALL, 'locked,allowed', 'thresholds-' + Path(mode).stem)])
        F.save(state='DONE', finished=F.now())
        F.log('FINAL_R3_DONE')
    except Exception as error:
        F.save(state='ABORTED', error=str(error), aborted=F.now())
        F.log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
