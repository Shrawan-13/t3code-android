#!/usr/bin/env bash
# Keep only the newest Actions caches for this repository.
#
# Sorted by last_accessed_at (ISO-8601, so lexical order is chronological).
# Requires GH_TOKEN with actions:write on GITHUB_REPOSITORY.
set -euo pipefail

keep="${KEEP:-20}"
repo="${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"

tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT

gh api --paginate "repos/$repo/actions/caches?per_page=100" \
  --jq '.actions_caches[] | [.id, .last_accessed_at, .created_at, .size_in_bytes, .key] | @tsv' \
  > "$tmp"

total="$(wc -l < "$tmp" | tr -d ' ')"
echo "Found $total Actions caches; keeping the newest $keep by last access."

if [ "$total" -le "$keep" ]; then
  exit 0
fi

# Keep the first $keep rows after sorting newest-accessed first.
sort -t $'\t' -k2,2r "$tmp" | tail -n "+$((keep + 1))" |
while IFS=$'\t' read -r id last_accessed created size key; do
  [ -n "${id:-}" ] || continue
  if [ "${DRY_RUN:-0}" = "1" ]; then
    echo "Would delete cache $id (${size:-?} bytes, last accessed ${last_accessed:-?}): $key"
  else
    echo "Deleting cache $id (${size:-?} bytes, last accessed ${last_accessed:-?}): $key"
    gh api -X DELETE "repos/$repo/actions/caches/$id" >/dev/null
  fi
done
