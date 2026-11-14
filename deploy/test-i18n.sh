#!/usr/bin/env bash
# 多语言验收：语言包、站点语言切换、内容多语言回退、官网 SEO 标记
#
# 用法：BASE=http://127.0.0.1:8080 SITE=http://127.0.0.1:5180 ./deploy/test-i18n.sh
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

TT=$(curl -s $NP -X POST "$B/api/tenant/auth/login" -H "X-Tenant-Code: ${TENANT}" -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123456"}' | J "d['data']['token']")
AUTH=(-H "X-Tenant-Code: ${TENANT}" -H "Authorization: Bearer ${TT}")

echo "=== 1 站点语言清单（开放接口）"
META=$(curl -s $NP "$B/api/open/site/meta" -H "X-Tenant-Code: ${TENANT}")
check "meta 可读" "0" "$(echo "$META" | J "d['code']")"
check "含 zh-CN" "True" "$(echo "$META" | J "any(l['code']=='zh-CN' for l in d['data']['languages'])")"
check "默认语言存在" "True" "$(echo "$META" | J "bool(d['data']['defaultLang'])")"

echo "=== 2 语言包下发"
PACK_ZH=$(curl -s $NP "$B/api/tenant/i18n/pack/zh-CN" "${AUTH[@]}")
check "中文语言包可读" "0" "$(echo "$PACK_ZH" | J "d['code']")"
PACK_EN=$(curl -s $NP "$B/api/tenant/i18n/pack/en-US" "${AUTH[@]}")
check "英文语言包可读" "0" "$(echo "$PACK_EN" | J "d['code']")"
check "语言包非空" "True" "$(echo "$PACK_ZH" | J "len(d['data'])>0")"

echo "=== 3 语言增删改"
NEW_LANG="ja-JP"
ADD=$(curl -s $NP -X POST "$B/api/tenant/i18n/languages" "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"code\":\"${NEW_LANG}\",\"name\":\"日本語\",\"enabled\":true,\"isDefault\":false,\"sort\":9}")
echo "  新增日文：$(echo "$ADD" | J "d['code'], d.get('msg','')")"
HAS=$(curl -s $NP "$B/api/tenant/i18n/languages" "${AUTH[@]}" | J "any(l['code']=='${NEW_LANG}' for l in d['data'])")
check "日文已加入语言列表" "True" "$HAS"

echo "=== 4 内容多语言回退（无译文时回落中文）"
HOME_ZH=$(curl -s $NP "$B/api/open/site/home?lang=zh-CN" -H "X-Tenant-Code: ${TENANT}")
HOME_EN=$(curl -s $NP "$B/api/open/site/home?lang=en-US" -H "X-Tenant-Code: ${TENANT}")
check "中文首页可读" "0" "$(echo "$HOME_ZH" | J "d['code']")"
check "英文首页可读（允许回退）" "0" "$(echo "$HOME_EN" | J "d['code']")"
check "英文首页仍有套餐数据" "True" "$(echo "$HOME_EN" | J "len(d['data']['packages'])>0")"

echo "=== 5 多语言文案保存"
LANG_ID=$(curl -s $NP "$B/api/tenant/i18n/languages" "${AUTH[@]}" | J "next((l['id'] for l in d['data'] if l['code']=='${NEW_LANG}'), '')")
if [ -n "$LANG_ID" ]; then
  PUT=$(curl -s $NP -X PUT "$B/api/tenant/i18n/messages" "${AUTH[@]}" -H 'Content-Type: application/json' \
    -d "{\"langCode\":\"${NEW_LANG}\",\"messages\":{\"site.name\":\"月の孕\",\"nav.rooms\":\"お部屋\"}}")
  check "文案保存成功" "0" "$(echo "$PUT" | J "d['code']")"
  PACK_JA=$(curl -s $NP "$B/api/tenant/i18n/pack/${NEW_LANG}" "${AUTH[@]}")
  check "日文语言包含新文案" "True" "$(echo "$PACK_JA" | J "'お部屋' in d['data'].values()")"
fi

echo "=== 6 官网 SEO 与 hreflang（静态检查）"
SITE_DIR="$(cd "$(dirname "$0")/.." && pwd)/web-site"
HTML="$SITE_DIR/index.html"
if [ -f "$HTML" ]; then
  check "含 hreflang zh-CN" "True" "$(grep -q 'hreflang="zh-CN"' "$HTML" && echo True || echo False)"
  check "含 hreflang en-US" "True" "$(grep -q 'hreflang="en-US"' "$HTML" && echo True || echo False)"
  check "含 x-default" "True" "$(grep -q 'hreflang="x-default"' "$HTML" && echo True || echo False)"
  check "含 JSON-LD 结构化数据" "True" "$(grep -q 'application/ld+json' "$HTML" && echo True || echo False)"
  check "含 description" "True" "$(grep -q 'name="description"' "$HTML" && echo True || echo False)"
else
  echo "  ! 未找到 web-site/index.html，跳过静态检查"
fi

echo
echo "=== 结果：通过 ${PASS} 项，失败 ${FAIL} 项"
[ "$FAIL" -eq 0 ] || exit 1
