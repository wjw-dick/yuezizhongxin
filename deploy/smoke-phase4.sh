#!/usr/bin/env bash
# 阶段 4（T20~T29）系统配置域端到端冒烟脚本
# 前置：MySQL 已在 3308 就绪并执行 sql/00~06；服务已在 18080 启动。
unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY all_proxy
export NO_PROXY="127.0.0.1,localhost"
export no_proxy="127.0.0.1,localhost"

B=${B:-http://127.0.0.1:18080}
NP="--noproxy 127.0.0.1"
PASS=0
FAIL=0

check() {
  local name="$1" expected="$2" actual="$3"
  if [ "$actual" = "$expected" ]; then
    echo "  [PASS] $name ($actual)"
    PASS=$((PASS + 1))
  else
    echo "  [FAIL] $name 期望=$expected 实际=$actual"
    FAIL=$((FAIL + 1))
  fi
}

jqf() { python -c "import sys,json;d=json.load(sys.stdin);print(eval(sys.argv[1]))" "$1"; }

echo "== 0. 登录租户后台"
LOGIN=$(curl -s $NP -X POST "$B/api/tenant/auth/login" -H 'Content-Type: application/json' \
  -H 'X-Tenant-Code: demo' -d '{"username":"admin","password":"Admin@123456"}')
TOKEN=$(echo "$LOGIN" | jqf "d['data']['token']")
AUTH="Authorization: Bearer $TOKEN"
check "租户登录" "0" "$(echo "$LOGIN" | jqf "d['code']")"

echo "== 1. 数据字典（T29）"
CATS=$(curl -s $NP "$B/api/tenant/dicts/categories" -H "$AUTH")
check "字典分类数量≥20" "True" "$(echo "$CATS" | jqf "len(d['data'])>=20")"
OPTS=$(curl -s $NP "$B/api/tenant/dicts/options/room_state" -H "$AUTH")
check "房态选项中文名" "True" "$(echo "$OPTS" | jqf "all(o['label'] and o['value'] for o in d['data'])")"
ITEM_CODE="cleaning_test_$(date +%H%M%S)"
NEWITEM=$(curl -s $NP -X POST "$B/api/tenant/dicts/categories/room_state/items" -H "$AUTH" \
  -H 'Content-Type: application/json' -d "{\"code\":\"$ITEM_CODE\",\"name\":\"清洁中（测试）\",\"color\":\"#409EFF\",\"sort\":99}")
check "新增字典项" "0" "$(echo "$NEWITEM" | jqf "d['code']")"
OPTS2=$(curl -s $NP "$B/api/tenant/dicts/options/room_state" -H "$AUTH")
check "新增后缓存已失效" "True" "$(echo "$OPTS2" | jqf "any(o['label']=='清洁中（测试）' for o in d['data'])")"

echo "== 2. 系统参数（T20）"
CFG_GROUPS=$(curl -s $NP "$B/api/tenant/configs" -H "$AUTH")
check "六个必需分组齐全" "True" "$(echo "$CFG_GROUPS" | jqf "set(['站点基础','门店信息','订单规则','护理规则','营销规则','客服']).issubset({g['group'] for g in d['data']})")"
curl -s $NP -o /dev/null -X PUT "$B/api/tenant/configs" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"group":"订单规则","confirm":true,"items":[{"key":"order.deposit.ratio","value":"30","valueType":"number"}]}'
NO_CONFIRM=$(curl -s $NP -o /tmp/c1.json -w "%{http_code}" -X PUT "$B/api/tenant/configs" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"group":"订单规则","confirm":false,"items":[{"key":"order.deposit.ratio","value":"35","valueType":"number"}]}')
check "金额比例变更未确认被拦截" "409" "$NO_CONFIRM"
CONFIRMED=$(curl -s $NP -o /tmp/c2.json -w "%{http_code}" -X PUT "$B/api/tenant/configs" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"group":"订单规则","confirm":true,"items":[{"key":"order.deposit.ratio","value":"35","valueType":"number"}]}')
check "二次确认后保存成功" "200" "$CONFIRMED"
VAL=$(curl -s $NP "$B/api/tenant/configs/order.deposit.ratio" -H "$AUTH")
check "参数即时生效" "35" "$(echo "$VAL" | jqf "d['data']")"
curl -s $NP -o /dev/null -X PUT "$B/api/tenant/configs" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"group":"订单规则","confirm":true,"items":[{"key":"order.deposit.ratio","value":"30","valueType":"number"}]}'
BADTYPE=$(curl -s $NP -o /tmp/c3.json -w "%{http_code}" -X PUT "$B/api/tenant/configs" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"group":"订单规则","confirm":true,"items":[{"key":"order.deposit.ratio","value":"abc","valueType":"number"}]}')
check "类型校验拦截非法数值" "400" "$BADTYPE"

