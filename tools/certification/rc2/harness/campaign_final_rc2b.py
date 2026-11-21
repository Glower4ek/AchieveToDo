"""rc.2 review fix: campaign22-24 (EASY, HARD, CHAOS) were prepared by editing the saved selection and adding
-Dachievetodo.progression.freshConfig=true, which is not the operator launch. campaign_modes now builds those
worlds as a fresh install started with the documented first-start option
-Dachievetodo.progression.initialConfig=<preset> (STARTING_THE_SERVER.md); the plugin derives freshConfig itself.
Reruns the three threshold passes as campaign26-28 with the same frozen harness (HARNESS_RC2B.sha256); status in
FINAL_STATUS_RC2B.json. Usage: campaign_final_rc2b.py <plugin.jar>"""
import os, sys
from pathlib import Path

os.environ.setdefault('ATD_LAUNCH', 'release')
import campaign_final as F
from campaign_final_rc2_resume import runtime_with_backoff

PLAN = [('campaign26', 'easy'), ('campaign27', 'hard'), ('campaign28', 'chaos')]


def main(argv):
    plugin = Path(argv[0]).resolve()
    F.STATUS = F.root / 'FINAL_STATUS_RC2B.json'
    F.save(started=F.now(), plugin=str(plugin), state='starting', rules='rc2b', launch=os.environ['ATD_LAUNCH'])
    try:
        for name, mode in PLAN:
            runtime_with_backoff(name, plugin, mode)
        F.save(state='DONE', finished=F.now())
        F.log('FINAL_RC2B_DONE')
    except Exception as error:
        F.save(state='ABORTED', error=str(error), aborted=F.now())
        F.log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
