import 'package:flutter/material.dart';

/// 设计 token：与后台、官网、小程序共用同一套暖白/墨绿/古铜配色。
class AppColors {
  static const brand900 = Color(0xFF1E5344);
  static const brand700 = Color(0xFF246A54);
  static const brand500 = Color(0xFF2F8B70);
  static const brand100 = Color(0xFFEEF7F2);
  static const bronze = Color(0xFFA87F3A);
  static const paper = Color(0xFFFBF9F5);
  static const paper2 = Color(0xFFF4F1E8);
  static const ink = Color(0xFF1F2421);
  static const ink2 = Color(0xFF414A45);
  static const ink3 = Color(0xFF6B746F);
  static const ink4 = Color(0xFF9AA39C);
  static const line = Color(0xFFE8E3D7);
  static const warn = Color(0xFFB4741F);
  static const danger = Color(0xFFB4543E);
}

class AppTheme {
  static ThemeData build() {
    final base = ThemeData(useMaterial3: true, brightness: Brightness.light);
    return base.copyWith(
      scaffoldBackgroundColor: AppColors.paper,
      colorScheme: base.colorScheme.copyWith(
        primary: AppColors.brand700,
        secondary: AppColors.bronze,
        surface: Colors.white,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.brand900,
        foregroundColor: Colors.white,
        elevation: 0,
        centerTitle: false,
      ),
      textTheme: base.textTheme.apply(
        bodyColor: AppColors.ink2,
        displayColor: AppColors.ink,
      ),
      cardTheme: CardThemeData(
        color: Colors.white,
        elevation: 0,
        margin: const EdgeInsets.only(bottom: 12),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: const BorderSide(color: AppColors.line),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: AppColors.paper,
        contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: AppColors.line),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: AppColors.line),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: AppColors.brand500),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: AppColors.brand700,
          foregroundColor: Colors.white,
          minimumSize: const Size.fromHeight(48),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
          textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
        ),
      ),
      chipTheme: base.chipTheme.copyWith(
        backgroundColor: Colors.white,
        side: const BorderSide(color: AppColors.line),
        labelStyle: const TextStyle(fontSize: 13, color: AppColors.ink3),
      ),
    );
  }
}
