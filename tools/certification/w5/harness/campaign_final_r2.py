"""Stage 3, round 2: harness rule set r2 after the first final run (campaign04 NORMAL, campaign05 EASY).

r2 fixes (harness only, product unchanged): creative 'inv' oracle -> minecraft.used statistic / case oracle,
hidden (no-display) advancements are not leaks, drift rule only in locked phases, locked_margin=2 for
enter_end / teleport_outer_islands (their own enter_block advancement lands on the threshold), off-hand
melee route NA, replay keys routes by notes.

Usage: campaign_final_r2.py <plugin.jar> [first-runtime-number=6]
  N    NORMAL supplement: modes (all), offhand (trident), portals locked/allowed, reload, portals again
  N+1  EASY supplement: portals locked/allowed
  N+2  HARD, N+3 CHAOS, N+4 Custom (vision=5): all cases locked/allowed
Rows of campaign04/05 that r2 supersedes are listed by REJUDGE.json there; the final table takes, per
(config, ability, route, phase), the r2 row when one exists.
"""
import sys, time
from pathlib import Path

import campaign, campaign_final as F

PORTALS = 'enter_end,teleport_outer_islands'


def runtime_with_retry(name, plugin, mode, auto_stop, passes):
    try:
        F.runtime(name, plugin, mode, auto_stop, passes)
    except RuntimeError as error:
        if not str(error).startswith('LOGIN_FAILED'):
            raise
        F.log(f'{error}; one retry in 15 min')
        F.status['runtimes'][name]['firstAttempt'] = str(error)
        F.save()
        time.sleep(900)
        old = F.root / name
        old.rename(F.root / f'{name}-loginfail-{int(time.time())}')
        F.runtime(name, plugin, mode, auto_stop, passes)


def main(argv):
    plugin = Path(argv[0]).resolve()
    first = int(argv[1]) if len(argv) > 1 else 6
    F.STATUS = F.root / 'FINAL_STATUS_R2.json'
    F.save(started=F.now(), plugin=str(plugin), state='starting', rules='r2')
    n = [f'campaign{i:02d}' for i in range(first, first + 5)]
    try:
        normal = n[0]
        runtime_with_retry(normal, plugin, None, 4 * 3600, [
            lambda: F.run_pass(normal, F.ALL, 'creative,op,spectator', 'modes-r2'),
            lambda: F.run_pass(normal, 'attack_with_trident', 'offhand_locked,offhand_allowed', 'offhand-r2'),
            lambda: F.run_pass(normal, PORTALS, 'locked,allowed', 'portals-r2'),
            lambda: (campaign.Server(F.root / normal).send(['reload confirm'], settle=30), F.log('reload sent')),
            lambda: F.run_pass(normal, PORTALS, 'locked,allowed', 'portals-reload-r2'),
        ])
        runtime_with_retry(n[1], plugin, 'easy', 3600, [
            lambda: F.run_pass(n[1], PORTALS, 'locked,allowed', 'portals-easy-r2')])
        for name, mode in zip(n[2:], ['hard', 'chaos', str(F.root / 'configs' / 'custom_vision.toml')]):
            runtime_with_retry(name, plugin, mode, 5 * 3600,
                               [lambda name=name, mode=mode: F.run_pass(name, F.ALL, 'locked,allowed', 'thresholds-' + Path(mode).stem)])
        F.save(state='DONE', finished=F.now())
        F.log('FINAL_R2_DONE')
    except Exception as error:
        F.save(state='ABORTED', error=str(error), aborted=F.now())
        F.log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
