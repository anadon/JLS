export const meta = {
  name: 'fix-issues-per-reviews',
  description: 'One Sonnet agent per issue applies both review lenses to the live GitHub issue and purges in-content history; a reconcile pass applies cross-issue edge fixes',
  phases: [
    { title: 'Fix', detail: 'rewrite each issue per its two reviews, purge in-body history', model: 'sonnet' },
    { title: 'Reconcile', detail: 'grouped cross-issue dependency-edge edits', model: 'sonnet' },
  ],
}

const ISSUES = [61, 62, 63, 73, 75, 76, 78, 82, 84, 91, 101, 111, 134, 162, 163, 167, 168, 169, 170, 171, 184, 185, 188, 191, 202, 212, 214, 223, 224, 232, 264, 265, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 296, 297, 298, 300, 301, 302, 304, 305, 306, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 319, 321, 322, 323, 324, 325, 327, 328, 329, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 343, 344, 345, 346, 347, 349, 350, 351, 353, 354, 355, 356, 357, 358, 359, 361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 386, 387, 388, 389, 390, 391, 392, 393, 394, 395, 396, 397, 398, 400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 413, 414, 415, 416, 417, 418, 419, 420, 421, 422, 423, 424, 425, 426, 427, 428, 429, 430, 432, 434, 435, 436, 437, 438, 439, 440, 442, 443, 444, 445, 446, 447, 448, 449, 450, 451, 452, 453, 454, 455, 456, 457, 458, 459, 460, 461, 462, 463, 464, 465, 466, 467, 468, 469, 470, 471, 472, 473, 476, 477, 478, 479, 480, 481, 482, 483, 484, 485, 486, 487, 488, 489, 490, 491, 492, 493, 494, 495, 496, 497, 498, 499, 502, 504, 505, 506, 507, 508, 509, 510, 511, 512, 513, 514, 515, 516, 517, 518, 519, 520, 521, 522, 523, 524, 525, 526, 527, 528, 529, 530, 531, 532, 533, 534, 535, 536, 537, 538, 539, 540, 541, 542, 543, 544, 545, 546, 547, 548, 549, 550, 551, 552, 553, 554, 555, 556, 557, 558, 559, 560, 561, 562, 563, 564, 565, 566, 567, 568, 569, 570, 571, 572, 573, 574, 575, 576, 577, 578, 579, 580, 581, 582, 583, 584, 585, 586, 587, 588, 589, 590, 591, 592, 593, 594, 595, 596, 597, 598, 599, 600, 601, 602, 603, 604, 605, 606, 607, 608, 609, 610, 611, 612, 613, 614, 615, 616, 617, 618, 619, 620, 621, 622, 623, 624, 625, 626, 627, 628, 629, 630, 631, 632, 633, 634, 635, 636, 637, 638, 639, 640, 641, 642, 643, 644, 645, 646, 647, 648, 649, 650, 651, 652, 653, 654, 655, 656, 657, 658, 659, 660, 665, 666, 671, 672, 673, 674, 675, 676, 677, 678, 679, 680, 681, 682, 683, 684, 685, 686, 687, 688, 689, 690, 691, 692, 693, 694, 695, 696, 697, 698, 699, 700, 701, 702, 703, 704, 705, 706, 707, 708, 709, 710, 711, 712, 713, 714, 715, 716, 717, 718, 719, 720, 721, 722, 723, 724, 725, 726, 728, 729, 730, 731, 732, 733, 734, 735, 737, 738, 739, 740, 741, 742, 743, 744, 745, 746, 747, 748, 749, 750, 751, 752, 753, 754, 755, 756, 757, 758, 759, 760, 761, 762, 763, 764, 765, 766, 767, 768, 769, 770, 771, 772, 773, 774, 775, 776, 777, 778, 779, 780, 781, 782, 783, 784, 785, 786, 787, 788, 789, 790, 791, 792, 793, 794, 795, 796, 797, 798, 799, 800, 801, 802, 803, 804, 805, 806, 807, 808, 809, 810, 811, 812, 813, 814, 815, 816, 817, 818, 819, 820, 821, 822, 823, 824, 825, 826, 827, 828, 829, 830, 831, 832, 833, 834, 835, 836, 837, 838, 839, 840, 841, 842, 843, 844, 845, 846, 847, 848, 849, 850, 851, 852, 853, 854, 855, 856, 857, 858, 859, 860, 861, 862, 863, 864, 865, 866, 867, 868, 872, 873, 874, 875, 876, 877, 878, 879, 880, 882, 883, 884, 885, 886, 888, 889]
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

const byTarget = {}
for (const r of done) for (const e of (r.other_issue_edits || [])) {
  const t = e.target
  if (!byTarget[t]) byTarget[t] = []
  byTarget[t].push({ requested_by: r.issue, change: e.change, reason: e.reason })
}
const targets = Object.keys(byTarget).map(Number).sort((a, b) => a - b)
log(`Fix phase: ${done.length}/${ISSUES.length} issues processed; ${targets.length} issues need cross-issue edge reconciliation`)

const REC_SCHEMA = {
  type: 'object',
  properties: {
    issue: { type: 'integer' },
    applied: { type: 'array', items: { type: 'string' } },
    skipped: { type: 'array', items: { type: 'string' } },
    note: { type: 'string' },
  },
  required: ['issue', 'applied', 'skipped', 'note'],
  additionalProperties: false,
}

phase('Reconcile')
const rec = await parallel(targets.map(t => () => agent(
`You are reconciling dependency-edge metadata on GitHub issue #${t} in anadon/jls. Per-issue fixer agents just rewrote issue bodies but were forbidden from editing issues other than their own; they reported that #${t} needs these changes:

${JSON.stringify(byTarget[t], null, 2)}

Load tools via ToolSearch query "select:mcp__github__issue_read,mcp__github__issue_write". Fetch #${t}'s CURRENT body (it may itself have been rewritten minutes ago — never work from a stale copy). For each requested change, verify it against the current state of both endpoints (fetch the counterpart issue when the change concerns a blocked_by/blocks mirror; repo convention: every edge must be declared on both endpoints). Apply the changes that verify with a surgical issue_write "update" — edit only the machine-readable header block lines involved and preserve the rest of the body exactly. If requests conflict, resolve from the counterparts' actual declarations and explain. Skip anything already satisfied or wrong. Do not post comments; retry rate-limited writes up to 4 times.

Return via StructuredOutput: issue=${t}; applied=[each change applied, one string]; skipped=[each skip with reason]; note.`,
  { label: `edge:${t}`, phase: 'Reconcile', model: 'sonnet', schema: REC_SCHEMA }
)))
const recDone = rec.filter(Boolean)

const flagged = done.filter(r => !r.ok || !r.edited)
return {
  fixed: done.length,
  expected: ISSUES.length,
  unprocessed: ISSUES.filter(n => !done.some(r => r.issue === n)),
  flagged: flagged.map(r => ({ issue: r.issue, ok: r.ok, edited: r.edited, note: r.note })),
  superseded_comments: done.flatMap(r => (r.superseded_comment_ids || []).map(c => ({ issue: r.issue, comment_id: c }))),
  reconciled: recDone.length,
  reconcile_targets: targets.length,
  reconcile_details: recDone,
}
