import 'package:flutter/material.dart';

import '../../core/api.dart';
import '../../core/session.dart';
import '../../core/theme.dart';

/// 首页：在住概览 + 套餐与房型入口。
///
/// 弱网时先渲染离线缓存，再静默刷新，避免白屏。
class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  bool _loading = true;
  String? _error;
  Map<String, dynamic>? _home;
  Map<String, dynamic>? _stay;
  bool _offline = false;

  @override
  void initState() {
    super.initState();
    _bootstrap();
  }

  Future<void> _bootstrap() async {
    final cached = await Session.readCache('home');
    if (cached is Map) {
      setState(() {
        _home = cached.cast<String, dynamic>();
        _loading = false;
      });
    }
    await _load();
  }

  Future<void> _load() async {
    try {
      final home = await Api.instance.get('/api/open/site/home') as Map<String, dynamic>;
      Map<String, dynamic>? stay;
      if (Session.logged) {
        final data = await Api.instance.get('/api/open/customer/stay', auth: true);
        if (data is Map<String, dynamic> && data['living'] == true) {
          stay = data;
        }
      }
      await Session.cache('home', home);
      if (!mounted) return;
      setState(() {
        _home = home;
        _stay = stay;
        _loading = false;
        _offline = false;
        _error = null;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _offline = _home != null;
        _error = _home == null ? e.message : null;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final site = (_home?['site'] as Map?)?.cast<String, dynamic>() ?? {};
    final packages = (_home?['packages'] as List?) ?? [];
    final rooms = (_home?['roomTypes'] as List?) ?? [];

    return Scaffold(
      appBar: AppBar(
        title: Text(site['name']?.toString() ?? '月之孕月子中心'),
        actions: [
          IconButton(onPressed: _load, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            if (_offline)
              Container(
                margin: const EdgeInsets.only(bottom: 12),
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                decoration: BoxDecoration(
                  color: const Color(0xFFFDF5E8),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Text('当前为离线缓存数据，下拉可重新同步',
                    style: TextStyle(fontSize: 12, color: AppColors.warn)),
              ),
            _hero(),
            if (_stay != null) _livingCard(_stay!),
            const SizedBox(height: 20),
            _sectionTitle('月子套餐', '共 ${packages.length} 个'),
            if (_loading && packages.isEmpty)
              const Padding(padding: EdgeInsets.all(24), child: Center(child: CircularProgressIndicator()))
            else if (packages.isEmpty)
              _empty('套餐整理中', '可先致电门店了解档期')
            else
              ...packages.take(3).map((pkg) => _packageCard(pkg as Map<String, dynamic>)),
            const SizedBox(height: 20),
            _sectionTitle('房型实拍', '共 ${rooms.length} 种'),
            if (rooms.isEmpty)
              _empty('房型整理中', '到店可参观真实在住房间')
            else
              ...rooms.take(4).map((room) => _roomCard(room as Map<String, dynamic>)),
            if (_error != null)
              Padding(
                padding: const EdgeInsets.only(top: 16),
                child: Text(_error!, style: const TextStyle(color: AppColors.danger)),
              ),
          ],
        ),
      ),
    );
  }

  Widget _hero() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [AppColors.brand900, AppColors.brand500],
        ),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('28 天，把坐月子\n交给一支持证团队',
              style: TextStyle(color: Colors.white, fontSize: 22, height: 1.35, fontWeight: FontWeight.w600)),
          const SizedBox(height: 10),
          Text('独立月子房 · 一对一护理 · 四阶段月子餐',
              style: TextStyle(color: Colors.white.withValues(alpha: 0.82), fontSize: 13)),
          const SizedBox(height: 18),
          Row(
            children: [
              _metric('1:1', '护理配比'),
              const SizedBox(width: 24),
              _metric('42', '天康复跟踪'),
              const SizedBox(width: 24),
              _metric('4', '阶段月子餐'),
            ],
          ),
        ],
      ),
    );
  }

  Widget _metric(String value, String label) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(value, style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.w600)),
        Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 11)),
      ],
    );
  }

  Widget _livingCard(Map<String, dynamic> stay) {
    final order = (stay['order'] as Map?)?.cast<String, dynamic>() ?? {};
    final care = (stay['care'] as Map?)?.cast<String, dynamic>() ?? {};
    return Card(
      margin: const EdgeInsets.only(top: 16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text('在住服务', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                Text('${care['rate'] ?? 0}% 计划完成',
                    style: const TextStyle(fontSize: 12, color: AppColors.brand700)),
              ],
            ),
            const SizedBox(height: 10),
            Text('${order['customerName'] ?? ''} · ${stay['roomNo'] ?? '房间待分配'}',
                style: const TextStyle(fontSize: 14, color: AppColors.ink2)),
            Text('${order['checkinDate']} 入馆 ~ ${order['checkoutDate']} 出馆',
                style: const TextStyle(fontSize: 12, color: AppColors.ink3)),
            const SizedBox(height: 10),
            ClipRRect(
              borderRadius: BorderRadius.circular(999),
              child: LinearProgressIndicator(
                value: ((care['rate'] ?? 0) as num).toDouble() / 100,
                minHeight: 8,
                backgroundColor: AppColors.paper2,
                color: AppColors.brand500,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _sectionTitle(String title, String trailing) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600, color: AppColors.ink)),
          Text(trailing, style: const TextStyle(fontSize: 12, color: AppColors.ink4)),
        ],
      ),
    );
  }

  Widget _packageCard(Map<String, dynamic> pkg) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(pkg['name']?.toString() ?? '套餐',
                      style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: AppColors.ink)),
                ),
                Text('¥${pkg['price']}',
                    style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: AppColors.brand700)),
              ],
            ),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              children: [
                _tag('${pkg['days']} 天'),
                _tag(pkg['roomTypeLevel']?.toString() ?? ''),
                _tag(pkg['careMode']?.toString() ?? ''),
              ],
            ),
            const SizedBox(height: 8),
            Text(pkg['rehabItems']?.toString() ?? '含基础康复评估与月子餐',
                style: const TextStyle(fontSize: 12, color: AppColors.ink3)),
          ],
        ),
      ),
    );
  }

  Widget _roomCard(Map<String, dynamic> room) {
    final images = (room['images'] as List?) ?? [];
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(12),
              child: images.isEmpty
                  ? Container(
                      width: 92,
                      height: 68,
                      color: AppColors.paper2,
                      alignment: Alignment.center,
                      child: const Text('房型图', style: TextStyle(fontSize: 11, color: AppColors.ink4)),
                    )
                  : Image.network(images.first.toString(), width: 92, height: 68, fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => Container(width: 92, height: 68, color: AppColors.paper2)),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(room['name']?.toString() ?? '房型',
                      style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: AppColors.ink)),
                  const SizedBox(height: 4),
                  Text('${room['area']}㎡ · ${room['bedType']} · 可住 ${room['maxGuest']} 人',
                      style: const TextStyle(fontSize: 12, color: AppColors.ink3)),
                  Text(room['facilities']?.toString() ?? '',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 12, color: AppColors.ink3)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _tag(String text) {
    if (text.isEmpty) return const SizedBox.shrink();
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 3),
      decoration: BoxDecoration(color: AppColors.brand100, borderRadius: BorderRadius.circular(999)),
      child: Text(text, style: const TextStyle(fontSize: 11, color: AppColors.brand700)),
    );
  }

  Widget _empty(String title, String desc) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            Text(title, style: const TextStyle(fontSize: 14, color: AppColors.ink2)),
            const SizedBox(height: 4),
            Text(desc, style: const TextStyle(fontSize: 12, color: AppColors.ink4)),
          ],
        ),
      ),
    );
  }
}
