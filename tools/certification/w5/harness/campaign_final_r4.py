"""Stage 3, r4 supplement: one fixture fix after r3 (campaign.landmark_setup moves the chest room off boxes of
other locked landmark types; CHAOS r3 trial_chambers allowed failed because its chest was inside a still-locked
mineshaft). Verdict rules (judge/evaluate) are unchanged. Re-runs the whole landmark group (22 structure and
feature cases) in a fresh CHAOS runtime, locked+allowed; status in FINAL_STATUS_R4.json.
Usage: campaign_final_r4.py <plugin.jar> [runtime-number=16]"""
import sys
from pathlib import Path

import campaign_cases, campaign_final as F
from campaign_final_r2 import runtime_with_retry

LANDMARKS = ','.join(sorted({c['ability'] for c in campaign_cases.CASES if c['group'].startswith('landmark22')}))


def main(argv):
    plugin = Path(argv[0]).resolve()
    name = f'campaign{int(argv[1]) if len(argv) > 1 else 16:02d}'
    F.STATUS = F.root / 'FINAL_STATUS_R4.json'
    F.save(started=F.now(), plugin=str(plugin), state='starting', rules='r4')
    try:
        runtime_with_retry(name, plugin, 'chaos', 3 * 3600,
                           [lambda: F.run_pass(name, LANDMARKS, 'locked,allowed', 'landmarks-chaos-r4')])
        F.save(state='DONE', finished=F.now())
        F.log('FINAL_R4_DONE')
    except Exception as error:
        F.save(state='ABORTED', error=str(error), aborted=F.now())
        F.log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
