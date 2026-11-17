#!/bin/sh
set -eu

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

chmod +x scripts/hooks/*
git config core.hooksPath scripts/hooks

echo "Git hooks enabled from scripts/hooks (pre-push blocks direct pushes to main and develop)."
