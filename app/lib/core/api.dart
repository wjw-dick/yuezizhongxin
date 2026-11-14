import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

import 'session.dart';

/// 统一 API 封装：注入租户标识与会员令牌，统一解包 {code,msg,data}。
///
/// 40100 时清理登录态并抛 [UnauthorizedException]，由界面引导重新登录。
class Api {
  Api._();

  static final Api instance = Api._();

  /// 通过 --dart-define=API_BASE=... 注入，默认 Android 模拟器访问本机。
  static const String baseUrl =
      String.fromEnvironment('API_BASE', defaultValue: 'http://10.0.2.2:8080');

  static const String tenantCode =
      String.fromEnvironment('TENANT', defaultValue: 'demo');

  Future<dynamic> get(String path, {Map<String, String>? query, bool auth = false}) {
    final uri = Uri.parse('$baseUrl$path').replace(queryParameters: query);
    return _send(() => http.get(uri, headers: _headers(auth)));
  }

  Future<dynamic> post(String path, {Object? body, bool auth = false}) {
    final uri = Uri.parse('$baseUrl$path');
    return _send(() => http.post(
          uri,
          headers: _headers(auth),
          body: body == null ? null : jsonEncode(body),
        ));
  }

  Future<dynamic> delete(String path, {bool auth = false}) {
    final uri = Uri.parse('$baseUrl$path');
    return _send(() => http.delete(uri, headers: _headers(auth)));
  }

  Map<String, String> _headers(bool auth) {
    final headers = <String, String>{
      'content-type': 'application/json',
      'X-Tenant-Code': tenantCode,
    };
    if (auth && Session.token != null) {
      headers['X-Member-Token'] = Session.token!;
    }
    return headers;
  }

  Future<dynamic> _send(Future<http.Response> Function() request) async {
    late http.Response response;
    try {
      response = await request().timeout(const Duration(seconds: 20));
    } on SocketException {
      throw ApiException('网络不可用，已展示离线缓存');
    } on HttpException {
      throw ApiException('服务暂时不可用，请稍后重试');
    }
    final text = utf8.decode(response.bodyBytes);
    final Map<String, dynamic> payload =
        text.isEmpty ? <String, dynamic>{} : jsonDecode(text) as Map<String, dynamic>;
    final code = payload['code'] ?? -1;
    if (code == 0) {
      return payload['data'];
    }
    if (code == 40100) {
      await Session.clear();
      throw UnauthorizedException(payload['msg']?.toString() ?? '登录已过期');
    }
    throw ApiException(payload['msg']?.toString() ?? '请求失败');
  }
}

class ApiException implements Exception {
  ApiException(this.message);
  final String message;
  @override
  String toString() => message;
}

class UnauthorizedException extends ApiException {
  UnauthorizedException(super.message);
}
