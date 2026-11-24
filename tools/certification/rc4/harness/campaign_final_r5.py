"""Stage 4 finding, E5: the r3 'reload' pass never reloaded. campaign_final sent 'reload confirm' (Bukkit
syntax); on Purpur 26.2 the console dispatches 'reload' to the vanilla datapack reload, which takes no argument,
so the server answered 'Incorrect argument for command' and the config generation stayed 1 for the whole pass.

This supplement runs a fresh NORMAL runtime, sends a plain vanilla 'reload', requires the plugin's preflight line
'generation=2' (BacapPacks.beginDiscovery after a datapack reload) and only then runs every case locked+allowed
under tag 'reload'. Harness files are the r4 set (HARNESS_R4.sha256); verdict rules unchanged.
Status in FINAL_STATUS_R5.json.
Usage: campaign_final_r5.py <plugin.jar> [runtime-number=17]"""
import re, sys, time
from pathlib import Path

import campaign, campaign_final as F
from campaign_final_r2 import runtime_with_retry

GEN = re.compile(r'preflight verified for native world discovery; generation=(\d+)')


def reload_to_generation_2(name):
    out = F.root / name / 'receipts' / 'output.log'
    before = out.read_text(encoding='utf8', errors='replace')
    gens = [int(g) for g in GEN.findall(before)]
    if gens != [1]:
        raise RuntimeError(f'E5 precondition: expected generation [1] before reload, got {gens}')
    campaign.Server(F.root / name).send(['reload'], settle=5)
    for _ in range(180):
        text = out.read_text(encoding='utf8', errors='replace')[len(before):]
        if 'Incorrect argument' in text or 'Unknown or incomplete command' in text:
            raise RuntimeError('E5: reload command rejected')
        new = [int(g) for g in GEN.findall(text)]
        if new:
            if new[-1] != 2:
                raise RuntimeError(f'E5: generation after reload is {new}')
            F.status['runtimes'][name]['reload'] = dict(generationBefore=1, generationAfter=new[-1], at=F.now())
            F.save()
            F.log(f'reload done: generation {new[-1]}')
            time.sleep(20)   # let the progression runtime revalidate before the first case
            return
        time.sleep(1)
    raise RuntimeError('E5: no generation line within 180 s after reload')


def main(argv):
    plugin = Path(argv[0]).resolve()
    name = f'campaign{int(argv[1]) if len(argv) > 1 else 17:02d}'
    F.STATUS = F.root / 'FINAL_STATUS_R5.json'
    F.save(started=F.now(), plugin=str(plugin), state='starting', rules='r4 harness, r5 reload orchestration')
    try:
        runtime_with_retry(name, plugin, None, 4 * 3600, [
            lambda: reload_to_generation_2(name),
            lambda: F.run_pass(name, F.ALL, 'locked,allowed', 'reload'),
        ])
        F.save(state='DONE', finished=F.now())
        F.log('FINAL_R5_DONE')
    except Exception as error:
        F.save(state='ABORTED', error=str(error), aborted=F.now())
        F.log(f'ABORTED: {error}')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
