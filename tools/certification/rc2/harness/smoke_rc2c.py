"""Live smoke of the rc.2 review fixes (aim rule, server_reach trace, reverse bundle route) on one NORMAL runtime."""
import os, sys
from pathlib import Path
os.environ.setdefault('ATD_LAUNCH', 'release')
import campaign_final as F

name = 'attempt23'
F.STATUS = F.root / 'SMOKE_RC2C.json'
plugin = Path(sys.argv[1]).resolve()
only = 'enter_nether'
F.runtime(name, plugin, None, 3600, [lambda: F.run_pass(name, only, 'creative,op,spectator,locked', 'smoke')])
print('SMOKE_DONE')
