"""Stage 4: one self-contained review package per group, for independent reviewers.

review/<group>/
  MANIFEST.json   artifacts and hashes (plugin, client mod, server jar, harness r3/r4, evidence files per runtime),
                  E3 evidence per runtime (online-mode, authenticated UUID line, client 'Setting user'),
                  E5 evidence (reload runtime, generation before/after), E6 replay summary per runtime,
                  the verdict glossary and the source of every row (r3 run or supplement)
  E1.json         copy of E1-final/E1_<group>.json
  rows.jsonl      the final rows of this group (campaign_report_final.select), unchanged verdicts
  replay.jsonl    the E6 replay entry of each row (recorded vs replayed verdict)
  excerpts.txt    per row: the raw server output.log lines the verdict was judged from (logLines range);
                  lines over 600 chars are compacted (state JSON reduced to count/mode/op/generation and the
                  tested ability's entry), every other byte is as logged
Usage: make_review_packages.py [out-dir=review]
"""
import hashlib, json, re, shutil, sys
from pathlib import Path

import campaign_cases
import campaign_report_final as R

A = Path(__file__).parent
REPO = A.parent.parent / 'repo'
CLIENT_MOD = Path(r'E:\Кубы сборки\ATD Stage2B W4 Attempt01\minecraft\mods\achievetodo.jar')
GLOSSARY = {
    'PASS': 'locked: action denied, no effect, overlay shown; allowed: action took effect',
    'PASS_NO_OVERLAY': 'locked: action denied and no effect, but the client overlay message was not observed',
    'PASS_PARITY_ENTER_BLOCK': 'locked portal/gateway: teleport denied; the only score change is the vanilla enter_block '
                               'advancement for touching the block, identical in the Fabric original',
    'PASS_MASKED_BY_DEPENDENCY': 'locked: denied, but a required fixture ability (row.requires) was also locked, so the '
                                 'denial may come from it; the locked state must be proven elsewhere',
    'NA_NO_LOCKED_STATE': 'threshold 0 (initially unlocked) in this config: there is no locked state to test',
    'NA_OFFHAND_NOT_APPLICABLE': 'the route has no off-hand variant (e.g. trident melee)',
}


def sha(p):
    h = hashlib.sha256()
    with open(p, 'rb') as f:
        for b in iter(lambda: f.read(1 << 20), b''):
            h.update(b)
    return h.hexdigest()


def compact(line, ability):
    if len(line) <= 600:
        return line
    m = re.search(r'ATD_STAGE2A_STATE (\{.*\})\s*$', line)
    if m:
        try:
            s = json.loads(m.group(1))
            keep = {k: s.get(k) for k in ('tick', 'count', 'mode', 'op', 'generation', 'ready', 'food', 'x', 'y', 'z')}
            keep['ability'] = {ability.upper(): (s.get('abilities') or {}).get(ability.upper())}
            return line[:m.start(1)] + json.dumps(keep, ensure_ascii=False) + '  [compacted]'
        except ValueError:
            pass
    return line[:600] + f'  [truncated, {len(line)} chars]'


def runtime_evidence(name):
    rec = A / name / 'receipts'
    out = (rec / 'output.log').read_text(encoding='utf8', errors='replace')
    props = (A / name / 'runtime' / 'server.properties').read_text(encoding='utf8')
    uuid = next((l for l in out.splitlines() if 'UUID of player Glower4ek' in l), None)
    joined = next((l for l in out.splitlines() if 'Glower4ek joined the game' in l), None)
    gens = re.findall(r'preflight verified for native world discovery; generation=(\d+)', out)
    replay = json.loads((rec / 'REPLAY.json').read_text(encoding='utf8'))
    return dict(onlineMode='online-mode=true' in props, authenticatedUuidLine=uuid, joinedLine=joined,
                configResolved=next((l for l in out.splitlines() if 'Resolved progression config' in l), None),
                generations=[int(g) for g in gens], evidenceHashes=json.loads((rec / 'EVIDENCE_HASHES.json').read_text(encoding='utf8')),
                prelaunch=json.loads((A / name / 'PRELAUNCH.json').read_text(encoding='utf8')),
                replay=dict(counts=replay['counts'], reproducedAll=replay['reproducedAll'], problems=replay['problems']))


