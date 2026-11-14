import 'package:flutter/material.dart';

import '../../core/api.dart';
import '../../core/session.dart';
import '../../core/theme.dart';
import 'order_detail_page.dart';

/// 我的订单：按状态分组，支持下拉刷新与离线缓存。
class OrdersPage extends StatefulWidget {
  const OrdersPage({super.key});

  @override
  State<OrdersPage> createState() => _OrdersPageState();
}

class _OrdersPageState extends State<OrdersPage> {
  static const _tabs = ['全部', '待支付', '待入住', '在住', '待结算', '已完成'];
  String _tab = '全部';
  bool _loading = true;
  List<Map<String, dynamic>> _list = [];
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    if (!Session.logged) {
      setState(() {
        _loading = false;
        _list = [];
      });
      return;
    }
    setState(() => _loading = true);
    try {
      final data = await Api.instance.get(
        '/api/open/customer/orders',
        query: _tab == '全部' ? null : {'status': _tab},
        auth: true,
      ) as List?;
      final list = (data ?? []).map((e) => (e as Map).cast<String, dynamic>()).toList();
      await Session.cache('orders', list);
      if (!mounted) return;
      setState(() {
        _list = list;
        _loading = false;
        _error = null;
      });
    } on UnauthorizedException {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _list = [];
      });
    } on ApiException catch (e) {
      final cached = await Session.readCache('orders');
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = e.message;
        if (cached is List) {
          _list = cached.map((e) => (e as Map).cast<String, dynamic>()).toList();
        }
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('我的订单'),
        actions: [IconButton(onPressed: _load, icon: const Icon(Icons.refresh))],
      ),
      body: Column(
        children: [
          SizedBox(
            height: 52,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              itemCount: _tabs.length,
              separatorBuilder: (_, __) => const SizedBox(width: 8),
              itemBuilder: (context, index) {
                final tab = _tabs[index];
                final on = tab == _tab;
                return ChoiceChip(
                  label: Text(tab),
                  selected: on,
                  showCheckmark: false,
                  selectedColor: AppColors.brand700,
                  labelStyle: TextStyle(fontSize: 13, color: on ? Colors.white : AppColors.ink3),
                  onSelected: (_) {
                    setState(() => _tab = tab);
                    _load();
                  },
                );
              },
            ),
          ),
          Expanded(
            child: RefreshIndicator(
              onRefresh: _load,
              child: _loading && _list.isEmpty
                  ? const Center(child: CircularProgressIndicator())
                  : ListView(
                      padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
                      children: [
                        if (_error != null)
                          Padding(
                            padding: const EdgeInsets.only(bottom: 12),
                            child: Text(_error!, style: const TextStyle(fontSize: 12, color: AppColors.danger)),
                          ),
                        if (!Session.logged)
                          _hintCard('登录后查看订单', '请在「我的」中使用手机号验证码登录')
                        else if (_list.isEmpty)
                          _hintCard('暂无$_tab订单', '选定房型与套餐后可在线锁定档期')
                        else
                          ..._list.map(_orderCard),
                      ],
                    ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _orderCard(Map<String, dynamic> row) {
    final total = (row['totalAmount'] as num?)?.toDouble() ?? 0;
    final paid = (row['paidAmount'] as num?)?.toDouble() ?? 0;
    final rate = total > 0 ? (paid / total).clamp(0.0, 1.0) : 0.0;
    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: () => Navigator.of(context).push(
          MaterialPageRoute(
            builder: (_) => OrderDetailPage(orderNo: row['orderNo'].toString()),
          ),
        ),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(row['orderNo']?.toString() ?? '',
                      style: const TextStyle(fontSize: 12, color: AppColors.ink3)),
                  _statusTag(row['status']?.toString() ?? ''),
                ],
              ),
              const SizedBox(height: 8),
              Text('${row['checkinDate']} 入馆  →  ${row['checkoutDate']} 出馆',
                  style: const TextStyle(fontSize: 14, color: AppColors.ink)),
              Text('${row['customerName'] ?? ''} · ${row['careMode'] ?? ''}',
                  style: const TextStyle(fontSize: 12, color: AppColors.ink3)),
              const SizedBox(height: 12),
              Row(
                children: [
                  _amount('应付', '¥$total'),
                  const SizedBox(width: 18),
                  _amount('已付', '¥$paid'),
                  const SizedBox(width: 18),
                  _amount('待付', '¥${row['tailAmount'] ?? 0}'),
                ],
              ),
              const SizedBox(height: 10),
              ClipRRect(
                borderRadius: BorderRadius.circular(999),
                child: LinearProgressIndicator(
                  value: rate,
                  minHeight: 6,
                  backgroundColor: AppColors.paper2,
                  color: AppColors.brand500,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _amount(String label, String value) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 11, color: AppColors.ink4)),
        Text(value, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: AppColors.ink)),
      ],
    );
  }

  Widget _statusTag(String status) {
    final warn = status == '待支付';
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
      decoration: BoxDecoration(
        color: warn ? const Color(0xFFFDF5E8) : AppColors.brand100,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(status,
          style: TextStyle(fontSize: 11, color: warn ? AppColors.warn : AppColors.brand700)),
    );
  }

  Widget _hintCard(String title, String desc) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            Text(title, style: const TextStyle(fontSize: 15, color: AppColors.ink)),
            const SizedBox(height: 6),
            Text(desc, textAlign: TextAlign.center, style: const TextStyle(fontSize: 12, color: AppColors.ink4)),
          ],
        ),
      ),
    );
  }
}
