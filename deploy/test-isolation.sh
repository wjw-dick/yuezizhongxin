#!/usr/bin/env bash
# 多租户隔离验收：两个租户的相同业务数据互相不可见，令牌跨租户一律 403
#
# 用法：BASE=http://127.0.0.1:8080 ./deploy/test-isolation.sh
set -uo pipefail
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY all_proxy
export NO_PROXY="127.0.0.1,localhost" no_proxy="127.0.0.1,localhost"

B="${BASE:-http://127.0.0.1:8080}"
NP="--noproxy 127.0.0.1"
PLATFORM_USER="${PLATFORM_USER:-platform}"
PLATFORM_PASS="${PLATFORM_PASS:-Admin@123456}"
STAMP=$(date +%H%M%S)
A="iso-a-${STAMP}"
B_TENANT="iso-b-${STAMP}"
PASS=0
FAIL=0

J() { python -c "import sys,json;d=json.load(sys.stdin);print($1)" 2>/dev/null; }
check() {
  if [ "$2" = "$3" ]; then echo "  ✓ $1"; PASS=$((PASS + 1));
  else echo "  ✗ $1 期望=$2 实际=$3"; FAIL=$((FAIL + 1)); fi
}

echo "=== 0 平台登录"
PT=$(curl -s $NP -X POST "$B/api/platform/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"${PLATFORM_USER}\",\"password\":\"${PLATFORM_PASS}\"}" | J "d['data']['token']")
check "平台令牌获取" "True" "$([ -n "$PT" ] && echo True || echo False)"

create_tenant() {
  local code="$1" name="$2"
  curl -s $NP -X POST "$B/api/platform/tenants" -H "Authorization: Bearer ${PT}" -H 'Content-Type: application/json' \
    -d "{\"tenantCode\":\"${code}\",\"tenantName\":\"${name}\",\"industry\":\"母婴照护\",\"contactName\":\"测试\",\"contactPhone\":\"13900000000\",\"packageId\":1,\"trialDays\":7,\"adminUsername\":\"admin\",\"adminRealName\":\"测试管理员\"}" \
    | J "d['data']['initialPassword'] if d['code']==0 else ''"
}

echo "=== 1 开通两个租户"
PW_A=$(create_tenant "$A" "隔离测试A ${STAMP}")
PW_B=$(create_tenant "$B_TENANT" "隔离测试B ${STAMP}")
check "租户A开通" "True" "$([ -n "$PW_A" ] && echo True || echo False)"
check "租户B开通" "True" "$([ -n "$PW_B" ] && echo True || echo False)"

login() {
  curl -s $NP -X POST "$B/api/tenant/auth/login" -H "X-Tenant-Code: $1" -H 'Content-Type: application/json' \
    -d "{\"username\":\"admin\",\"password\":\"$2\"}" | J "d['data']['token']"
}
TA=$(login "$A" "$PW_A")
TB=$(login "$B_TENANT" "$PW_B")
check "租户A登录" "True" "$([ -n "$TA" ] && echo True || echo False)"
check "租户B登录" "True" "$([ -n "$TB" ] && echo True || echo False)"

echo "=== 2 各自创建房型（同名，验证物理隔离）"
mk_room_type() {
  curl -s $NP -X POST "$B/api/tenant/rooms/types" -H "X-Tenant-Code: $1" -H "Authorization: Bearer $2" \
    -H 'Content-Type: application/json' \
    -d "{\"name\":\"隔离验证房型-${STAMP}\",\"level\":\"标准\",\"area\":35,\"bedType\":\"双床\",\"maxGuest\":2,\"facilities\":\"独立卫浴\",\"status\":\"ENABLED\"}" \
    | J "d['data'] if d['code']==0 else ''"
}
RA=$(mk_room_type "$A" "$TA")
RB=$(mk_room_type "$B_TENANT" "$TB")
check "租户A建房型" "True" "$([ -n "$RA" ] && echo True || echo False)"
check "租户B建房型" "True" "$([ -n "$RB" ] && echo True || echo False)"

echo "=== 3 列表只看到自己的数据"
LA=$(curl -s $NP "$B/api/tenant/rooms/types" -H "X-Tenant-Code: $A" -H "Authorization: Bearer $TA" | J "len(d['data'])")
LB=$(curl -s $NP "$B/api/tenant/rooms/types" -H "X-Tenant-Code: $B_TENANT" -H "Authorization: Bearer $TB" | J "len(d['data'])")
check "租户A仅 1 条房型" "1" "$LA"
check "租户B仅 1 条房型" "1" "$LB"

echo "=== 4 用A的令牌改B的请求头（预期 40300/403）"
CROSS=$(curl -s $NP -o /tmp/iso_cross.json -w "%{http_code}" "$B/api/tenant/rooms/types" \
  -H "X-Tenant-Code: $B_TENANT" -H "Authorization: Bearer $TA")
CODE_CROSS=$(cat /tmp/iso_cross.json | J "d['code']")
check "跨租户请求被拒" "403" "$CROSS"
check "错误码为 40300" "40300" "$CODE_CROSS"

echo "=== 5 按主键操作对方租户数据（删除应 404 且不生效）"
if [ -n "$RB" ]; then
  PEEK=$(curl -s $NP -X DELETE "$B/api/tenant/rooms/types/$RB" -H "X-Tenant-Code: $A" -H "Authorization: Bearer $TA" | J "d['code']")
  check "按主键跨租户删除被拒" "40400" "$PEEK"
  STILL=$(curl -s $NP "$B/api/tenant/rooms/types" -H "X-Tenant-Code: $B_TENANT" -H "Authorization: Bearer $TB" | J "len(d['data'])")
  check "对方数据未被删除" "1" "$STILL"
fi

echo "=== 6 开放接口按租户隔离（同域名不同租户头）"
OA=$(curl -s $NP "$B/api/open/site/packages" -H "X-Tenant-Code: $A" | J "len(d['data'])")
OB=$(curl -s $NP "$B/api/open/site/packages" -H "X-Tenant-Code: $B_TENANT" | J "len(d['data'])")
check "租户A开放接口为空（未上架套餐）" "0" "$OA"
check "租户B开放接口为空（未上架套餐）" "0" "$OB"

echo
echo "=== 结果：通过 ${PASS} 项，失败 ${FAIL} 项"
[ "$FAIL" -eq 0 ] || exit 1
