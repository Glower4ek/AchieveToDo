"""Switch a campaign world's overworld from superflat to natural generation (landmark22 needs real biomes).

Usage: campaign_world.py <campaignNN>   (server must be stopped)
The base world is superflat (plains only, features off), so oceans, snow, deserts, jungles - and with them
shipwrecks, igloos, pyramids, monuments, dungeons and desert wells - can never generate. This rewrites
dimensions/minecraft/overworld/data/minecraft/world_gen_settings.dat: the overworld generator becomes the
vanilla noise/multi_noise overworld with the same seed. Already generated chunks (the arena at 0,0) keep
their flat terrain; only fresh chunks - where landmark_setup searches, 40000+ blocks out - generate
naturally. World identity (UUID, progression bindings) is unchanged. A backup of the original file is kept.
"""
import shutil, sys
from pathlib import Path

import nbt_min
from nbt_min import COMPOUND, STRING

root = Path(__file__).parent


def natural_overworld(a: Path):
    if (a / 'receipts' / 'output.log').exists() and not (a / 'receipts' / 'execution.json').exists():
        sys.exit('server still running: stop it first')
    f = a / 'runtime' / 'stage2b-w4-attempt01' / 'dimensions' / 'minecraft' / 'overworld' / 'data' / 'minecraft' / 'world_gen_settings.dat'
    name, root_tag = nbt_min.load(f)
    dims = root_tag['data'][1]['dimensions'][1]
    gen = dims['minecraft:overworld'][1]['generator'][1]
    if gen['type'][1] == 'minecraft:noise':
        return 'already natural'
    backup = f.with_name('world_gen_settings.flat-backup.dat')
    if not backup.exists():
        shutil.copy2(f, backup)
    dims['minecraft:overworld'][1]['generator'] = (COMPOUND, {
        'type': (STRING, 'minecraft:noise'),
        'biome_source': (COMPOUND, {'type': (STRING, 'minecraft:multi_noise'), 'preset': (STRING, 'minecraft:overworld')}),
        'settings': (STRING, 'minecraft:overworld'),
    })
    nbt_min.dump(f, name, root_tag)
    return 'natural overworld (backup: ' + backup.name + ')'


if __name__ == '__main__':
    print(natural_overworld(root / sys.argv[1]))
