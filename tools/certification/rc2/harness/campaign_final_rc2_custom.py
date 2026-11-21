"""rc.2: rerun only the last runtime, campaign25 (Custom vision=5), after its logins failed on the Mojang session
server. Same frozen harness and pass as campaign_final_rc2_resume; merges into FINAL_STATUS_RC2.json.
Usage: campaign_final_rc2_custom.py <plugin.jar>"""
import json, os, sys, time
from pathlib import Path

os.environ.setdefault('ATD_LAUNCH', 'release')
import campaign_final as F
from campaign_final_rc2_resume import runtime_with_backoff


def main(argv):
    plugin = Path(argv[0]).resolve()
    F.STATUS = F.root / 'FINAL_STATUS_RC2.json'
    F.status.clear()
    F.status.update(json.loads(F.STATUS.read_text(encoding='utf8')))
    old = F.root / 'campaign25'
    if old.exists():
        old.rename(F.root / f'campaign25-loginfail-{int(time.time())}')
    F.save(state='resuming campaign25', error=None, resumedCustom=F.now())
    try:
        runtime_with_backoff('campaign25', plugin, str(F.root / 'configs' / 'custom_vision.toml'))
        F.save(state='DONE', finished=F.now())
        F.log('FINAL_RC2_DONE')
    except Exception as error:
        F.save(state='ABORTED', error=str(error), aborted=F.now())
        F.log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
