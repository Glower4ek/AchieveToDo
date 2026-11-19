"""Stage 3, round r3: the whole final campaign again with one frozen harness rule set (r3), so every verdict of
the certification table comes from one run and one rule set. Earlier runs (r1: campaign04/05, r2: 06-10) are
dry runs; their analysis is in the plan. Same plan as campaign_final.main, with one login retry per runtime.
Usage: campaign_final_r3.py <plugin.jar> [first-runtime-number=11]"""
import sys
from pathlib import Path

import campaign, campaign_final as F
from campaign_final_r2 import runtime_with_retry


def main(argv):
    plugin = Path(argv[0]).resolve()
    first = int(argv[1]) if len(argv) > 1 else 11
    F.STATUS = F.root / 'FINAL_STATUS_R3.json'
    F.save(started=F.now(), plugin=str(plugin), state='starting', rules='r3')
    names = [f'campaign{i:02d}' for i in range(first, first + 5)]
    try:
        base = names[0]
        runtime_with_retry(base, plugin, None, 14 * 3600, [
            lambda: F.run_pass(base, F.ALL, 'locked,allowed', 'pass1'),
            lambda: F.run_pass(base, F.ALL, 'creative,op,spectator', 'modes'),
            lambda: F.run_pass(base, F.OFFHAND, 'offhand_locked,offhand_allowed', 'offhand'),
            lambda: (campaign.Server(F.root / base).send(['reload confirm'], settle=30), F.log('reload sent')),
            lambda: F.run_pass(base, F.ALL, 'locked,allowed', 'reload'),
        ])
        for name, mode in zip(names[1:], ['easy', 'hard', 'chaos', str(F.root / 'configs' / 'custom_vision.toml')]):
            runtime_with_retry(name, plugin, mode, 5 * 3600,
                               [lambda name=name, mode=mode: F.run_pass(name, F.ALL, 'locked,allowed', 'thresholds-' + Path(mode).stem)])
        F.save(state='DONE', finished=F.now())
        F.log('FINAL_R3_DONE')
    except Exception as error:
        F.save(state='ABORTED', error=str(error), aborted=F.now())
        F.log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
