#!/usr/bin/env bash
# =====================================================================
# MediFind — database restore helper
#
# SLP: Platform Security & Compliance → "Automated database backup &
# recovery"
#
# Usage:
#   ./scripts/restore.sh path/to/backup.sql
# =====================================================================
set -euo pipefail

if [ $# -lt 1 ]; then
    echo "Usage: $0 <backup-file.sql>"
    exit 1
fi

DB_NAME="${DB_NAME:-medifind_db}"
DB_USER="${DB_USER:-root}"
DB_HOST="${DB_HOST:-localhost}"
BACKUP_FILE="$1"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "Backup file not found: $BACKUP_FILE"
    exit 1
fi

echo "This will OVERWRITE the current contents of database '$DB_NAME'."
read -r -p "Continue? [y/N] " confirm
if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
    echo "Cancelled."
    exit 0
fi

mysql -h "$DB_HOST" -u "$DB_USER" -p "$DB_NAME" < "$BACKUP_FILE"
echo "Restore complete."
