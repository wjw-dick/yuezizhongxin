#!/usr/bin/env bash
# 阶段 1–3 冒烟脚本：需要先启动服务（默认 18080）与 MySQL（默认 3308）
# 每次运行使用新的租户编码，可重复执行，不依赖历史数据
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY all_proxy
export NO_PROXY="127.0.0.1,localhost"
export no_proxy="127.0.0.1,localhost"
B=http://127.0.0.1:18080
J() { python -c "import sys,json;d=json.load(sys.stdin);print($1)"; }
NP="--noproxy 127.0.0.1"

SUFFIX=$(date +%H%M%S)
CODE="test${SUFFIX}"
ADMIN="admin${SUFFIX}"

echo "== 1 平台登录"
PL=$(curl -s $NP -X POST $B/api/platform/auth/login -H 'Content-Type: application/json' -d '{"username":"platform","password":"Admin@123456"}')
echo "$PL" | head -c 200; echo
PT=$(echo "$PL" | J "d['data']['token']")

echo "== 2 平台租户列表"
curl -s $NP "$B/api/platform/tenants?pageSize=5" -H "Authorization: Bearer $PT" \
  | J "d['code'], [(r['tenantCode'],r['statusName'],r['seatUsed'],r['seatLimit']) for r in d['data']['records']]"

echo "== 3 开通新租户 ${CODE}（试用 14 天）"
CREATE=$(curl -s $NP -X POST $B/api/platform/tenants -H "Authorization: Bearer $PT" -H 'Content-Type: application/json' \
  -d "{\"tenantCode\":\"${CODE}\",\"tenantName\":\"杭州西湖旗舰店 ${SUFFIX}\",\"industry\":\"母婴照护\",\"contactName\":\"李店长\",\"contactPhone\":\"13900000000\",\"packageId\":1,\"trialDays\":14,\"adminUsername\":\"${ADMIN}\",\"adminRealName\":\"李店长\"}")
echo "$CREATE" | head -c 300; echo
PWD2=$(echo "$CREATE" | J "d['data']['initialPassword']")

echo "== 4 新租户初始化数据核对"
docker exec yzz-mysql-verify mysql -uroot -proot --default-character-set=utf8mb4 -N -e "
select concat('roles=',(select count(*) from yzz_platform.sys_role where tenant_id=t.id),
 ' menus=',(select count(*) from yzz_platform.sys_menu where tenant_id=t.id),
 ' configs=',(select count(*) from yzz_platform.sys_config where tenant_id=t.id),
 ' role_menu=',(select count(*) from yzz_platform.sys_role_menu where tenant_id=t.id),
 ' status=',t.status)
from yzz_platform.platform_tenant t where t.tenant_code='${CODE}';" 2>/dev/null

echo "== 5 新租户管理员登录"
TL2=$(curl -s $NP -X POST $B/api/tenant/auth/login -H 'Content-Type: application/json' -H "X-Tenant-Code: ${CODE}" \
  -d "{\"username\":\"${ADMIN}\",\"password\":\"$PWD2\"}")
echo "$TL2" | head -c 200; echo
T2=$(echo "$TL2" | J "d['data']['token']")

echo "== 6 demo 令牌 + ${CODE} 请求头（预期 403 越权）"
D=$(curl -s $NP -X POST $B/api/tenant/auth/login -H 'Content-Type: application/json' -H 'X-Tenant-Code: demo' -d '{"username":"admin","password":"Admin@123456"}')
T1=$(echo "$D" | J "d['data']['token']")
curl -s $NP -o /tmp/r6.json -w "http=%{http_code} " "$B/api/tenant/audits" -H "Authorization: Bearer $T1" -H "X-Tenant-Code: ${CODE}"; cat /tmp/r6.json; echo

echo "== 7 租户令牌访问平台端（预期 403 入口不互通）"
curl -s $NP -o /tmp/r7.json -w "http=%{http_code} " "$B/api/platform/tenants" -H "Authorization: Bearer $T1"; cat /tmp/r7.json; echo

echo "== 8 租户审计日志（demo）"
curl -s $NP "$B/api/tenant/audits?pageSize=3" -H "Authorization: Bearer $T1" \
  | J "d['code'], d['data']['total'], [(r['module'],r['action'],r['result']) for r in d['data']['records']]"

echo "== 9 停用 ${CODE} 后登录（预期 403 已停用）"
TID=$(curl -s $NP "$B/api/platform/tenants?tenantCode=${CODE}" -H "Authorization: Bearer $PT" | J "d['data']['records'][0]['id']")
curl -s $NP -o /dev/null -w "disable http=%{http_code}\n" -X POST "$B/api/platform/tenants/$TID/disable" -H "Authorization: Bearer $PT"
curl -s $NP -o /tmp/r9.json -w "http=%{http_code} " -X POST $B/api/tenant/auth/login -H 'Content-Type: application/json' -H "X-Tenant-Code: ${CODE}" -d "{\"username\":\"${ADMIN}\",\"password\":\"$PWD2\"}"; cat /tmp/r9.json; echo

echo "== 10 恢复 ${CODE} 后登录（预期成功）"
curl -s $NP -o /dev/null -w "resume http=%{http_code}\n" -X POST "$B/api/platform/tenants/$TID/resume" -H "Authorization: Bearer $PT"
curl -s $NP -o /tmp/r10.json -w "http=%{http_code} " -X POST $B/api/tenant/auth/login -H 'Content-Type: application/json' -H "X-Tenant-Code: ${CODE}" -d "{\"username\":\"${ADMIN}\",\"password\":\"$PWD2\"}"; head -c 120 /tmp/r10.json; echo

echo "== 11 平台审计（应记录开通/停用/恢复）"
curl -s $NP "$B/api/platform/audits?pageSize=5" -H "Authorization: Bearer $PT" \
  | J "d['code'], [(r['module'],r['action'],r['result']) for r in d['data']['records']]"
