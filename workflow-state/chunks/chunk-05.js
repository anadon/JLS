export const meta = {
  name: 'fix-chunk-05',
  description: 'Fix chunk 05/9: apply reviews to issues 774-796 (23 issues)',
  phases: [{ title: 'Fix', detail: 'issues 774-796', model: 'sonnet' }],
}

const ISSUES = [774, 775, 776, 777, 778, 779, 780, 781, 782, 783, 784, 785, 786, 787, 788, 789, 790, 791, 792, 793, 794, 795, 796]
const DIR = '/home/user/JLS/issue-reviews'

const FIX_SCHEMA = {
  type: 'object',
  properties: {
    issue: { type: 'integer' },
    ok: { type: 'boolean' },
    edited: { type: 'boolean' },
    verdicts: { type: 'string' },
    purged: { type: 'string' },
    superseded_comment_ids: { type: 'array', items: { type: 'integer' } },
    other_issue_edits: { type: 'array', items: { type: 'object', properties: {
      target: { type: 'integer' }, change: { type: 'string' }, reason: { type: 'string' } },
      required: ['target', 'change', 'reason'], additionalProperties: false } },
    note: { type: 'string' },
  },
  required: ['issue', 'ok', 'edited', 'verdicts', 'purged', 'superseded_comment_ids', 'other_issue_edits', 'note'],
  additionalProperties: false,
}

const fixPrompt = (n) => {
  const id = String(n).padStart(4, '0')
  return `You are fixing GitHub issue #${n} in anadon/jls according to two completed reviews of it. Work autonomously; the repo checkout is at /home/user/JLS.

Inputs, in order:
1. Read ${DIR}/issue-${id}.adversarial.finished.md (skeptic lens: verified factual defects) and ${DIR}/issue-${id}.visionary.finished.md (direction lens: reframings and redirections).
2. Load GitHub tools via ToolSearch query "select:mcp__github__issue_read,mcp__github__issue_write". Fetch the live issue with issue_read method "get" and full comments with method "get_comments" (owner "anadon", repo "jls", perPage 100, paginate until exhausted).

Then rewrite the issue with issue_write (method "update"):
- Verify before applying: the reviews are dated 2026-08-08. If the live issue changed since and a finding no longer reproduces, skip that finding (say so in note).
- Apply the adversarial review's defect fixes: stale machine-readable header fields, internal contradictions, false claims, miscounted rosters, and gameable or vague acceptance criteria (tighten them so they cannot be satisfied vacuously).
- Apply the visionary review in full: fold endorse-with-reframing reframings into the body; for rethink/redirect verdicts, restructure the issue around the review's proposed direction (goal, decomposition, acceptance criteria). If either review argues for closing, merging, or splitting the issue, do NOT close it and do NOT create new issues — instead open the body with a short "## Proposed disposition" section describing the recommendation, and write the rest per the redirected scope.
- Purge in-content history: delete changelog / REPLAN / absorption / edit-log sections and any narration of how the issue evolved. The body must state only current intent, self-contained: fold anything still load-bearing that today lives only in the comment thread into the body, so no reader (human or agent) ever needs the comments.
- Dependency edges: you may correct THIS issue's own machine-block blocked_by/blocks lists, but never edit any other issue — report every change some OTHER issue needs (e.g. the mirror half of an edge) in other_issue_edits instead.
- Preserve repo conventions: keep the machine-readable header block format used by sibling issues and the issue's designator (TASK-NNNN / FEAT-NNN / CAP-NN). Change the title only if a review found it wrong or misleading.

Comments: the available tools cannot delete or edit comments, and you must not post any comment. Instead return the numeric IDs of comments that your rewritten body fully supersedes (bookkeeping, REPLAN logs, discussions you absorbed) in superseded_comment_ids so the caller can purge them later with authorized tooling. If a comment holds standalone content you could not fold in, fold it in rather than leaving it out.

If a write fails with a rate-limit error (403/429), retry up to 4 times, doing verification reads between attempts to space them out.

Return via StructuredOutput: issue=${n}; ok (false only if blocked or conflicted — explain in note); edited (did you actually write the issue); verdicts="<adversarial-verdict>/<visionary-verdict>"; purged=one sentence on what history was removed; superseded_comment_ids; other_issue_edits=[{target, change, reason}]; note=1-2 sentences on the substance of your rewrite.`
}

phase('Fix')
const results = await parallel(ISSUES.map(n => () => agent(fixPrompt(n), { label: `fix:${n}`, phase: 'Fix', model: 'sonnet', schema: FIX_SCHEMA })))
const done = results.filter(Boolean)
return {
  chunk: 5,
  fixed: done.map(r => r.issue),
  missing: ISSUES.filter(x => !done.some(r => r.issue === x)),
  flagged: done.filter(r => !r.ok || !r.edited).map(r => ({ issue: r.issue, ok: r.ok, edited: r.edited, note: r.note })),
  superseded_comments: done.flatMap(r => (r.superseded_comment_ids || []).map(cid => ({ issue: r.issue, comment_id: cid }))),
  other_issue_edits: done.flatMap(r => (r.other_issue_edits || []).map(e => ({ from: r.issue, ...e }))),
}
