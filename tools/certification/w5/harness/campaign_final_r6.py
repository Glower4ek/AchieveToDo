"""Stage 4 finding (R2, E4): equipment was runtime-proven only through right-click into the head slot. r6 adds
every route the native equipment guard covers (armor-slot place, shift-click, hotbar swap into the armor slot,
dispenser equip onto the player) and a second slot (boots via right-click) for all nine EQUIP_* abilities.
Runs a fresh NORMAL runtime, locked+allowed, tag 'equip-routes'. Harness: HARNESS_R6.sha256 (campaign_cases.py r6).
Status in FINAL_STATUS_R6.json.
Usage: campaign_final_r6.py <plugin.jar> [runtime-number=18]"""
import sys
from pathlib import Path

import campaign_cases, campaign_final as F
from campaign_final_r2 import runtime_with_retry

EQUIP = ','.join(sorted({c['ability'] for c in campaign_cases.CASES if c['ability'].startswith('equip_')}))


def main(argv):
    plugin = Path(argv[0]).resolve()
    name = f'campaign{int(argv[1]) if len(argv) > 1 else 18:02d}'
    F.STATUS = F.root / 'FINAL_STATUS_R6.json'
    F.save(started=F.now(), plugin=str(plugin), state='starting', rules='r6')
    try:
        runtime_with_retry(name, plugin, None, 3 * 3600, [lambda: F.run_pass(name, EQUIP, 'locked,allowed', 'equip-routes')])
        F.save(state='DONE', finished=F.now())
        F.log('FINAL_R6_DONE')
    except Exception as error:
        F.save(state='ABORTED', error=str(error), aborted=F.now())
        F.log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
