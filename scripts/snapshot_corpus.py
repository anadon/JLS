#!/usr/bin/env python3
"""Dump the JLS issue corpus to a snapshot directory for offline validation.

All bulk reads go through REST (`gh api`) — GraphQL quota exhausts fast under
agent fleets and is reserved for the one query REST cannot do: the Projects v2
board. Comments come from the single repo-wide endpoint, not one call per
issue. Validators (validate-issue-template.py --snapshot, validate_issue_graph,
validate_hygiene, validate_board) consume the directory this writes; see
issue_corpus.py for the file inventory.

Usage:
  scripts/snapshot_corpus.py --out .../snapshots/2026-08-23 \
      [--repo anadon/JLS] [--repo-root .] [--project-number 1] [--skip-board]

Exit status: 0 ok, 2 environment/gh error.
"""
import argparse
import datetime
import hashlib
import json
import os
import re
import subprocess
import sys


def gh_api(path, *extra):
    r = subprocess.run(["gh", "api", *extra, path],
                       capture_output=True, text=True)
    if r.returncode != 0:
        print(f"gh api {path} failed: {r.stderr.strip()}", file=sys.stderr)
        sys.exit(2)
    return r.stdout


def gh_api_json(path, *extra):
    return json.loads(gh_api(path, *extra))


def gh_paginate(path):
    """Paginated REST results as one list (`--paginate --slurp` emits an
    array of pages)."""
    pages = json.loads(gh_api(path, "--paginate", "--slurp"))
    return [item for page in pages for item in page]


def fetch_board(project_number, owner):
    """All board items via paginated GraphQL: [{item_id, type, issue, state,
    status}]. The one sanctioned GraphQL query per snapshot."""
    query = """
    query($owner: String!, $number: Int!, $after: String) {
      user(login: $owner) {
        projectV2(number: $number) {
          items(first: 100, after: $after) {
            pageInfo { hasNextPage endCursor }
            nodes {
              id
              type
              fieldValueByName(name: "Status") {
                ... on ProjectV2ItemFieldSingleSelectValue { name }
              }
              content {
                ... on Issue { number state }
                ... on PullRequest { number }
              }
            }
          }
        }
      }
    }"""
    items, after = [], None
    while True:
        args = ["-f", f"query={query}", "-F", f"owner={owner}",
                "-F", f"number={project_number}"]
        if after:
            args += ["-F", f"after={after}"]
        r = subprocess.run(["gh", "api", "graphql", *args],
                           capture_output=True, text=True)
        if r.returncode != 0:
            print(f"board GraphQL failed: {r.stderr.strip()}", file=sys.stderr)
            sys.exit(2)
        data = json.loads(r.stdout)
        conn = data["data"]["user"]["projectV2"]["items"]
        for n in conn["nodes"]:
            content = n.get("content") or {}
            status = (n.get("fieldValueByName") or {}).get("name")
            items.append({"item_id": n["id"], "type": n["type"],
                          "issue": content.get("number"),
                          "state": content.get("state"),
                          "status": status})
        if not conn["pageInfo"]["hasNextPage"]:
            return items
        after = conn["pageInfo"]["endCursor"]


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--out", required=True, help="snapshot directory to create")
    ap.add_argument("--repo", default="anadon/JLS")
    ap.add_argument("--repo-root", default=".",
                    help="local clone, for recording HEAD in meta.json")
    ap.add_argument("--project-number", type=int, default=1)
    ap.add_argument("--skip-board", action="store_true",
                    help="skip the board GraphQL query")
    args = ap.parse_args()
    owner = args.repo.split("/")[0]

    os.makedirs(args.out, exist_ok=True)

    raw = gh_paginate(f"repos/{args.repo}/issues?state=open&per_page=100")
    issues = [d for d in raw if "pull_request" not in d]
    with open(os.path.join(args.out, "issues.jsonl"), "w") as fh:
        for d in sorted(issues, key=lambda d: d["number"]):
            fh.write(json.dumps(d) + "\n")
    print(f"issues: {len(issues)} open ({len(raw) - len(issues)} PRs filtered)")

    closed_raw = gh_paginate(f"repos/{args.repo}/issues?state=closed&per_page=100")
    closed = [d for d in closed_raw if "pull_request" not in d]
    with open(os.path.join(args.out, "closed.jsonl"), "w") as fh:
        for d in sorted(closed, key=lambda d: d["number"]):
            fh.write(json.dumps(
                {"number": d["number"], "title": d["title"],
                 "state": d["state"], "state_reason": d.get("state_reason"),
                 "labels": [l["name"] for l in d.get("labels") or []],
                 "updated_at": d["updated_at"]}) + "\n")
    print(f"closed: {len(closed)} issues "
          f"({len(closed_raw) - len(closed)} PRs filtered)")

    open_numbers = {d["number"] for d in issues}
    comments = gh_paginate(f"repos/{args.repo}/issues/comments?per_page=100")
    kept = 0
    with open(os.path.join(args.out, "comments.jsonl"), "w") as fh:
        for c in comments:
            m = re.search(r"/issues/(\d+)$", c.get("issue_url") or "")
            if m and int(m.group(1)) in open_numbers:
                fh.write(json.dumps(c) + "\n")
                kept += 1
    print(f"comments: {kept} on open issues ({len(comments)} repo-wide)")

    parents = [d["number"] for d in issues
               if (d.get("sub_issues_summary") or {}).get("total", 0) > 0]
    with open(os.path.join(args.out, "subissues.jsonl"), "w") as fh:
        for n in parents:
            subs = gh_paginate(f"repos/{args.repo}/issues/{n}/sub_issues?per_page=100")
            fh.write(json.dumps(
                {"parent": n,
                 "children": [s["number"] for s in subs],
                 "children_state": {str(s["number"]): s["state"] for s in subs}}
            ) + "\n")
    print(f"subissues: {len(parents)} parents fetched")

    if not args.skip_board:
        board = fetch_board(args.project_number, owner)
        with open(os.path.join(args.out, "board.jsonl"), "w") as fh:
            for item in board:
                fh.write(json.dumps(item) + "\n")
        print(f"board: {len(board)} items")

    milestones = gh_api_json(f"repos/{args.repo}/milestones?state=all&per_page=100")
    with open(os.path.join(args.out, "milestones.json"), "w") as fh:
        json.dump(milestones, fh, indent=1)
    labels = gh_paginate(f"repos/{args.repo}/labels?per_page=100")
    with open(os.path.join(args.out, "labels.json"), "w") as fh:
        json.dump(labels, fh, indent=1)
    print(f"milestones: {len(milestones)}; labels: {len(labels)}")

    head = subprocess.run(["git", "-C", args.repo_root, "rev-parse", "HEAD"],
                          capture_output=True, text=True).stdout.strip()
    rate = gh_api_json("rate_limit")["resources"]
    meta = {
        "fetched_at": datetime.datetime.now(datetime.timezone.utc)
                      .strftime("%Y-%m-%dT%H:%M:%SZ"),
        "repo": args.repo,
        "head": head,
        "rate_limit": {k: rate[k]["remaining"] for k in ("core", "graphql")},
        "index": {str(d["number"]): {
            "updated_at": d["updated_at"],
            "body_sha256": hashlib.sha256(
                (d.get("body") or "").encode()).hexdigest()}
            for d in issues},
    }
    with open(os.path.join(args.out, "meta.json"), "w") as fh:
        json.dump(meta, fh, indent=1)
    print(f"meta: head={head[:9]} core_remaining={meta['rate_limit']['core']} "
          f"graphql_remaining={meta['rate_limit']['graphql']}")


if __name__ == "__main__":
    main()
