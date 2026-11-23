import 'package:flutter/material.dart';
import '../../features/catalogo/domain/entities/producto_entity.dart';

class ProductAvatar extends StatelessWidget {
  final ProductoEntity producto;
  final double size;

  const ProductAvatar({super.key, required this.producto, this.size = 42});

  (String, Color, Color) _estilo() {
    final t = '${producto.servicio} ${producto.producto}'.toLowerCase();
    if (t.contains('telcel')) return ('TC', const Color(0xFFE8EFFF), const Color(0xFF1D4ED8));
    if (t.contains('at&t') || t.contains('att')) return ('AT', const Color(0xFFE0F2FE), const Color(0xFF0369A1));
    if (t.contains('movistar')) return ('MV', const Color(0xFFE6F6EC), const Color(0xFF15803D));
    if (t.contains('cfe')) return ('CFE', const Color(0xFFE8F5EF), const Color(0xFF0B7A5A));
    if (t.contains('totalplay')) return ('TP', const Color(0xFFF1E9FB), const Color(0xFF6B21A8));
    if (t.contains('telmex') || t.contains('izzi')) return ('TX', const Color(0xFFFDECEA), const Color(0xFFB42318));
    if (t.contains('netflix')) return ('NF', const Color(0xFFFDECEA), const Color(0xFFB91C1C));
    if (t.contains('spotify')) return ('SP', const Color(0xFFE6F6EC), const Color(0xFF166534));
    final s = producto.servicio.isNotEmpty ? producto.servicio : producto.producto;
    final sig = s.length >= 2 ? s.substring(0, 2).toUpperCase() : 'GP';
    return (sig, const Color(0xFFEEF0F4), const Color(0xFF475569));
  }

  @override
  Widget build(BuildContext context) {
    final (siglas, bg, fg) = _estilo();
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(10)),
      alignment: Alignment.center,
      child: Text(
        siglas,
        style: TextStyle(fontWeight: FontWeight.w700, color: fg, fontSize: size * 0.33),
      ),
    );
  }
}
