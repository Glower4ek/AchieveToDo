"""rc.2 round c supplement: CHAOS ignite_tnt allowed failed in campaign34 because its fixture lights the TNT with flint
and steel, whose own threshold was higher in that CHAOS world. campaign_cases now declares
requires=['use_flint_and_steel'] (allowed score raised to both thresholds; locked masked when the flint is locked too).
Runs ignite_tnt locked+allowed in a fresh CHAOS install (campaign36) with the frozen harness HARNESS_RC2D.sha256;
its rows supersede campaign34's ignite_tnt rows. Status FINAL_STATUS_RC2D.json.
Usage: campaign_final_rc2d.py <plugin.jar>"""
import os, sys
from pathlib import Path

os.environ.setdefault('ATD_LAUNCH', 'release')
import campaign_final as F
from campaign_final_rc2c import with_backoff


def main(argv):
    plugin = Path(argv[0]).resolve()
    F.STATUS = F.root / 'FINAL_STATUS_RC2D.json'
    F.save(started=F.now(), plugin=str(plugin), state='starting', rules='rc2d', launch=os.environ['ATD_LAUNCH'])
    try:
        with_backoff('campaign36', plugin, 'chaos', 3 * 3600,
                     [lambda: F.run_pass('campaign36', 'ignite_tnt', 'locked,allowed', 'thresholds-chaos')])
        F.save(state='DONE', finished=F.now())
        F.log('FINAL_RC2D_DONE')
    except Exception as error:
        F.save(state='ABORTED', error=str(error), aborted=F.now())
        F.log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
