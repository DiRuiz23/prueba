import 'package:flutter/services.dart';

class UpperCaseInputFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    return newValue.copyWith(text: newValue.text.toUpperCase());
  }
}

class DecimalInputFormatter extends TextInputFormatter {
  final int maxEnteros;
  final int maxDecimales;

  const DecimalInputFormatter({this.maxEnteros = 10, this.maxDecimales = 2});

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final text = newValue.text;
    if (text.isEmpty) return newValue;
    final regex = RegExp('^\\d{0,$maxEnteros}(\\.\\d{0,$maxDecimales})?\$');
    return regex.hasMatch(text) ? newValue : oldValue;
  }
}

class AppInputFormatters {
  AppInputFormatters._();

  static List<TextInputFormatter> soloLetras(int max) => [
        FilteringTextInputFormatter.allow(
          RegExp(r'[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ\s]'),
        ),
        LengthLimitingTextInputFormatter(max),
      ];

  static List<TextInputFormatter> soloDigitos(int max) => [
        FilteringTextInputFormatter.digitsOnly,
        LengthLimitingTextInputFormatter(max),
      ];

  static List<TextInputFormatter> decimal({
    int maxEnteros = 10,
    int maxDecimales = 2,
  }) =>
      [
        DecimalInputFormatter(
          maxEnteros: maxEnteros,
          maxDecimales: maxDecimales,
        ),
      ];

  static final List<TextInputFormatter> curp = [
    FilteringTextInputFormatter.allow(RegExp(r'[A-Za-z0-9]')),
    UpperCaseInputFormatter(),
    LengthLimitingTextInputFormatter(18),
  ];

  static final List<TextInputFormatter> rfc = [
    FilteringTextInputFormatter.allow(RegExp(r'[A-Za-zÑñ&0-9]')),
    UpperCaseInputFormatter(),
    LengthLimitingTextInputFormatter(13),
  ];

  static final List<TextInputFormatter> correo = [
    FilteringTextInputFormatter.deny(RegExp(r'\s')),
    LengthLimitingTextInputFormatter(100),
  ];

  static final List<TextInputFormatter> referenciaServicio = [
    FilteringTextInputFormatter.allow(RegExp(r'[A-Za-z0-9\-_]')),
    LengthLimitingTextInputFormatter(30),
  ];

  static List<TextInputFormatter> textoLimpio(int max) => [
        FilteringTextInputFormatter.allow(
          RegExp(r'[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ0-9\s.,#/\-&]'),
        ),
        LengthLimitingTextInputFormatter(max),
      ];

  static final List<TextInputFormatter> numeroDomicilio = [
    FilteringTextInputFormatter.allow(RegExp(r'[A-Za-z0-9/\-]')),
    LengthLimitingTextInputFormatter(6),
  ];

  static final List<TextInputFormatter> password = [
    FilteringTextInputFormatter.deny(RegExp(r'\s')),
    LengthLimitingTextInputFormatter(64),
  ];
}
