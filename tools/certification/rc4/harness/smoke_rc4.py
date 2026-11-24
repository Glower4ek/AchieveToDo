"""0.4.0 harness smoke (rc.4, Minecraft 26.3): one NORMAL runtime, the new cases (abandoned camp, the known issue 6
audit in the igloo, the cartographer message) plus a few old ones, then the chat checks and the E6 replay.
Usage: smoke_rc4.py <plugin.jar> [runtime-name=campaign50]"""
import os, sys, time
from pathlib import Path

os.environ.setdefault('ATD_LAUNCH', 'release')
import campaign
import campaign_final as F

ONLY = ['jump', 'open_door', 'use_water_bucket', 'trade_with_cartographer', 'interact_inside_igloo',
        'interact_inside_abandoned_camp']


def main(argv):
    plugin = Path(argv[0]).resolve()
    name = argv[1] if len(argv) > 1 else 'campaign50'
    F.STATUS = F.root / f'SMOKE_RC4_{name}.json'
    F.save(started=F.now(), plugin=str(plugin), state='starting', rules='rc4-smoke')
    timing = {}

    def timed(tag, fn):
        start = time.time()
        fn()
        timing[tag] = round(time.time() - start, 1)
        F.log(f'{name} {tag} took {timing[tag]} s')

    try:
        F.runtime(name, plugin, None, 2 * 3600, [
            lambda: timed('cases', lambda: F.run_pass(name, ','.join(ONLY), 'locked,allowed', 'smoke')),
            lambda: timed('presentation', lambda: campaign.presentation(name)),
        ])
        F.save(state='DONE', finished=F.now(), timing=timing)
        F.log('SMOKE_RC4_DONE')
        return 0
    except Exception as error:
        F.save(state='ABORTED', error=str(error), timing=timing)
        F.log(f'ABORTED: {error}')
        return 1


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
