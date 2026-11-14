#!/usr/bin/env bash
# 数据库自动备份：每天 02:30 全量 mysqldump + gzip，保留 BACKUP_KEEP_DAYS 天
# 容器内运行，日志走 docker logs，备份文件落在 deploy/backup/
set -uo pipefail

KEEP_DAYS="${BACKUP_KEEP_DAYS:-14}"
DIR="/backup"
STAMP="$(date +%Y%m%d-%H%M%S)"
FILE="$DIR/yzz_platform-$STAMP.sql.gz"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }

mkdir -p "$DIR"

log "备份开始：$FILE"
if mysqldump -h mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
    --single-transaction --quick --routines --events --triggers \
    --default-character-set=utf8mb4 --databases yzz_platform 2>/dev/null | gzip -9 > "$FILE"; then
  SIZE=$(du -h "$FILE" | cut -f1)
  log "备份完成：$FILE（$SIZE）"
else
  log "备份失败，删除不完整文件"
  rm -f "$FILE"
fi

log "清理 ${KEEP_DAYS} 天前的备份"
find "$DIR" -name 'yzz_platform-*.sql.gz' -type f -mtime +"$KEEP_DAYS" -print -delete

# 简单自检：最新备份可解压且包含建表语句
LATEST=$(ls -t "$DIR"/yzz_platform-*.sql.gz 2>/dev/null | head -1)
if [ -n "$LATEST" ] && gzip -dc "$LATEST" | grep -q 'CREATE TABLE'; then
  log "自检通过：$LATEST"
else
  log "自检失败：最新备份不可用，请立即人工检查"
fi

# 每 24 小时执行一次（容器常驻）
while true; do
  NOW=$(date +%H%M)
  if [ "$NOW" = "0230" ]; then
    STAMP="$(date +%Y%m%d-%H%M%S)"
    FILE="$DIR/yzz_platform-$STAMP.sql.gz"
    log "定时备份开始：$FILE"
    mysqldump -h mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
      --single-transaction --quick --routines --events --triggers \
      --default-character-set=utf8mb4 --databases yzz_platform 2>/dev/null | gzip -9 > "$FILE" \
      && log "定时备份完成" || log "定时备份失败"
    find "$DIR" -name 'yzz_platform-*.sql.gz' -type f -mtime +"$KEEP_DAYS" -delete
  fi
  sleep 50
done
