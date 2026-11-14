import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

/// 登录态与轻量离线缓存。
///
/// 令牌只存本地，不上报；离线缓存用于弱网展示订单与护理摘要。
class Session {
  Session._();

  static String? token;
  static Map<String, dynamic>? profile;

  static const _tokenKey = 'memberToken';
  static const _profileKey = 'memberProfile';

  static Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    token = prefs.getString(_tokenKey);
    final raw = prefs.getString(_profileKey);
    if (raw != null && raw.isNotEmpty) {
      profile = jsonDecode(raw) as Map<String, dynamic>;
    }
  }

  static Future<void> save(String newToken, Map<String, dynamic> newProfile) async {
    token = newToken;
    profile = newProfile;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_tokenKey, newToken);
    await prefs.setString(_profileKey, jsonEncode(newProfile));
  }

  static Future<void> updateProfile(Map<String, dynamic> newProfile) async {
    profile = newProfile;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_profileKey, jsonEncode(newProfile));
  }

  static Future<void> clear() async {
    token = null;
    profile = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);
    await prefs.remove(_profileKey);
  }

  static bool get logged => token != null && token!.isNotEmpty;

  /// 离线缓存：键前缀 + 路径，最多保留 7 天由系统清理策略接管。
  static Future<void> cache(String key, Object data) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('cache:$key', jsonEncode({
      'time': DateTime.now().millisecondsSinceEpoch,
      'data': data,
    }));
  }

  static Future<dynamic> readCache(String key) async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString('cache:$key');
    if (raw == null) return null;
    final map = jsonDecode(raw) as Map<String, dynamic>;
    return map['data'];
  }
}