echo "== 3. 多语言（T21）"
PACK=$(curl -s $NP "$B/api/tenant/i18n/pack/en-US" -H "$AUTH")
check "英文语言包已缓存生成" "True" "$(echo "$PACK" | jqf "d['data'].get('common.confirm')=='Confirm'")"
SAVE_MSG=$(curl -s $NP -X PUT "$B/api/tenant/i18n/messages" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"langCode":"en-US","messages":{"smoke.key":"Smoke OK"}}')
check "保存语言包词条" "1" "$(echo "$SAVE_MSG" | jqf "d['data']")"
PACK2=$(curl -s $NP "$B/api/tenant/i18n/pack/en-US" -H "$AUTH")
check "词条保存后即时生效" "Smoke OK" "$(echo "$PACK2" | jqf "d['data'].get('smoke.key')")"
OPEN_PACK=$(curl -s $NP "$B/api/open/i18n/en-US" -H 'X-Tenant-Code: demo')
check "开放接口免登录读语言包" "0" "$(echo "$OPEN_PACK" | jqf "d['code']")"
LANGS=$(curl -s $NP "$B/api/open/i18n/languages" -H 'X-Tenant-Code: demo')
check "开放接口返回默认语言" "zh-CN" "$(echo "$LANGS" | jqf "d['data']['default']")"

echo "== 4. 菜单可视化（T22）"
TAB6=$(curl -s $NP -o /tmp/n1.json -w "%{http_code}" -X PUT "$B/api/tenant/navs" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"endType":"小程序tabBar","nodes":[{"id":null,"title":"首页"},{"id":null,"title":"房型"},{"id":null,"title":"预约"},{"id":null,"title":"订单"},{"id":null,"title":"我的"},{"id":null,"title":"多余"}]}')
check "tabBar 超过 5 项被拦截" "400" "$TAB6"
MP4=$(curl -s $NP -o /tmp/n2.json -w "%{http_code}" -X PUT "$B/api/tenant/navs" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"endType":"公众号","nodes":[{"id":null,"title":"a"},{"id":null,"title":"b"},{"id":null,"title":"c"},{"id":null,"title":"d"}]}')
check "公众号一级超过 3 个被拦截" "400" "$MP4"
MP_TREE=$(curl -s $NP -X PUT "$B/api/tenant/navs" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"endType":"公众号","nodes":[{"id":null,"title":"了解我们"},{"id":null,"title":"房型套餐"},{"id":null,"title":"预约参观"}]}')
check "公众号 3 个一级菜单保存成功" "3" "$(echo "$MP_TREE" | jqf "len(d['data'])")"
ROOT_ID=$(echo "$MP_TREE" | jqf "d['data'][0]['id']")
WITH_CHILD=$(curl -s $NP -X PUT "$B/api/tenant/navs" -H "$AUTH" \
  -H 'Content-Type: application/json' -d "{\"endType\":\"公众号\",\"nodes\":[{\"id\":$ROOT_ID,\"title\":\"了解我们\"},{\"id\":null,\"parentId\":$ROOT_ID,\"title\":\"品牌故事\"}]}" -o /tmp/n3.json -w "%{http_code}")
check "二级菜单保存成功" "200" "$WITH_CHILD"
TREE=$(curl -s $NP "$B/api/tenant/navs?endType=%E5%85%AC%E4%BC%97%E5%8F%B7" -H "$AUTH")
check "树形结构返回子节点" "1" "$(echo "$TREE" | jqf "len(d['data'][0]['children'])")"

echo "== 5. 对象存储（T23/T24）"
CUR=$(curl -s $NP "$B/api/tenant/storage-config" -H "$AUTH")
check "默认本地驱动" "local" "$(echo "$CUR" | jqf "d['data']['driver']")"
TEST_LOCAL=$(curl -s $NP -X POST "$B/api/tenant/storage-config/test" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"driver":"local"}')
check "本地驱动连接测试" "0" "$(echo "$TEST_LOCAL" | jqf "d['code']")"
SAVE_LOCAL=$(curl -s $NP -X PUT "$B/api/tenant/storage-config" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"driver":"local","confirmSwitch":true}')
check "本地驱动保存" "0" "$(echo "$SAVE_LOCAL" | jqf "d['code']")"
NO_TEST=$(curl -s $NP -o /tmp/s1.json -w "%{http_code}" -X PUT "$B/api/tenant/storage-config" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"driver":"minio","endpoint":"http://127.0.0.1:9000","bucket":"yzz","accessKey":"ak","secretKey":"sk","confirmSwitch":true}')
check "未测试直接保存 S3 被拦截" "400" "$NO_TEST"

