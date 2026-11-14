import 'package:flutter/material.dart';

import '../../core/api.dart';
import '../../core/theme.dart';

/// 订单详情：金额构成、收款记录、退款与合同，支持申请退订。
class OrderDetailPage extends StatefulWidget {
  const OrderDetailPage({super.key, required this.orderNo});

  final String orderNo;

  @override
  State<OrderDetailPage> createState() => _OrderDetailPageState();
}

class _OrderDetailPageState extends State<OrderDetailPage> {
  bool _loading = true;
  String? _error;
  Map<String, dynamic>? _detail;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final data = await Api.instance.get('/api/open/customer/orders/${widget.orderNo}', auth: true);
      if (!mounted) return;
      setState(() {
        _detail = (data as Map).cast<String, dynamic>();
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

  Future<void> _applyRefund() async {
    final controller = TextEditingController();
    final reason = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('申请退订'),
        content: TextField(
          controller: controller,
          maxLines: 3,
          decoration: const InputDecoration(hintText: '请填写退订原因（必填）'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('取消')),
          FilledButton(
            onPressed: () => Navigator.pop(context, controller.text.trim()),
            child: const Text('提交'),
          ),
        ],
      ),
    );
    if (reason == null || reason.isEmpty) return;
    try {
      await Api.instance.post(
        '/api/open/customer/orders/${widget.orderNo}/refund',
        body: {'reason': reason},
        auth: true,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('退订申请已提交，预计 3 个工作日内审核')),
      );
      _load();
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final order = (_detail?['order'] as Map?)?.cast<String, dynamic>();
    final items = (_detail?['items'] as List?) ?? [];
    final payments = (_detail?['payments'] as List?) ?? [];
    final refunds = (_detail?['refunds'] as List?) ?? [];

    return Scaffold(
      appBar: AppBar(title: Text(widget.orderNo)),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!, style: const TextStyle(color: AppColors.danger)))
              : ListView(
                  padding: const EdgeInsets.all(16),
                  children: [
                    _block('订单信息', [
                      _kv('状态', order?['status']?.toString() ?? ''),
                      _kv('入住人', order?['customerName']?.toString() ?? ''),
                      _kv('联系电话', order?['phone']?.toString() ?? ''),
                      _kv('入离馆', '${order?['checkinDate']} ~ ${order?['checkoutDate']}'),
                      _kv('护理模式', order?['careMode']?.toString() ?? ''),
                    ]),
                    _block('金额构成', [
                      _kv('套餐金额', '¥${order?['packageAmount'] ?? 0}'),
                      _kv('增值服务', '¥${order?['extraAmount'] ?? 0}'),
                      _kv('商品', '¥${order?['productAmount'] ?? 0}'),
                      _kv('优惠减免', '-¥${order?['discountAmount'] ?? 0}'),
                      _kv('押金', '¥${order?['bondAmount'] ?? 0}'),
                      _kv('应付总额', '¥${order?['totalAmount'] ?? 0}', strong: true),
                      _kv('已付 / 待付', '¥${order?['paidAmount'] ?? 0} / ¥${order?['tailAmount'] ?? 0}'),
                    ]),
                    if (items.isNotEmpty)
                      _block('费用明细', [
                        for (final item in items)
                          _kv('${(item as Map)['itemName']} ×${item['quantity']}', '¥${item['amount']}'),
                      ]),
                    if (payments.isNotEmpty)
                      _block('收款记录', [
                        for (final row in payments)
                          _kv('${(row as Map)['payType']} · ${row['method']}',
                              '¥${row['amount']}（${row['status']}）'),
                      ]),
                    if (refunds.isNotEmpty)
                      _block('退款记录', [
                        for (final row in refunds)
                          _kv('${(row as Map)['reason']}', '¥${row['amount']}（${row['status']}）'),
                      ]),
                    const SizedBox(height: 8),
                    if (order != null && ['待入住', '在住', '待结算'].contains(order['status']))
                      FilledButton.tonal(onPressed: _applyRefund, child: const Text('申请退订')),
                  ],
                ),
    );
  }

  Widget _block(String title, List<Widget> children) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: AppColors.ink)),
            const SizedBox(height: 8),
            ...children,
          ],
        ),
      ),
    );
  }

  Widget _kv(String key, String value, {bool strong = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(key, style: const TextStyle(fontSize: 13, color: AppColors.ink3)),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: TextStyle(
                fontSize: strong ? 15 : 13,
                fontWeight: strong ? FontWeight.w600 : FontWeight.normal,
                color: strong ? AppColors.brand700 : AppColors.ink,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
