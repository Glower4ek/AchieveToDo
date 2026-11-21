"""rc.2 round c supplement 3 (replaces supplement 2, campaign37, rejected by the second portal review): the independent review found campaign31 'modes' enter_nether creative PASS vacuous (a
creative player crosses a portal instantly, and the fixture stood the player inside it, so the crossing happened
during the setup). Fixes in the harness (HARNESS_RC2F.sha256): the enter_nether fixture starts next to the portal
and steps in during the window; any client: oracle already true in the 'pre' sample is INVALID_FIXTURE_PRESET
(rejudging campaign31-36 with this rule changes only that one row). Runs enter_nether creative/op/spectator in a
fresh NORMAL runtime (campaign38); its rows supersede campaign31's 'modes' enter_nether rows.
campaign37 crossed with enter_nether already unlocked (vanilla story/enter_the_nether is earned on portal contact by
the guard-exempt creative player) and walked the op through the portal; now locked_margin=2 and a short step
(key.forward 3) keep the creative crossing locked and the op inside the portal for the full window.
Status FINAL_STATUS_RC2F.json. Usage: campaign_final_rc2f.py <plugin.jar>"""
import os, sys
from pathlib import Path

os.environ.setdefault('ATD_LAUNCH', 'release')
import campaign_final as F
from campaign_final_rc2c import with_backoff


def main(argv):
    plugin = Path(argv[0]).resolve()
    F.STATUS = F.root / 'FINAL_STATUS_RC2F.json'
    F.save(started=F.now(), plugin=str(plugin), state='starting', rules='rc2f', launch=os.environ['ATD_LAUNCH'])
    try:
        with_backoff('campaign38', plugin, None, 3 * 3600,
                     [lambda: F.run_pass('campaign38', 'enter_nether', 'creative,op,spectator', 'modes')])
        F.save(state='DONE', finished=F.now())
        F.log('FINAL_RC2F_DONE')
    except Exception as error:
        F.save(state='ABORTED', error=str(error), aborted=F.now())
        F.log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