def main(argv):
    out_dir = A / (argv[0] if argv else 'review')
    rows, superseded = R.select()
    runtimes = sorted({r['campaign'] for r in rows.values()})
    evidence = {n: runtime_evidence(n) for n in runtimes}
    replay_rows = {}
    for n in runtimes:
        detail = json.loads((A / n / 'receipts' / 'REPLAY.json').read_text(encoding='utf8'))['rowsDetail']
        for d in detail:
            replay_rows[(n, d['line'])] = d
    lines_of = {}
    line_no = {}
    for n in runtimes:
        res = (A / n / 'receipts' / 'campaign-results.jsonl').read_text(encoding='utf8').splitlines()
        for i, l in enumerate(res, 1):
            if l.strip():
                r = json.loads(l)
                line_no[(n, r['tag'], r['ability'], r.get('notes', ''), r['phase'])] = i
    status = {n: json.loads((A / n / 'PRELAUNCH.json').read_text(encoding='utf8'))['plugin']['sha256'] for n in runtimes}
    harness = {f: (A / f).read_text(encoding='utf8') for f in ('HARNESS_R3.sha256', 'HARNESS_R4.sha256', 'HARNESS_R6.sha256')}
    r5 = A / 'FINAL_STATUS_R5.json'
    e5 = json.loads(r5.read_text(encoding='utf8')).get('runtimes', {}).get('campaign17', {}).get('reload') if r5.exists() else None
    common = dict(
        plugin=dict(sha256=sha(REPO / 'port' / 'AchieveToDo-Purpur-26.2' / 'build' / 'libs' / 'AchieveToDo-Purpur-26.2-0.2.1-rc.1.jar'),
                    perRuntime=status),
        clientMod=dict(path=str(CLIENT_MOD), sha256=sha(CLIENT_MOD)),
        harness=harness, glossary=GLOSSARY, e5Reload=dict(runtime='campaign17', evidence=e5),
        runtimes={n: dict(config=next(r['config'] for r in rows.values() if r['campaign'] == n), **evidence[n]) for n in runtimes},
        supplements=R.SUPPLEMENTS, r3=R.R3)
    if out_dir.exists():
        shutil.rmtree(out_dir)
    logs = {}
    for g, abilities in campaign_cases.GROUPS.items():
        d = out_dir / g
        d.mkdir(parents=True)
        mine = sorted(((k, r) for k, r in rows.items() if k[2] in abilities), key=lambda kr: (kr[1]['campaign'], kr[1].get('logLines') or [0]))
        shutil.copy(A / 'E1-final' / f'E1_{g}.json', d / 'E1.json')
        (d / 'rows.jsonl').write_text(''.join(json.dumps(dict(r, finalKey=list(k)), ensure_ascii=False) + '\n' for k, r in mine), encoding='utf8')
        rep, ex = [], []
        for k, r in mine:
            n = r['campaign']
            ln = line_no.get((n, r['tag'], r['ability'], r.get('notes', ''), r['phase']))
            rep.append(dict(replay_rows.get((n, ln), {'status': 'MISSING'}), campaign=n))
            if n not in logs:
                logs[n] = (A / n / 'receipts' / 'output.log').read_text(encoding='utf8', errors='replace').splitlines()
            a, b = r.get('logLines') or (0, 0)
            ex.append(f'===== {n} {r["tag"]} {r["ability"]}{"/" + r["notes"] if r.get("notes") else ""} {r["phase"]} '
                      f'score={r.get("score")} threshold={r.get("threshold")} verdict={r["verdict"]} output.log lines {a + 1}-{b}')
            ex.extend(compact(l, r['ability']) for l in logs[n][a:b])
        (d / 'replay.jsonl').write_text(''.join(json.dumps(x, ensure_ascii=False) + '\n' for x in rep), encoding='utf8')
        (d / 'excerpts.txt').write_text('\n'.join(ex) + '\n', encoding='utf8')
        sup = [s for s in superseded if s['key'][2] in abilities]
        (d / 'MANIFEST.json').write_text(json.dumps(dict(group=g, abilities=list(abilities), rows=len(mine), superseded=sup, **common),
                                                    indent=1, ensure_ascii=False), encoding='utf8')
    print(f'{len(campaign_cases.GROUPS)} packages in {out_dir}')


if __name__ == '__main__':
    main(sys.argv[1:])
