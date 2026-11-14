#!/usr/bin/env bash
# 安全验收：未登录拦截、敏感词、重复提交、限流、上传路径穿越、手机号脱敏、审计留痕
#
# 用法：BASE=http://127.0.0.1:8080 ./deploy/test-security.sh
set -uo pipefail
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY all_proxy
export NO_PROXY="127.0.0.1,localhost" no_proxy="127.0.0.1,localhost"

B="${BASE:-http://127.0.0.1:8080}"
TENANT="${TENANT:-demo}"
NP="--noproxy 127.0.0.1"
PASS=0
FAIL=0

J() { python -c "import sys,json;d=json.load(sys.stdin);print($1)" 2>/dev/null; }
check() {
  if [ "$2" = "$3" ]; then echo "  ✓ $1"; PASS=$((PASS + 1));
  else echo "  ✗ $1 期望=$2 实际=$3"; FAIL=$((FAIL + 1)); fi
}

echo "=== 1 未登录访问租户接口（预期 401）"
H=$(curl -s $NP -o /tmp/sec1.json -w "%{http_code}" "$B/api/tenant/orders?pageSize=1" -H "X-Tenant-Code: ${TENANT}")
check "HTTP 401" "401" "$H"
check "错误码 40100" "40100" "$(cat /tmp/sec1.json | J "d['code']")"

echo "=== 2 伪造令牌（预期 40100 签名校验失败）"
FORGED=$(curl -s $NP "$B/api/tenant/orders" -H "X-Tenant-Code: ${TENANT}" -H "Authorization: Bearer abc.def" | J "d['code']")
check "伪造令牌被拒" "40100" "$FORGED"

echo "=== 3 平台账号访问租户接口（预期 403/40305）"
PT=$(curl -s $NP -X POST "$B/api/platform/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"platform","password":"Admin@123456"}' | J "d['data']['token']")
H=$(curl -s $NP -o /tmp/sec3.json -w "%{http_code}" "$B/api/tenant/orders" -H "X-Tenant-Code: ${TENANT}" -H "Authorization: Bearer ${PT}")
check "平台令牌访问租户接口被拒" "403" "$H"
check "错误码 40305" "40305" "$(cat /tmp/sec3.json | J "d['code']")"

echo "=== 4 租户登录后：敏感词拦截"
TT=$(curl -s $NP -X POST "$B/api/tenant/auth/login" -H "X-Tenant-Code: ${TENANT}" -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123456"}' | J "d['data']['token']")
AUTH=(-H "X-Tenant-Code: ${TENANT}" -H "Authorization: Bearer ${TT}" -H 'Content-Type: application/json')
SENS=$(curl -s $NP -X POST "$B/api/tenant/cms" "${AUTH[@]}" \
  -d '{"contentType":"文章","title":"夸大宣传测试","bodyHtml":"包治百病 绝对有效 药到病除","status":"草稿"}')
echo "  返回：$(echo "$SENS" | J "d['code'], d.get('msg','')")"
check "敏感词命中返回 40010" "40010" "$(echo "$SENS" | J "d['code']")"

echo "=== 5 手机号脱敏（客户列表不回明文）"
ORDERS=$(curl -s $NP "$B/api/tenant/orders?pageSize=1" "${AUTH[@]}")
PHONE_FIELD=$(echo "$ORDERS" | J "d['data']['records'][0]['phone'] if d['data']['records'] else ''")
echo "  样本：$PHONE_FIELD"
check "手机号含掩码" "True" "$([[ "$PHONE_FIELD" == *"*"* ]] && echo True || echo False)"

echo "=== 6 重复提交防护（同一幂等键第二次应 40902）"
IDEM=(-H "X-Idempotent-Key: sec-test-${RANDOM}${RANDOM}" -H "X-Tenant-Code: ${TENANT}" -H 'Content-Type: application/json')
BODY='{"customerName":"安全测试","phone":"13900000009","sourceCode":"安全测试"}'
R1=$(curl -s $NP -X POST "$B/api/open/leads" "${IDEM[@]}" -d "$BODY" | J "d['code']")
R2=$(curl -s $NP -X POST "$B/api/open/leads" "${IDEM[@]}" -d "$BODY" | J "d['code']")
check "首次提交成功" "0" "$R1"
check "重复提交被拒 40902" "40902" "$R2"

echo "=== 7 限流（同一接口高频调用应出现 42900）"
HIT=0
for i in $(seq 1 30); do
  C=$(curl -s $NP -X POST "$B/api/open/member/sms" -H "X-Tenant-Code: ${TENANT}" -H 'Content-Type: application/json' \
    -d '{"phone":"13900000077"}' | J "d['code']")
  [ "$C" = "42900" ] && HIT=$((HIT + 1))
done
check "出现限流响应" "True" "$([ "$HIT" -gt 0 ] && echo True || echo False)"

echo "=== 8 上传路径穿越（预期 50011 或 400）"
TRAV=$(curl -s $NP "$B/files/../etc/passwd" | J "d['code']")
check "路径穿越被拒" "50011" "$TRAV"
OUTSIDE=$(curl -s $NP "$B/files/other-tenant/benign/x.png" -H "Authorization: Bearer ${TT}" | J "d['code']")
check "跨租户对象键被拒" "50011" "$OUTSIDE"

echo "=== 9 审计留痕（登录与写操作均有记录）"
AUD=$(curl -s $NP "$B/api/tenant/audits?pageSize=5" "${AUTH[@]}")
check "审计接口可读" "0" "$(echo "$AUD" | J "d['code']")"
check "存在认证类审计" "True" "$(echo "$AUD" | J "any(r['module'] in ('认证','会员') for r in d['data']['records'])")"

echo
echo "=== 结果：通过 ${PASS} 项，失败 ${FAIL} 项"
[ "$FAIL" -eq 0 ] || exit 1
