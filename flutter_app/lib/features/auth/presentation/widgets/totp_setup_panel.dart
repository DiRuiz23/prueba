import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../../core/theme/app_theme.dart';

class TotpSetupPanel extends StatefulWidget {
  final String correo;
  final String secreto;
  final String qrBase64;

  const TotpSetupPanel({
    super.key,
    required this.correo,
    required this.secreto,
    required this.qrBase64,
  });

  @override
  State<TotpSetupPanel> createState() => _TotpSetupPanelState();
}

class _TotpSetupPanelState extends State<TotpSetupPanel> {
  Uint8List? _qrBytes;
  late bool _usarQr;

  @override
  void initState() {
    super.initState();
    _decodificarQr();
    _usarQr = _qrBytes != null;
  }

  @override
  void didUpdateWidget(covariant TotpSetupPanel oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.qrBase64 != widget.qrBase64) {
      _decodificarQr();
      if (_qrBytes == null && _usarQr) {
        _usarQr = false;
      } else if (_qrBytes != null && oldWidget.qrBase64.isEmpty) {
        _usarQr = true;
      }
    }
  }

  void _decodificarQr() {
    final raw = widget.qrBase64.trim();
    if (raw.isEmpty) {
      _qrBytes = null;
      return;
    }
    try {
      _qrBytes = base64Decode(raw);
    } catch (_) {
      _qrBytes = null;
    }
  }

  String _formatearSecreto(String s) {
    final buf = StringBuffer();
    for (var i = 0; i < s.length; i += 4) {
      if (i > 0) buf.write(' ');
      buf.write(s.substring(i, i + 4 > s.length ? s.length : i + 4));
    }
    return buf.toString();
  }

  @override
  Widget build(BuildContext context) {
    final qrBytes = _qrBytes;
    final mostrarQr = _usarQr && qrBytes != null;

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.line),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          SegmentedButton<bool>(
            showSelectedIcon: false,
            style: SegmentedButton.styleFrom(
              visualDensity: VisualDensity.compact,
              selectedBackgroundColor: AppColors.brandSoft,
              selectedForegroundColor: AppColors.brand,
            ),
            segments: [
              ButtonSegment<bool>(
                value: true,
                enabled: qrBytes != null,
                icon: const Icon(Icons.qr_code_2_rounded, size: 17),
                label: const Text('Código QR'),
              ),
              const ButtonSegment<bool>(
                value: false,
                icon: Icon(Icons.key_rounded, size: 17),
                label: Text('Clave manual'),
              ),
            ],
            selected: {mostrarQr},
            onSelectionChanged: (s) => setState(() => _usarQr = s.first),
          ),
          const SizedBox(height: 14),
          if (mostrarQr) ...[
            Center(
              child: Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: AppColors.line),
                ),
                child: Image.memory(
                  qrBytes,
                  width: 192,
                  height: 192,
                  fit: BoxFit.contain,
                ),
              ),
            ),
            const SizedBox(height: 10),
            const Text(
              'Abre Google Authenticator → + → Escanear un código QR.',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 12.5, color: AppColors.ink2),
            ),
          ] else ...[
            Text(
              '1. Abre Google Authenticator → + → Ingresar clave de configuración.\n'
              '2. Cuenta: AppHasani (${widget.correo})\n'
              '3. Copia y pega esta clave secreta:',
              style: const TextStyle(fontSize: 12.5, color: AppColors.ink2),
            ),
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
              decoration: BoxDecoration(
                color: AppColors.brandSoft,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: SelectableText(
                      _formatearSecreto(widget.secreto),
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 12.5,
                        color: AppColors.brand,
                      ),
                    ),
                  ),
                  IconButton(
                    tooltip: 'Copiar clave',
                    icon: const Icon(Icons.copy_rounded, size: 18, color: AppColors.brand),
                    onPressed: () {
                      Clipboard.setData(ClipboardData(text: widget.secreto));
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Clave secreta copiada')),
                      );
                    },
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}
