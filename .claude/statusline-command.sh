#!/bin/sh
# Status line for Claude Code — mirrors powerlevel10k left prompt:
# user | dir | vcs | status (model + context)

input=$(cat)

user=$(whoami)
dir=$(echo "$input" | jq -r '.workspace.current_dir // .cwd // empty')
[ -z "$dir" ] && dir=$(pwd)

# Shorten home prefix
home="$HOME"
short_dir="${dir#$home}"
[ "$short_dir" != "$dir" ] && short_dir="~$short_dir"

# Git branch (skip optional lock to avoid blocking)
branch=$(git -C "$dir" --no-optional-locks symbolic-ref --short HEAD 2>/dev/null)
if [ -z "$branch" ]; then
  branch=$(git -C "$dir" --no-optional-locks rev-parse --short HEAD 2>/dev/null)
fi

# Model (short name)
model=$(echo "$input" | jq -r '.model.display_name // empty')

# Context used (share of the context window consumed). The input only exposes
# the remaining percentage, so derive used = 100 - remaining.
remaining=$(echo "$input" | jq -r '.context_window.remaining_percentage // empty')
if [ -n "$remaining" ]; then
  used=$(awk "BEGIN{printf \"%.0f\", 100 - $remaining}")
fi

# Build output
out=""

# context used segment
if [ -n "$used" ]; then
  printf '\033[90mctx:%s%%\033[0m | ' "$used"
fi

# user segment
printf '\033[32m%s\033[0m' "$user"

# dir segment
printf ' \033[34m%s\033[0m' "$short_dir"

# vcs segment
if [ -n "$branch" ]; then
  printf ' \033[33m(%s)\033[0m' "$branch"
fi

# model + context segment
if [ -n "$model" ]; then
  printf ' \033[36m%s\033[0m' "$model"
fi

# app segment: where the app on 8080 is reachable, e.g. a devcontainer's own IP
if ss -ltnH 'sport = :8080' 2>/dev/null | grep -q .; then
  ip=$(hostname -I 2>/dev/null | cut -d' ' -f1)
  printf ' | \033[35mhttp://%s:8080\033[0m' "${ip:-localhost}"
fi

printf '\n'
