import 'package:flutter/material.dart';

import '../../core/api.dart';
import '../../core/session.dart';
import '../../core/theme.dart';

/// 我的：会员资产、护理日志入口、消息与退出。
class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key});

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  bool _loading = false;
  Map<String, dynamic>? _profile;
  String? _error;

  @override
  void initState() {
    super.initState();
    _profile = Session.profile;
    _load();
  }

  Future<void> _load() async {
    if (!Session.logged) return;
    setState(() => _loading = true);
    try {
      final data = await Api.instance.get('/api/open/member/profile', auth: true) as Map<String, dynamic>;
      await Session.updateProfile(data);
      if (!mounted) return;
      setState(() {
        _profile = data;
        _loading = false;
        _error = null;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = e.message;
      });
    }
  }

  Future<void> _logout() async {
    await Session.clear();
    if (!mounted) return;
    Navigator.of(context).pushReplacementNamed('/login');
  }

  @override
  Widget build(BuildContext context) {
    final name = _profile?['nickname']?.toString() ?? '未登录';
    final phone = _profile?['phone']?.toString() ?? '';

    return Scaffold(
      appBar: AppBar(
        title: const Text('我的'),
        actions: [IconButton(onPressed: _load, icon: const Icon(Icons.refresh))],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [AppColors.brand900, AppColors.brand500],
              ),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Row(
              children: [
                Container(
                  width: 56,
                  height: 56,
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.2),
                    shape: BoxShape.circle,
                  ),
                  alignment: Alignment.center,
                  child: Text(
                    name.isEmpty ? '会' : name.substring(0, 1),
                    style: const TextStyle(color: Colors.white, fontSize: 22),
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(name, style: const TextStyle(color: Colors.white, fontSize: 17, fontWeight: FontWeight.w600)),
                      Text(phone, style: TextStyle(color: Colors.white.withValues(alpha: 0.75), fontSize: 12)),
                    ],
                  ),
                ),
                if (!Session.logged)
                  TextButton(
                    onPressed: () => Navigator.of(context).pushReplacementNamed('/login'),
                    child: const Text('登录', style: TextStyle(color: Colors.white)),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 16),
              child: Row(
                children: [
                  _asset('${_profile?['points'] ?? 0}', '积分'),
                  _asset('${_profile?['balance'] ?? '0.00'}', '储值余额'),
                  _asset('${_profile?['couponCount'] ?? 0}', '可用券'),
                ],
              ),
            ),
          ),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Text(_error!, style: const TextStyle(fontSize: 12, color: AppColors.danger)),
            ),
          Card(
            child: Column(
              children: [
                _entry(Icons.receipt_long_outlined, '我的订单', () {
                  Navigator.of(context).pop();
                }),
                _entry(Icons.monitor_heart_outlined, '护理日志', () {
                  Navigator.of(context).push(MaterialPageRoute(builder: (_) => const _CareLogPage()));
                }),
                _entry(Icons.local_florist_outlined, '康复与月子餐', () {
                  Navigator.of(context).pop();
                }),
              ],
            ),
          ),
          const SizedBox(height: 12),
          Card(
            child: Column(
              children: [
                _entry(Icons.notifications_none, '消息通知', () {
                  Navigator.of(context).push(MaterialPageRoute(builder: (_) => const _NoticePage()));
                }),
                _entry(Icons.privacy_tip_outlined, '隐私与协议', () {
                  showDialog<void>(
                    context: context,
                    builder: (context) => AlertDialog(
                      title: const Text('隐私与协议'),
                      content: const Text(
                        '我们仅收集服务必需信息（姓名、手机号、孕产周期、护理记录），'
                        '用于预约、护理执行与售后；手机号加密存储，展示一律脱敏；'
                        '未经同意不向第三方提供。可随时联系门店查询、更正或删除。',
                      ),
                      actions: [
                        TextButton(onPressed: () => Navigator.pop(context), child: const Text('知道了')),
                      ],
                    ),
                  );
                }),
              ],
            ),
          ),
          const SizedBox(height: 12),
          if (Session.logged)
            OutlinedButton(onPressed: _logout, child: const Text('退出登录')),
          if (_loading)
            const Padding(
              padding: EdgeInsets.only(top: 12),
              child: Center(child: SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))),
            ),
        ],
      ),
    );
  }

  Widget _asset(String value, String label) {
    return Expanded(
      child: Column(
        children: [
          Text(value, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600, color: AppColors.brand700)),
          const SizedBox(height: 2),
          Text(label, style: const TextStyle(fontSize: 11, color: AppColors.ink4)),
        ],
      ),
    );
  }

  Widget _entry(IconData icon, String title, VoidCallback onTap) {
    return ListTile(
      leading: Icon(icon, color: AppColors.brand700, size: 20),
      title: Text(title, style: const TextStyle(fontSize: 14)),
      trailing: const Icon(Icons.chevron_right, size: 18, color: AppColors.ink4),
      onTap: onTap,
    );
  }
}

/// 护理日志：护理计划进度、今日项目与体征。
class _CareLogPage extends StatefulWidget {
  const _CareLogPage();

  @override
  State<_CareLogPage> createState() => _CareLogPageState();
}

