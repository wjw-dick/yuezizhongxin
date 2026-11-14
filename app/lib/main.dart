import 'package:flutter/material.dart';

import 'core/session.dart';
import 'core/theme.dart';
import 'features/home/home_page.dart';
import 'features/login/login_page.dart';
import 'features/orders/orders_page.dart';
import 'features/profile/profile_page.dart';
import 'features/services/services_page.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Session.load();
  runApp(const YzzApp());
}

class YzzApp extends StatelessWidget {
  const YzzApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '月之孕月子中心',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.build(),
      home: Session.logged ? const RootShell() : const LoginPage(),
      routes: {
        '/login': (_) => const LoginPage(),
        '/shell': (_) => const RootShell(),
      },
    );
  }
}

class RootShell extends StatefulWidget {
  const RootShell({super.key});

  @override
  State<RootShell> createState() => _RootShellState();
}

class _RootShellState extends State<RootShell> {
  int _index = 0;

  static const _pages = <Widget>[
    HomePage(),
    OrdersPage(),
    ServicesPage(),
    ProfilePage(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(index: _index, children: _pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (value) => setState(() => _index = value),
        backgroundColor: Colors.white,
        indicatorColor: AppColors.brand100,
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: '首页'),
          NavigationDestination(icon: Icon(Icons.receipt_long_outlined), selectedIcon: Icon(Icons.receipt_long), label: '订单'),
          NavigationDestination(icon: Icon(Icons.spa_outlined), selectedIcon: Icon(Icons.spa), label: '服务'),
          NavigationDestination(icon: Icon(Icons.person_outline), selectedIcon: Icon(Icons.person), label: '我的'),
        ],
      ),
    );
  }
}
