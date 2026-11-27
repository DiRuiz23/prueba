import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:apphasani_mobile/core/config/env_config.dart';
import 'package:apphasani_mobile/core/widgets/balance_card.dart';
import 'package:apphasani_mobile/core/widgets/product_avatar.dart';
import 'package:apphasani_mobile/core/widgets/virtual_bank_card.dart';
import 'package:apphasani_mobile/features/catalogo/domain/entities/producto_entity.dart';
import 'package:apphasani_mobile/features/compra/domain/entities/cuenta_entity.dart';

void main() {
  const cuenta = CuentaEntity(
    id: 1,
    numeroCuenta: '0129604495873',
    saldo: 1000.0,
    estatus: 'ACTIVA',
  );

  testWidgets('BalanceCard muestra saldo disponible, oculta importe y muestra terminación',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: BalanceCard(
            cuentas: [cuenta],
            cuentaActiva: cuenta,
          ),
        ),
      ),
    );

    expect(find.text('Saldo disponible'), findsOneWidget);
    expect(find.text('\$1,000.00'), findsOneWidget);
    expect(find.text('MXN'), findsOneWidget);
    expect(find.text('Cuenta •••• 5873'), findsOneWidget);

    await tester.tap(find.byIcon(Icons.visibility_outlined));
    await tester.pump();

    expect(find.text('••••••'), findsOneWidget);
  });

  testWidgets('VirtualBankCard muestra saldo, titular y oculta/muestra número de cuenta',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: VirtualBankCard(
            titular: 'diegoruizrivera5@gmail.com',
            cuentas: [cuenta],
            cuentaActiva: cuenta,
          ),
        ),
      ),
    );

    expect(find.text('\$1,000.00 MXN'), findsOneWidget);
    expect(find.text('diegoruizrivera5@gmail.com'), findsOneWidget);
    expect(find.text('•••• •••• 5873'), findsOneWidget);

    await tester.tap(find.byIcon(Icons.visibility));
    await tester.pump();

    expect(find.text('0129604495873'), findsOneWidget);
  });

  testWidgets('ProductAvatar usa URL de GestoPago por idServicio y muestra fallback si falla carga',
      (WidgetTester tester) async {
    const producto = ProductoEntity(
      producto: 'ABIB 100',
      servicio: 'ABIB',
      idServicio: 79,
      idProducto: 500,
      idCatTipoServicio: 1,
      tipoFront: 1,
      precio: '100',
    );

    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: ProductAvatar(producto: producto, size: 48, circular: true),
        ),
      ),
    );

    final image = tester.widget<Image>(find.byType(Image));
    final provider = image.image as NetworkImage;
    expect(
      provider.url,
      '${EnvConfig.baseUrl}/gestopago/servicios/79/imagen',
    );
    await tester.pumpAndSettle();
    expect(find.text('AB'), findsOneWidget);
  });
}