echo "== 6. 消息配置（T27）"
CH=$(curl -s $NP "$B/api/tenant/messages/channels" -H "$AUTH")
check "4 个消息通道" "4" "$(echo "$CH" | jqf "len(d['data']['message'])")"
check "6 个短信渠道" "6" "$(echo "$CH" | jqf "len(d['data']['sms'])")"
EN1=$(curl -s $NP -X PUT "$B/api/tenant/messages/channels" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"channelCode":"aliyun_sms","enabled":true}')
check "启用阿里云短信" "0" "$(echo "$EN1" | jqf "d['code']")"
EN2=$(curl -s $NP -X PUT "$B/api/tenant/messages/channels" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"channelCode":"local_log","enabled":true}')
check "切换本地日志渠道" "0" "$(echo "$EN2" | jqf "d['code']")"
CH2=$(curl -s $NP "$B/api/tenant/messages/channels" -H "$AUTH")
check "短信渠道互斥仅一条启用" "1" "$(echo "$CH2" | jqf "sum(1 for c in d['data']['sms'] if c['enabled'])")"
SMS_TEST=$(curl -s $NP -X POST "$B/api/tenant/messages/test?channelCode=local_log&phone=13800000000" -H "$AUTH")
check "本地日志渠道测试发送" "0" "$(echo "$SMS_TEST" | jqf "d['code']")"
TPL=$(curl -s $NP -X PUT "$B/api/tenant/messages/templates" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"triggerNode":"定金支付成功","channel":"小程序订阅消息","templateCode":"TM001","fieldMapping":"thing1=客户姓名,time2=入住时间"}')
check "保存消息模板" "0" "$(echo "$TPL" | jqf "d['code']")"
TPL2=$(curl -s $NP -X PUT "$B/api/tenant/messages/templates" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"triggerNode":"定金支付成功","channel":"小程序订阅消息","templateCode":"TM002","fieldMapping":"thing1=客户姓名"}')
TLIST=$(curl -s $NP "$B/api/tenant/messages/templates?triggerNode=%E5%AE%9A%E9%87%91%E6%94%AF%E4%BB%98%E6%88%90%E5%8A%9F" -H "$AUTH")
check "同节点同通道唯一（更新而非新增）" "1" "$(echo "$TLIST" | jqf "len(d['data'])")"

echo "== 7. SSO（T28）"
SSO_SAVE=$(curl -s $NP -X PUT "$B/api/tenant/sso-config" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"enabled":false,"protocol":"OIDC","serverUrl":"https://id.example.com/oauth/authorize","clientId":"yzz","clientSecret":"SuperSecret123","accountMapping":"email","fallbackEnabled":true,"rejectStrategy":"拒绝"}')
check "SSO 保存成功" "0" "$(echo "$SSO_SAVE" | jqf "d['code']")"
check "根地址自动裁剪协议后缀" "https://id.example.com" "$(echo "$SSO_SAVE" | jqf "d['data']['serverUrl']")"
check "Secret 只回掩码" "Supe****t123" "$(echo "$SSO_SAVE" | jqf "d['data']['clientSecretMasked']")"
check "自动拼接三个端点" "3" "$(echo "$SSO_SAVE" | jqf "len(d['data']['endpoints'])")"
SSO_ENABLE=$(curl -s $NP -X PUT "$B/api/tenant/sso-config" -H "$AUTH" \
  -H 'Content-Type: application/json' -d '{"enabled":true,"protocol":"OIDC","serverUrl":"https://id.example.com","clientId":"yzz","accountMapping":"email","fallbackEnabled":true,"rejectStrategy":"拒绝"}')
check "留空密钥沿用已保存值" "0" "$(echo "$SSO_ENABLE" | jqf "d['code']")"
LOGIN_OPT=$(curl -s $NP "$B/api/open/login-options" -H 'X-Tenant-Code: demo')
check "登录页返回 SSO 入口" "True" "$(echo "$LOGIN_OPT" | jqf "d['data']['enabled'] and bool(d['data']['authUrl'])")"
check "SSO 故障可降级账号密码" "True" "$(echo "$LOGIN_OPT" | jqf "d['data']['passwordFallback']")"

echo "== 8. 隔离与鉴权回归"
NO_TOKEN=$(curl -s $NP -o /tmp/t1.json -w "%{http_code}" "$B/api/tenant/configs")
check "无令牌访问后台接口 401" "401" "$NO_TOKEN"
CROSS=$(curl -s $NP -o /tmp/t2.json -w "%{http_code}" "$B/api/tenant/configs" -H "$AUTH" -H 'X-Tenant-Code: other')
check "篡改租户标识 403" "403" "$CROSS"

echo
echo "==== 结果：通过 $PASS 项，失败 $FAIL 项 ===="
[ "$FAIL" -eq 0 ]
