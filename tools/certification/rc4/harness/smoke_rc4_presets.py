"""0.4.0 stage 6 smoke (rc.4, Minecraft 26.3): one runtime per non-NORMAL preset (EASY, HARD, CHAOS, Custom vision=5),
each with a few cases in both phases. The abandoned camp is included because its threshold is new in every preset
(EASY 93, HARD 148, CHAOS seeded, Custom 121). NORMAL was covered by smoke_rc4.py.
Usage: smoke_rc4_presets.py <plugin.jar> [first-runtime-number=56]"""
import os, sys, time
from pathlib import Path

os.environ.setdefault('ATD_LAUNCH', 'release')
import campaign_final as F

ONLY = ['jump', 'open_door', 'interact_inside_abandoned_camp']
MODES = ['easy', 'hard', 'chaos', str(F.root / 'configs' / 'custom_vision.toml')]


def main(argv):
    plugin = Path(argv[0]).resolve()
    first = int(argv[1]) if len(argv) > 1 else 56
    F.STATUS = F.root / 'SMOKE_RC4_PRESETS.json'
    F.save(started=F.now(), plugin=str(plugin), state='starting', rules='rc4-smoke-presets')
    timing = {}
    try:
        for i, mode in enumerate(MODES):
            name = f'campaign{first + i:02d}'
            tag = Path(mode).stem
            start = time.time()
            F.runtime(name, plugin, mode, 2 * 3600,
                      [lambda name=name, tag=tag: F.run_pass(name, ','.join(ONLY), 'locked,allowed', 'smoke-' + tag)])
            timing[tag] = round(time.time() - start, 1)
            F.log(f'{name} {tag} took {timing[tag]} s')
            F.save(timing=timing)
        F.save(state='DONE', finished=F.now(), timing=timing)
        F.log('SMOKE_RC4_PRESETS_DONE')
        return 0
    except Exception as error:
        F.save(state='ABORTED', error=str(error), timing=timing)
        F.log(f'ABORTED: {error}')
        return 1


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