class _CareLogPageState extends State<_CareLogPage> {
  bool _loading = true;
  Map<String, dynamic>? _care;
  int? _orderId;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final stay = await Api.instance.get('/api/open/customer/stay', auth: true) as Map<String, dynamic>;
      if (stay['living'] != true) {
        if (!mounted) return;
        setState(() {
          _loading = false;
          _orderId = null;
        });
        return;
      }
      final orderId = (((stay['order'] as Map)['id']) as num).toInt();
      final care = await Api.instance.get('/api/open/customer/care', query: {'orderId': '$orderId'}, auth: true);
      if (!mounted) return;
      setState(() {
        _orderId = orderId;
        _care = (care as Map).cast<String, dynamic>();
        _loading = false;
        _error = null;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = e.message;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final records = (_care?['records'] as List?) ?? [];
    final today = (_care?['today'] as List?) ?? [];
    return Scaffold(
      appBar: AppBar(title: const Text('护理日志')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _orderId == null
              ? const Center(child: Text('暂未入馆，入馆后可查看护理日志'))
              : ListView(
                  padding: const EdgeInsets.all(16),
                  children: [
                    if (_error != null)
                      Text(_error!, style: const TextStyle(fontSize: 12, color: AppColors.danger)),
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('计划完成 ${_care?['rate'] ?? 0}%',
                                style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600)),
                            const SizedBox(height: 8),
                            ClipRRect(
                              borderRadius: BorderRadius.circular(999),
                              child: LinearProgressIndicator(
                                value: ((_care?['rate'] ?? 0) as num).toDouble() / 100,
                                minHeight: 8,
                                backgroundColor: AppColors.paper2,
                                color: AppColors.brand500,
                              ),
                            ),
                            const SizedBox(height: 10),
                            Text('今日 ${today.length} 项护理',
                                style: const TextStyle(fontSize: 12, color: AppColors.ink3)),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),
                    const Text('护理记录', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 8),
                    if (records.isEmpty)
                      const Card(
                        child: Padding(
                          padding: EdgeInsets.all(20),
                          child: Text('暂无护理记录，护理员完成操作后会实时同步',
                              style: TextStyle(fontSize: 12, color: AppColors.ink4)),
                        ),
                      )
                    else
                      ...records.take(30).map((row) {
                        final map = (row as Map).cast<String, dynamic>();
                        return Card(
                          child: Padding(
                            padding: const EdgeInsets.all(14),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                  children: [
                                    Text('${map['itemName']} · ${map['targetType']}',
                                        style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
                                    Text(map['submitTime']?.toString().split('T').first ?? '',
                                        style: const TextStyle(fontSize: 11, color: AppColors.ink4)),
                                  ],
                                ),
                                const SizedBox(height: 6),
                                Text(map['content']?.toString() ?? '',
                                    style: const TextStyle(fontSize: 13, color: AppColors.ink2)),
                                if (map['temperature'] != null)
                                  Padding(
                                    padding: const EdgeInsets.only(top: 6),
                                    child: Text('体温 ${map['temperature']}℃',
                                        style: const TextStyle(fontSize: 11, color: AppColors.ink3)),
                                  ),
                              ],
                            ),
                          ),
                        );
                      }),
                  ],
                ),
    );
  }
}

/// 消息：待办汇总（异常、交接、出餐）。
class _NoticePage extends StatefulWidget {
  const _NoticePage();

  @override
  State<_NoticePage> createState() => _NoticePageState();
}

class _NoticePageState extends State<_NoticePage> {
  bool _loading = true;
  List<Map<String, dynamic>> _list = [];
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final stay = await Api.instance.get('/api/open/customer/stay', auth: true) as Map<String, dynamic>;
      final items = <Map<String, dynamic>>[];
      if (stay['living'] == true) {
        final orderId = (((stay['order'] as Map)['id']) as num).toInt();
        final care = await Api.instance.get('/api/open/customer/care', query: {'orderId': '$orderId'}, auth: true);
        final records = ((care as Map)['records'] as List?) ?? [];
        for (final row in records.take(10)) {
          final map = (row as Map).cast<String, dynamic>();
          items.add({
            'title': map['itemName']?.toString() ?? '护理更新',
            'desc': map['content']?.toString() ?? '',
            'time': map['submitTime']?.toString().split('T').first ?? '',
          });
        }
      }
      if (!mounted) return;
      setState(() {
        _list = items;
        _loading = false;
        _error = null;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = e.message;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('消息通知')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                if (_error != null)
                  Text(_error!, style: const TextStyle(fontSize: 12, color: AppColors.danger)),
                if (_list.isEmpty)
                  const Card(
                    child: Padding(
                      padding: EdgeInsets.all(20),
                      child: Text('暂无消息', style: TextStyle(fontSize: 12, color: AppColors.ink4)),
                    ),
                  )
                else
                  ..._list.map((item) => Card(
                        child: ListTile(
                          title: Text(item['title'] ?? '', style: const TextStyle(fontSize: 14)),
                          subtitle: Text(item['desc'] ?? '',
                              style: const TextStyle(fontSize: 12, color: AppColors.ink3)),
                          trailing: Text(item['time'] ?? '',
                              style: const TextStyle(fontSize: 11, color: AppColors.ink4)),
                        ),
                      )),
              ],
            ),
    );
  }
}
