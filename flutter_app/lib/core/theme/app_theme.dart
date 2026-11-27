import 'package:flutter/material.dart';

class AppColors {
  AppColors._();

  static const bg = Color(0xFFF6F8F7);
  static const surface = Colors.white;
  static const ink = Color(0xFF0F172A);
  static const ink2 = Color(0xFF475569);
  static const ink3 = Color(0xFF94A3B8);
  static const line = Color(0xFFE6E8EC);
  static const lineStrong = Color(0xFFD3D7DE);
  static const brand = Color(0xFF0B7A5A);
  static const brandLight = Color(0xFF10B981);
  static const brandAura = Color(0xFFC6EFE0);
  static const brandSoft = Color(0xFFE8F5EF);
  static const danger = Color(0xFFC0392B);
  static const dangerSoft = Color(0xFFFDECEA);
  static const warn = Color(0xFFB45309);
  static const warnSoft = Color(0xFFFEF3C7);
}

class AppTheme {
  AppTheme._();

  static InputDecoration pillInputDecoration({
    required String hintText,
    String? labelText,
    IconData? prefixIcon,
    Widget? prefix,
    Widget? suffixIcon,
    String? errorText,
    String? helperText,
    String? counterText,
    bool enabled = true,
  }) {
    final border = OutlineInputBorder(
      borderRadius: BorderRadius.circular(28),
      borderSide: const BorderSide(color: AppColors.lineStrong),
    );
    return InputDecoration(
      labelText: labelText,
      hintText: hintText,
      hintStyle: const TextStyle(color: AppColors.ink3, fontSize: 14.5),
      prefixIcon: prefixIcon != null
          ? Icon(prefixIcon, size: 20, color: AppColors.ink3)
          : null,
      prefix: prefix,
      suffixIcon: suffixIcon,
      errorText: errorText,
      errorMaxLines: 2,
      helperText: helperText,
      helperMaxLines: 2,
      counterText: counterText,
      filled: true,
      fillColor: enabled ? AppColors.surface : const Color(0xFFF1F5F9),
      border: border,
      enabledBorder: border,
      disabledBorder: border.copyWith(
        borderSide: const BorderSide(color: AppColors.line),
      ),
      focusedBorder: border.copyWith(
        borderSide: const BorderSide(color: AppColors.brand, width: 1.6),
      ),
      errorBorder: border.copyWith(
        borderSide: const BorderSide(color: AppColors.danger),
      ),
      focusedErrorBorder: border.copyWith(
        borderSide: const BorderSide(color: AppColors.danger, width: 1.6),
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
    );
  }

  static ThemeData get light {
    final border = OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: const BorderSide(color: AppColors.lineStrong),
    );
    return ThemeData(
      useMaterial3: true,
      scaffoldBackgroundColor: AppColors.bg,
      colorScheme: ColorScheme.fromSeed(
        seedColor: AppColors.brand,
        primary: AppColors.brand,
        surface: AppColors.surface,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.surface,
        foregroundColor: AppColors.ink,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        titleTextStyle: TextStyle(
          color: AppColors.ink,
          fontSize: 20,
          fontWeight: FontWeight.w700,
        ),
        shape: Border(bottom: BorderSide(color: AppColors.line)),
      ),
      cardTheme: CardThemeData(
        color: AppColors.surface,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: const BorderSide(color: AppColors.line),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: AppColors.surface,
        border: border,
        enabledBorder: border,
        focusedBorder: border.copyWith(
          borderSide: const BorderSide(color: AppColors.brand, width: 1.6),
        ),
        errorBorder: border.copyWith(
          borderSide: const BorderSide(color: AppColors.danger),
        ),
        focusedErrorBorder: border.copyWith(
          borderSide: const BorderSide(color: AppColors.danger, width: 1.6),
        ),
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 15),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: AppColors.brand,
          foregroundColor: Colors.white,
          minimumSize: const Size.fromHeight(50),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
          textStyle: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: AppColors.ink,
          side: const BorderSide(color: AppColors.lineStrong),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(999)),
          textStyle: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14.5),
        ),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: AppColors.surface,
        indicatorColor: AppColors.brandSoft,
        elevation: 0,
        labelTextStyle: WidgetStateProperty.all(
          const TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
        ),
      ),
      dividerTheme: const DividerThemeData(color: AppColors.line, space: 1),
    );
  }
}
