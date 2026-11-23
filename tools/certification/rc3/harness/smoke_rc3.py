"""0.3.0 harness smoke: one NORMAL runtime, the new cases plus a few old ones for timing, then the chat checks.
Usage: smoke_rc3.py <plugin.jar> [runtime-name=smoke41]"""
import os, sys, time
from pathlib import Path

os.environ.setdefault('ATD_LAUNCH', 'release')
import campaign
import campaign_final as F

ONLY = ['enter_nether', 'enter_end', 'teleport_outer_islands']


def main(argv):
    plugin = Path(argv[0]).resolve()
    name = argv[1] if len(argv) > 1 else 'smoke41'
    F.STATUS = F.root / f'SMOKE_RC3_{name}.json'
    F.save(started=F.now(), plugin=str(plugin), state='starting', rules='rc3-smoke')
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
        F.log('SMOKE_RC3_DONE')
        return 0
    except Exception as error:
        F.save(state='ABORTED', error=str(error), timing=timing)
        F.log(f'ABORTED: {error}')
        return 1


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
