#!/usr/bin/env bash
# 多端（小程序 / H5 / App / 官网）冒烟：会员登录 → 订单 → 护理 → 月子餐 → 康复 → 员工端
#
# 前置：后端已启动（默认 http://127.0.0.1:8080），demo 租户存在客户手机号 13900000001 的在住订单。
# 可选：BASE=... PHONE=... 覆盖默认值
set -uo pipefail
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY all_proxy
export NO_PROXY="127.0.0.1,localhost" no_proxy="127.0.0.1,localhost"

B="${BASE:-http://127.0.0.1:8080}"
TENANT="${TENANT:-demo}"
PHONE="${PHONE:-13900000001}"
NP="--noproxy 127.0.0.1"
PASS=0
FAIL=0

J() { python -c "import sys,json;d=json.load(sys.stdin);print($1)" 2>/dev/null; }

check() {
  local name="$1" expected="$2" actual="$3"
  if [ "$expected" = "$actual" ]; then
    echo "  ✓ $name（$actual）"
    PASS=$((PASS + 1))
  else
    echo "  ✗ $name 期望 $expected 实际 $actual"
    FAIL=$((FAIL + 1))
  fi
}

echo "=== 1 站点数据（免登录，需租户头）"
HOME_JSON=$(curl -s $NP "$B/api/open/site/home" -H "X-Tenant-Code: ${TENANT}")
check "首页聚合返回 0" "0" "$(echo "$HOME_JSON" | J "d['code']")"
check "套餐列表非空" "True" "$(echo "$HOME_JSON" | J "len(d['data']['packages'])>0")"

echo "=== 2 缺租户头（预期 40102）"
NO_TENANT=$(curl -s $NP "$B/api/open/site/home" -H "X-Tenant-Code: not-exist-tenant")
check "未知租户被拒" "40102" "$(echo "$NO_TENANT" | J "d['code']")"

echo "=== 3 会员验证码登录"
SMS=$(curl -s $NP -X POST "$B/api/open/member/sms" -H "X-Tenant-Code: ${TENANT}" -H 'Content-Type: application/json' -d "{\"phone\":\"${PHONE}\"}")
check "验证码发送成功" "True" "$(echo "$SMS" | J "d['data']['sent']")"
CODE=$(echo "$SMS" | J "d['data'].get('devCode','')")
if [ -z "$CODE" ]; then
  echo "  ! 生产模式未回显验证码，请手动指定 CODE=xxxxxx 重跑"
  CODE="${CODE:-000000}"
fi
LOGIN=$(curl -s $NP -X POST "$B/api/open/member/login" -H "X-Tenant-Code: ${TENANT}" -H 'Content-Type: application/json' \
  -d "{\"phone\":\"${PHONE}\",\"code\":\"${CODE}\",\"source\":\"冒烟\"}")
MT=$(echo "$LOGIN" | J "d['data']['token']")
check "登录返回令牌" "True" "$([ -n "$MT" ] && echo True || echo False)"

AUTH=(-H "X-Tenant-Code: ${TENANT}" -H "X-Member-Token: ${MT}")

echo "=== 4 会员资料与资产"
PROFILE=$(curl -s $NP "$B/api/open/member/profile" "${AUTH[@]}")
check "资料手机号已脱敏" "True" "$(echo "$PROFILE" | J "'*' in d['data']['phone']")"
POINTS=$(curl -s $NP "$B/api/open/member/points" "${AUTH[@]}")
check "积分流水可读" "0" "$(echo "$POINTS" | J "d['code']")"

echo "=== 5 我的订单"
ORDERS=$(curl -s $NP "$B/api/open/customer/orders" "${AUTH[@]}")
check "订单列表可读" "0" "$(echo "$ORDERS" | J "d['code']")"
ORDER_ID=$(echo "$ORDERS" | J "d['data'][0]['id'] if d['data'] else ''")
ORDER_NO=$(echo "$ORDERS" | J "d['data'][0]['orderNo'] if d['data'] else ''")
if [ -z "$ORDER_ID" ]; then
  echo "  ! 该手机号暂无订单，后续在住/护理用例跳过"
fi

echo "=== 6 订单详情（越权校验）"
if [ -n "$ORDER_NO" ]; then
  DETAIL=$(curl -s $NP "$B/api/open/customer/orders/${ORDER_NO}" "${AUTH[@]}")
  check "订单详情可读" "0" "$(echo "$DETAIL" | J "d['code']")"
  OTHER=$(curl -s $NP "$B/api/open/customer/care?orderId=999999" "${AUTH[@]}")
  check "非本人订单被拒（40304）" "40304" "$(echo "$OTHER" | J "d['code']")"
fi

echo "=== 7 在住与护理"
STAY=$(curl -s $NP "$B/api/open/customer/stay" "${AUTH[@]}")
LIVING=$(echo "$STAY" | J "d['data']['living']")
check "在住概览可读" "0" "$(echo "$STAY" | J "d['code']")"
if [ "$LIVING" = "True" ]; then
  CARE=$(curl -s $NP "$B/api/open/customer/care?orderId=${ORDER_ID}" "${AUTH[@]}")
  check "护理计划可读" "0" "$(echo "$CARE" | J "d['code']")"
  CONFIRM=$(curl -s $NP -X POST "$B/api/open/customer/care/confirm" "${AUTH[@]}" -H 'Content-Type: application/json' \
    -d "{\"orderId\":${ORDER_ID},\"satisfied\":true,\"remark\":\"冒烟确认\"}")
  echo "  家属每日确认：code=$(echo "$CONFIRM" | J "d['code']")（重复执行返回 40900 属正常）"
fi

echo "=== 8 月子餐与康复"
if [ -n "$ORDER_ID" ]; then
  MEAL=$(curl -s $NP "$B/api/open/customer/meal?orderId=${ORDER_ID}" "${AUTH[@]}")
  check "餐单可读" "0" "$(echo "$MEAL" | J "d['code']")"
  REHAB=$(curl -s $NP "$B/api/open/customer/rehab?orderId=${ORDER_ID}" "${AUTH[@]}")
  check "康复疗程可读" "0" "$(echo "$REHAB" | J "d['code']")"
fi

echo "=== 9 员工端（护理师小程序）"
TL=$(curl -s $NP -X POST "$B/api/tenant/auth/login" -H "X-Tenant-Code: ${TENANT}" -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123456"}')
TT=$(echo "$TL" | J "d['data']['token']")
check "员工登录" "True" "$([ -n "$TT" ] && echo True || echo False)"
if [ -n "$TT" ]; then
  WB=$(curl -s $NP "$B/api/tenant/mobile/workbench" -H "X-Tenant-Code: ${TENANT}" -H "Authorization: Bearer ${TT}")
  check "工作台可读" "0" "$(echo "$WB" | J "d['code']")"
  MEALS=$(curl -s $NP "$B/api/tenant/mobile/meals?checked=0" -H "X-Tenant-Code: ${TENANT}" -H "Authorization: Bearer ${TT}")
  check "出餐核对列表可读" "0" "$(echo "$MEALS" | J "d['code']")"
fi

echo
echo "=== 结果：通过 ${PASS} 项，失败 ${FAIL} 项"
[ "$FAIL" -eq 0 ] || exit 1
