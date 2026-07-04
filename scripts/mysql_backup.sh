#!/bin/bash
# 脚本目录 /usr/local/scripts/mysql_backup.sh, 或者其他目录
# 在root用户的 /root/.my.cnf 中维护用户名和密码
# [client]
# user=root
# password=你的密码
#
# 系统定时任务配置
# 0 3 * * * /usr/local/scripts/mysql_backup.sh >> /你的备份目录/logs/backup.log 2>&1

BACKUP=你的备份目录
mkdir -p $BACKUP/schema
mkdir -p $BACKUP/data

DATE=$(date +%F)
TIME=$(date +%F_%H-%M-%S)

tmp=$BACKUP/schema/schema.sql.gz.tmp
#schame
if mysqldump --all-databases --no-data --routines --events --triggers | gzip > "$tmp"; then
        mv "$tmp" $BACKUP/schema/schema_${DATE}.sql.gz
fi
rm -f $tmp

tmp=$BACKUP/data/data.sql.gz.tmp
#data
if mysqldump --all-databases --no-create-info --single-transaction --quick --skip-lock-tables | gzip > $tmp; then
        mv $tmp $BACKUP/data/data_${TIME}.sql.gz
fi
rm -f $tmp

# 删除30天以前
find $BACKUP/schema -name "*.gz" -mtime +30 -delete
find $BACKUP/data -name "*.gz" -mtime +30 -delete
