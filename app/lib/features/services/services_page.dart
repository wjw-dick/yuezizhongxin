import 'package:flutter/material.dart';

import '../../core/api.dart';
import '../../core/session.dart';
import '../../core/theme.dart';

/// 服务页：康复疗程余额与预约、今日月子餐与忌口。
class ServicesPage extends StatefulWidget {
  const ServicesPage({super.key});

  @override
  State<ServicesPage> createState() => _ServicesPageState();
}

class _ServicesPageState extends State<ServicesPage> with SingleTickerProviderStateMixin {
  late final TabController _tabs = TabController(length: 2, vsync: this);
  bool _loading = true;
  String? _error;
  int? _orderId;
  Map<String, dynamic> _rehab = {};
  Map<String, dynamic> _meal = {};

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _tabs.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    if (!Session.logged) {
      setState(() => _loading = false);
      return;
    }
    setState(() => _loading = true);
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
      final order = (stay['order'] as Map).cast<String, dynamic>();
      final orderId = (order['id'] as num).toInt();
      final results = await Future.wait([
        Api.instance.get('/api/open/customer/rehab', query: {'orderId': '$orderId'}, auth: true),
        Api.instance.get('/api/open/customer/meal', query: {'orderId': '$orderId'}, auth: true),
      ]);
      await Session.cache('services', {'rehab': results[0], 'meal': results[1]});
      if (!mounted) return;
      setState(() {
        _orderId = orderId;
        _rehab = (results[0] as Map).cast<String, dynamic>();
        _meal = (results[1] as Map).cast<String, dynamic>();
        _loading = false;
        _error = null;
      });
    } on ApiException catch (e) {
      final cached = await Session.readCache('services');
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = e.message;
        if (cached is Map) {
          _rehab = ((cached['rehab'] as Map?) ?? {}).cast<String, dynamic>();
          _meal = ((cached['meal'] as Map?) ?? {}).cast<String, dynamic>();
        }
      });
    }
  }

  Future<void> _addon() async {
    if (_orderId == null) return;
    final dishes = TextEditingController();
    final value = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('申请加餐'),
        content: TextField(
          controller: dishes,
          decoration: const InputDecoration(hintText: '如：红豆汤 + 全麦面包'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('取消')),
          FilledButton(onPressed: () => Navigator.pop(context, dishes.text.trim()), child: const Text('提交')),
        ],
      ),
    );
    if (value == null || value.isEmpty) return;
    try {
      await Api.instance.post('/api/open/customer/meal/addon', body: {
        'orderId': _orderId,
        'mealType': '加餐',
        'dishes': value,
        'charge': 0,
      }, auth: true);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('加餐申请已提交')));
      _load();
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('康复与月子餐'),
        bottom: TabBar(
          controller: _tabs,
          labelColor: Colors.white,
          unselectedLabelColor: Colors.white70,
          indicatorColor: Colors.white,
          tabs: const [Tab(text: '产后康复'), Tab(text: '月子餐')],
        ),
        actions: [IconButton(onPressed: _load, icon: const Icon(Icons.refresh))],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : !Session.logged
              ? _hint('登录后使用服务', '康复预约与月子餐仅对在住客户开放')
              : _orderId == null
                  ? _hint('暂未入馆', '入馆后可预约康复、查看每日月子餐')
                  : TabBarView(
                      controller: _tabs,
                      children: [_rehabView(), _mealView()],
                    ),
    );
  }

  Widget _rehabView() {
    final courses = (_rehab['courses'] as List?) ?? [];
    final appointments = (_rehab['appointments'] as List?) ?? [];
    final assesses = (_rehab['assesses'] as List?) ?? [];
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (_error != null)
            Text(_error!, style: const TextStyle(fontSize: 12, color: AppColors.danger)),
          const Text('我的疗程卡', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
          const SizedBox(height: 10),
          if (courses.isEmpty)
            _hint('套餐内未含康复项目', '可联系顾问增购疗程卡')
          else
            ...courses.map((row) {
              final map = (row as Map).cast<String, dynamic>();
              final course = (map['course'] as Map).cast<String, dynamic>();
              final remain = (map['remain'] as num?)?.toInt() ?? 0;
              final total = (course['totalTimes'] as num?)?.toInt() ?? 1;
              return Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(map['itemName']?.toString() ?? '',
                              style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
                          Text('$remain / $total 次',
                              style: const TextStyle(fontSize: 13, color: AppColors.brand700)),
                        ],
                      ),
                      const SizedBox(height: 8),
                      ClipRRect(
                        borderRadius: BorderRadius.circular(999),
                        child: LinearProgressIndicator(
                          value: total > 0 ? remain / total : 0,
                          minHeight: 6,
                          backgroundColor: AppColors.paper2,
                          color: AppColors.brand500,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text('到期 ${course['expireDate']} · ${course['source']}',
                          style: const TextStyle(fontSize: 11, color: AppColors.ink4)),
                    ],
                  ),
                ),
              );
            }),
          const SizedBox(height: 12),
          const Text('预约记录', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          if (appointments.isEmpty)
            _hint('暂无预约', '可在小程序/前台预约康复项目')
          else
            ...appointments.take(6).map((row) {
              final map = (row as Map).cast<String, dynamic>();
              return Card(
                child: ListTile(
                  title: Text('${map['appointDate']} ${map['startTime']}-${map['endTime']}',
                      style: const TextStyle(fontSize: 14)),
                  subtitle: Text(map['status']?.toString() ?? '',
                      style: const TextStyle(fontSize: 12, color: AppColors.ink3)),
                ),
              );
            }),
          const SizedBox(height: 12),
          const Text('康复评估', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          if (assesses.isEmpty)
            _hint('暂无评估', '入馆 42 天复查时会生成评估报告')
          else
            ...assesses.take(3).map((row) {
              final map = (row as Map).cast<String, dynamic>();
              return Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('盆底肌 ${map['pelvicScore']} 分 · 腹直肌 ${map['diastasisCm']} cm',
                          style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
                      const SizedBox(height: 4),
                      Text(map['report']?.toString() ?? '',
                          style: const TextStyle(fontSize: 12, color: AppColors.ink3)),
                    ],
                  ),
                ),
              );
            }),
        ],
      ),
    );
  }

  Widget _mealView() {
    final meals = (_meal['meals'] as List?) ?? [];
    final restricts = (_meal['restricts'] as List?) ?? [];
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('今日餐单', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
              Text(_meal['date']?.toString() ?? '',
                  style: const TextStyle(fontSize: 12, color: AppColors.ink4)),
            ],
          ),
          const SizedBox(height: 10),
          if (meals.isEmpty)
            _hint('今日餐单未生成', '营养师每日 20:00 排次日餐单')
          else
            ...meals.map((row) {
              final map = (row as Map).cast<String, dynamic>();
              return Card(
                child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Text(map['mealType']?.toString() ?? '',
                              style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: AppColors.brand700)),
                          const SizedBox(width: 8),
                          if (map['specialTag'] != null)
                            _tag(map['specialTag'].toString(), warn: true),
                          if (((map['extraCharge'] as num?) ?? 0) > 0)
                            _tag('加餐 ¥${map['extraCharge']}'),
                        ],
                      ),
                      const SizedBox(height: 6),
                      Text(map['dishes']?.toString() ?? '', style: const TextStyle(fontSize: 13)),
                    ],
                  ),
                ),
              );
            }),
          const SizedBox(height: 10),
          FilledButton.tonal(onPressed: _addon, child: const Text('申请加餐')),
          const SizedBox(height: 16),
          const Text('忌口与过敏原', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          if (restricts.isEmpty)
            _hint('尚未登记', '登记后厨房出餐自动规避')
          else
            Card(
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  children: [
                    for (final row in restricts)
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 4),
                        child: Row(
                          children: [
                            _tag((row as Map)['restrictType'].toString(),
                                warn: row['restrictType'] == '过敏'),
                            const SizedBox(width: 8),
                            Expanded(child: Text(row['content']?.toString() ?? '',
                                style: const TextStyle(fontSize: 13))),
                          ],
                        ),
                      ),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _tag(String text, {bool warn = false}) {
    return Container(
      margin: const EdgeInsets.only(right: 6),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: warn ? const Color(0xFFFDF5E8) : AppColors.brand100,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(text,
          style: TextStyle(fontSize: 11, color: warn ? AppColors.warn : AppColors.brand700)),
    );
  }

  Widget _hint(String title, String desc) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            Text(title, style: const TextStyle(fontSize: 14)),
            const SizedBox(height: 4),
            Text(desc, textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 12, color: AppColors.ink4)),
          ],
        ),
      ),
    );
  }
}
