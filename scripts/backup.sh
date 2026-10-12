#!/usr/bin/env bash
# =====================================================================
# MediFind — database backup helper
#
# SLP: Platform Security & Compliance → "Automated database backup &
# recovery"
#
# A small, transparent wrapper around mysqldump rather than a hidden
# scheduled job — run it by hand, or point your OS's own scheduler
# (cron, systemd timer, Windows Task Scheduler) at it. Kept as a plain
# script rather than application code because backup/restore is an
# operational concern that runs independently of whether the Spring
# Boot app itself is up.
#
# Usage:
#   ./scripts/backup.sh [output-directory]
# =====================================================================
set -euo pipefail

DB_NAME="${DB_NAME:-medifind_db}"
DB_USER="${DB_USER:-root}"
DB_HOST="${DB_HOST:-localhost}"
OUT_DIR="${1:-./backups}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

mkdir -p "$OUT_DIR"
OUT_FILE="$OUT_DIR/medifind_${TIMESTAMP}.sql"

echo "Backing up '$DB_NAME' to $OUT_FILE ..."
mysqldump -h "$DB_HOST" -u "$DB_USER" -p "$DB_NAME" > "$OUT_FILE"
echo "Done."
