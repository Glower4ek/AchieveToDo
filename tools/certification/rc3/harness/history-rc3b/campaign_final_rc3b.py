"""0.3.0 supplement (rc.3b harness): reruns the four campaign41 rows that failed for harness reasons.

  - modes / use_iron_tools creative: a creative player cannot break blocks with a sword (vanilla); the sword
    route is NA_VANILLA_NO_CREATIVE_ROUTE in creative (case flag creative=False).
  - offhand / use_shears: the wolf armor route (an empty main hand sits the own wolf first) and the cobweb mining
    route (mining uses the main hand) have no off-hand meaning (offhand=False).
  - reload / use_water_bucket locked: the client briefly predicted the use with a stale count. rc.3 waited only for
    the server after pinning the score; rc.3b also waits until the bridge has sent that count (Server.await_count).
The rerun rows supersede the campaign41 rows of the same tag, ability and phase. Status FINAL_STATUS_RC3B.json.
Usage: campaign_final_rc3b.py <plugin.jar> [runtime-number=46]"""
import os, sys
from pathlib import Path

os.environ.setdefault('ATD_LAUNCH', 'release')
import campaign_final as F
from campaign_final_rc2c import with_backoff
from campaign_final_r5 import reload_to_generation_2


def main(argv):
    plugin = Path(argv[0]).resolve()
    name = f'campaign{int(argv[1]) if len(argv) > 1 else 46:02d}'
    F.STATUS = F.root / 'FINAL_STATUS_RC3B.json'
    F.save(started=F.now(), plugin=str(plugin), state='starting', rules='rc3b', launch=os.environ['ATD_LAUNCH'])
    try:
        with_backoff(name, plugin, None, 2 * 3600, [
            lambda: F.run_pass(name, 'use_iron_tools', 'creative,op,spectator', 'modes'),
            lambda: F.run_pass(name, 'use_shears', 'offhand_locked,offhand_allowed', 'offhand'),
            lambda: reload_to_generation_2(name),
            lambda: F.run_pass(name, 'use_water_bucket', 'locked,allowed', 'reload'),
        ])
        F.save(state='DONE', finished=F.now())
        F.log('FINAL_RC3B_DONE')
    except Exception as error:
        F.save(state='ABORTED', error=str(error), aborted=F.now())
        F.log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
