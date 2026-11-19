"""Stage 3 final table: per-group verdicts from the r3 run (campaign11-15) and the r4 supplement (campaign16).

No verdict is rewritten here: every row keeps the verdict judge() gave it (and E6 replay reproduced).
Source rows:
  r3  campaign11 NORMAL (pass1, modes, offhand, reload), 12 EASY, 13 HARD, 14 CHAOS, 15 Custom vision=5
  r4  campaign16 CHAOS, landmark group: supersedes the campaign14 rows of the same (ability, notes, phase),
      because r4 fixed only that fixture (chest room off other locked landmark boxes).
Group status RUNTIME_PASS when, for every case of every ability in the group:
  - no row has a verdict outside OK (every PASS_* kind or NA_*), and
  - the allowed phase passed (PASS*) in every config where it ran, and
  - the locked state was proven at least once (a locked-phase PASS, PASS_NO_OVERLAY or PASS_PARITY_ENTER_BLOCK;
    PASS_MASKED_BY_DEPENDENCY does not count), unless every locked row is NA_NO_LOCKED_STATE.
Writes FINAL_GROUP_VERDICTS.json and FINAL_TABLE.md.
"""
import collections, json
from pathlib import Path

import campaign_cases

A = Path(__file__).parent
R3 = {'campaign11': 'normal', 'campaign12': 'easy', 'campaign13': 'hard', 'campaign14': 'chaos', 'campaign15': 'custom_vision'}
# Supplements: (runtime, config, tag or None) - their rows replace the r3 rows of the same config, ability, case and
# phase (and tag, when given). r4: CHAOS landmark fixture fix. r5: E5 reload pass with a real datapack reload.
# r6: all equip routes (new cases, own tag: adds rows, replaces none).
SUPPLEMENTS = [('campaign16', 'chaos', None), ('campaign17', 'normal', 'reload'), ('campaign18', 'normal', 'equip-routes')]
LOCKED_PROVEN = {'PASS', 'PASS_NO_OVERLAY', 'PASS_PARITY_ENTER_BLOCK'}
OK = LOCKED_PROVEN | {'PASS_MASKED_BY_DEPENDENCY', 'NA_NO_LOCKED_STATE', 'NA_OFFHAND_NOT_APPLICABLE'}
LOCKED = ('locked', 'offhand_locked', 'op', 'spectator')
ALLOWED = ('allowed', 'offhand_allowed', 'creative')


def load(name):
    return [json.loads(l) for l in (A / name / 'receipts' / 'campaign-results.jsonl').read_text(encoding='utf8').splitlines() if l.strip()]


def select():
    """Final rows keyed (config, tag, ability, case, phase), plus the list of superseded r3 rows."""
    rows, superseded = {}, []
    for name, cfg in R3.items():
        for r in load(name):
            r['campaign'], r['config'] = name, cfg
            rows[(cfg, r['tag'], r['ability'], r.get('notes', ''), r['phase'])] = r
    for name, cfg, tag in SUPPLEMENTS:
        if not (A / name / 'receipts' / 'campaign-results.jsonl').exists():
            continue
        for r in load(name):
            r['campaign'], r['config'] = name, cfg
            old = [k for k in rows if k[0] == cfg and k[2] == r['ability'] and k[3] == r.get('notes', '') and k[4] == r['phase']
                   and (tag is None or k[1] == tag)]
            for k in old:
                superseded.append(dict(key=list(k), was=rows[k]['verdict'], now=r['verdict'], by=name))
                del rows[k]
            rows[(cfg, r['tag'], r['ability'], r.get('notes', ''), r['phase'])] = r
    return rows, superseded


def main():
    rows, superseded = select()
    valid = {(c['ability'], c['notes']) for c in campaign_cases.CASES}
    stray = sorted({(k[2], k[3]) for k in rows if (k[2], k[3]) not in valid})
    summary, counts = collections.OrderedDict(), collections.Counter(r['verdict'] for r in rows.values())
    for g, abilities in campaign_cases.GROUPS.items():
        problems, notes = [], []
        for ability in abilities:
            cases = sorted({k[3] for k in rows if k[2] == ability})
            if not cases:
                problems.append(f'{ability}: no rows')
                continue
            for case in cases:
                mine = {k: r for k, r in rows.items() if k[2] == ability and k[3] == case}
                label = ability + (f'/{case}' if case else '')
                for k, r in mine.items():
                    if r['verdict'] not in OK:
                        problems.append(f'{label} {k[0]}:{k[1]}:{k[4]}={r["verdict"]}')
                    elif k[4] in ALLOWED and not r['verdict'].startswith('PASS') and r['verdict'] != 'NA_OFFHAND_NOT_APPLICABLE':
                        problems.append(f'{label} {k[0]}:{k[1]}:{k[4]}={r["verdict"]}')
                locked = [r['verdict'] for k, r in mine.items() if k[4] in LOCKED]
                if locked and not any(v in LOCKED_PROVEN for v in locked):
                    if all(v == 'NA_NO_LOCKED_STATE' for v in locked):
                        notes.append(f'{label}: never locked in any config (NA_NO_LOCKED_STATE)')
                    else:
                        problems.append(f'{label}: locked state never proven ({sorted(set(locked))})')
                special = sorted({f'{k[0]}:{k[4]}={r["verdict"]}' for k, r in mine.items()
                                  if r['verdict'] in ('PASS_PARITY_ENTER_BLOCK', 'PASS_MASKED_BY_DEPENDENCY', 'PASS_NO_OVERLAY')})
                if special:
                    notes.append(f'{label}: ' + ', '.join(special))
        summary[g] = dict(status='RUNTIME_PASS' if not problems else 'OPEN', abilities=len(abilities), problems=problems, notes=notes)

    result = dict(sources=dict(r3=R3, supplements=SUPPLEMENTS), rows=len(rows), verdictCounts=dict(counts), superseded=superseded, strayCases=stray,
                  statusCounts=dict(collections.Counter(v['status'] for v in summary.values())), groups=summary)
    (A / 'FINAL_GROUP_VERDICTS.json').write_text(json.dumps(result, indent=1, ensure_ascii=False), encoding='utf8')

    md = ['# W5 final group verdicts (r3 + supplements r4, r5)', '',
          f'Rows: {len(rows)}. Verdicts: ' + ', '.join(f'{k} {v}' for k, v in sorted(counts.items())), '',
          f'Groups: ' + ', '.join(f'{k} {v}' for k, v in result['statusCounts'].items()), '',
          '| Group | Abilities | Status | Notes |', '|---|---|---|---|']
    for g, v in summary.items():
        md.append(f'| {g} | {v["abilities"]} | {v["status"]} | {"; ".join(v["problems"] + v["notes"]).replace("|", "/")} |')
    (A / 'FINAL_TABLE.md').write_text('\n'.join(md) + '\n', encoding='utf8')
    print(result['statusCounts'], dict(counts))
    print('superseded', [(s['key'][2], s['key'][4], s['was'], s['now']) for s in superseded if s['was'] != s['now']])
    print('stray', stray)
    for g, v in summary.items():
        if v['status'] != 'RUNTIME_PASS':
            print(g, v['problems'][:6])


if __name__ == '__main__':
    main()
