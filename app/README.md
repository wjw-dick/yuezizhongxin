# Flutter 客户 App（Android / iOS）

月子中心客户 App：登录会员、浏览套餐、查看在住护理日志、康复与月子餐、消息推送。

## 目录

```
lib/
  main.dart                 应用入口、主题与路由
  core/api.dart             统一 HTTP 封装（租户头 + 会员令牌）
  core/session.dart         登录态与本地缓存
  core/theme.dart           设计 token（暖白/墨绿/古铜）
  models/order.dart         订单与在住模型
  features/login/           手机号验证码登录
  features/home/            首页（套餐、房型、门店）
  features/orders/          我的订单与详情（含离线缓存）
  features/care/            护理日志与体征趋势
  features/services/        康复预约与月子餐
  features/profile/         会员中心
  features/notify/          消息与本地提醒
```

## 运行

```bash
cd app
flutter pub get
# Android 模拟器访问本机后端用 10.0.2.2，真机改为局域网 IP
flutter run --dart-define=API_BASE=http://10.0.2.2:8080 --dart-define=TENANT=demo
```

## 打包

```bash
flutter build apk --release --dart-define=API_BASE=https://api.yuezizhongxin.com --dart-define=TENANT=demo
flutter build ios --release --dart-define=API_BASE=https://api.yuezizhongxin.com --dart-define=TENANT=demo
```

上架材料（隐私政策、权限说明、截图）见 `docs/manual.md` 附录。
