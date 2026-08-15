#!/usr/bin/env python3
"""Merge a fix-chunk workflow's journal into branch state. Usage: merge-chunk.py <chunk_num> <workflow_run_dir>"""
import json, os, sys
chunk, wd = int(sys.argv[1]), sys.argv[2]
rows = []
for l in open(os.path.join(wd, 'journal.jsonl')):
    try: j = json.loads(l)
    except: continue
    if j.get('type') == 'result':
        r = j.get('result')
        if isinstance(r, str):
            try: r = json.loads(r)
            except: pass
        if isinstance(r, dict) and 'issue' in r: rows.append(r)
res = {
 'chunk': chunk,
 'fixed': sorted(r['issue'] for r in rows),
 'flagged': [{'issue': r['issue'], 'ok': r['ok'], 'edited': r['edited'], 'note': r['note']} for r in rows if not r.get('ok') or not r.get('edited')],
 'superseded_comments': [{'issue': r['issue'], 'comment_id': c} for r in rows for c in (r.get('superseded_comment_ids') or [])],
 'other_issue_edits': [dict(e, **{'from': r['issue']}) for r in rows for e in (r.get('other_issue_edits') or [])],
}
json.dump(res, open(f'workflow-state/chunk-results/chunk-{chunk:02d}.json', 'w'), indent=1)
st = json.load(open('workflow-state/fix-status.json'))
st['fixed'] = sorted(set(st['fixed']) | set(res['fixed']))
st['pending'] = [x for x in st['pending'] if x not in set(res['fixed'])]
json.dump(st, open('workflow-state/fix-status.json', 'w'), indent=1)
print(f"chunk {chunk}: {len(res['fixed'])} fixed | total {len(st['fixed'])} fixed, {len(st['pending'])} pending | edges {len(res['other_issue_edits'])}, sup-comments {len(res['superseded_comments'])}, flagged {len(res['flagged'])}")
