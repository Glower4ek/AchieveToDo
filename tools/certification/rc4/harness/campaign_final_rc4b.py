"""0.4.0 supplement (rc.4b harness): reruns the campaign51/campaign53 rows that failed for harness reasons.

  - modes / interact_inside_igloo (criteria audit, creative and op) and reload / interact_inside_igloo (audit, allowed):
    the audit room is rebuilt at the same spot for every row, and the frame, the tadpole and dropped items stayed, so a
    leftover framed map or age-locked tadpole made the next row's probe true before the action; and out of water a
    tadpole dies after 15 s of air, sometimes before the post probe. rc.4b removes them after each row and gives the
    tadpole air (campaign_cases.AUDIT_CLEANUP, Air:32767s).
  - thresholds-hard / glide_with_fireworks locked: the denial held (FireworkRocketItem.use Pass, overlay shown), but
    BACAP's tick functions updated their tallies (bacap/advancement_legend, bacap/*_milestone) inside the row's window
    after the fixture pinned score 787. No real advancement was earned. rc.4b's oracle keeps tallies alone out of the
    leak (campaign.BACAP_TALLY) and records them as 'bookkeeping'; any real advancement still fails the row.
The rerun rows supersede the rows of the same tag, ability and phase. Status FINAL_STATUS_RC4B.json.
Usage: campaign_final_rc4b.py <plugin.jar> [first-runtime-number=60]"""
import os, sys
from pathlib import Path

os.environ.setdefault('ATD_LAUNCH', 'release')
import campaign_final as F
from campaign_final_rc2c import with_backoff
from campaign_final_r5 import reload_to_generation_2


def main(argv):
    plugin = Path(argv[0]).resolve()
    first = int(argv[1]) if len(argv) > 1 else 60
    normal, hard = f'campaign{first:02d}', f'campaign{first + 1:02d}'
    F.STATUS = F.root / 'FINAL_STATUS_RC4B.json'
    F.save(started=F.now(), plugin=str(plugin), state='starting', rules='rc4b', launch=os.environ['ATD_LAUNCH'])
    try:
        with_backoff(normal, plugin, None, 2 * 3600, [
            lambda: F.run_pass(normal, 'interact_inside_igloo', 'creative,op,spectator', 'modes'),
            lambda: reload_to_generation_2(normal),
            lambda: F.run_pass(normal, 'interact_inside_igloo', 'locked,allowed', 'reload'),
        ])
        with_backoff(hard, plugin, 'hard', 2 * 3600, [
            lambda: F.run_pass(hard, 'glide_with_fireworks', 'locked,allowed', 'thresholds-hard'),
        ])
        F.save(state='DONE', finished=F.now())
        F.log('FINAL_RC4B_DONE')
    except Exception as error:
        F.save(state='ABORTED', error=str(error), aborted=F.now())
        F.log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
