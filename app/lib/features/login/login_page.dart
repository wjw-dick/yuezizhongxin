import 'package:flutter/material.dart';

import '../../core/api.dart';
import '../../core/session.dart';
import '../../core/theme.dart';

/// 手机号 + 验证码登录；未建档自动注册，与小程序/H5 共用同一会员体系。
class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _phone = TextEditingController();
  final _code = TextEditingController();
  String? _devCode;
  String? _error;
  bool _loading = false;
  int _countdown = 0;

  @override
  void dispose() {
    _phone.dispose();
    _code.dispose();
    super.dispose();
  }

  bool get _phoneValid => RegExp(r'^1[3-9]\d{9}$').hasMatch(_phone.text.trim());

  Future<void> _sendCode() async {
    if (!_phoneValid) {
      setState(() => _error = '请输入正确的 11 位手机号');
      return;
    }
    setState(() => _error = null);
    try {
      final res = await Api.instance.post('/api/open/member/sms', body: {'phone': _phone.text.trim()});
      setState(() {
        _devCode = res is Map ? res['devCode']?.toString() : null;
        _countdown = 60;
      });
      _tick();
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    }
  }

  void _tick() {
    if (_countdown <= 0) return;
    Future.delayed(const Duration(seconds: 1), () {
      if (!mounted) return;
      setState(() => _countdown -= 1);
      _tick();
    });
  }

  Future<void> _login() async {
    if (!_phoneValid || _code.text.trim().length < 4) {
      setState(() => _error = '请输入手机号与验证码');
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await Api.instance.post('/api/open/member/login', body: {
        'phone': _phone.text.trim(),
        'code': _code.text.trim(),
        'source': 'App',
      }) as Map<String, dynamic>;
      await Session.save(res['token'].toString(), (res['profile'] as Map).cast<String, dynamic>());
      if (!mounted) return;
      Navigator.of(context).pushReplacementNamed('/shell');
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 48, 24, 32),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 56,
                height: 56,
                decoration: BoxDecoration(
                  color: AppColors.brand700,
                  borderRadius: BorderRadius.circular(16),
                ),
                alignment: Alignment.center,
                child: const Text('月', style: TextStyle(color: Colors.white, fontSize: 26)),
              ),
              const SizedBox(height: 20),
              const Text('会员登录',
                  style: TextStyle(fontSize: 26, fontWeight: FontWeight.w600, color: AppColors.ink)),
              const SizedBox(height: 8),
              const Text('同一手机号自动关联订单、护理记录与积分权益',
                  style: TextStyle(fontSize: 14, color: AppColors.ink3)),
              const SizedBox(height: 28),
              TextField(
                controller: _phone,
                keyboardType: TextInputType.phone,
                maxLength: 11,
                decoration: const InputDecoration(labelText: '手机号', counterText: '', hintText: '11 位手机号'),
              ),
              const SizedBox(height: 14),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _code,
                      keyboardType: TextInputType.number,
                      maxLength: 6,
                      decoration: const InputDecoration(labelText: '验证码', counterText: '', hintText: '6 位验证码'),
                    ),
                  ),
                  const SizedBox(width: 10),
                  SizedBox(
                    height: 48,
                    child: OutlinedButton(
                      onPressed: _countdown > 0 ? null : _sendCode,
                      child: Text(_countdown > 0 ? '${_countdown}s' : '获取验证码'),
                    ),
                  ),
                ],
              ),
              if (_devCode != null) ...[
                const SizedBox(height: 8),
                Text('联调验证码：$_devCode（正式环境不显示）',
                    style: const TextStyle(fontSize: 12, color: AppColors.warn)),
              ],
              if (_error != null) ...[
                const SizedBox(height: 10),
                Text(_error!, style: const TextStyle(fontSize: 13, color: AppColors.danger)),
              ],
              const SizedBox(height: 22),
              FilledButton(
                onPressed: _loading ? null : _login,
                child: Text(_loading ? '登录中…' : '登录 / 注册'),
              ),
              const SizedBox(height: 14),
              const Text('登录即表示同意《用户协议》与《隐私政策》',
                  style: TextStyle(fontSize: 12, color: AppColors.ink4)),
            ],
          ),
        ),
      ),
    );
  }
}
