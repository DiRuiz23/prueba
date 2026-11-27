import 'dart:async';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/presentation/cubit/auth_cubit.dart';
import '../../features/auth/presentation/cubit/auth_state.dart';
import '../../features/auth/presentation/pages/login_page.dart';
import '../../features/auth/presentation/pages/otp_verification_page.dart';
import '../../features/auth/presentation/pages/registro_page.dart';
import '../../features/catalogo/domain/entities/producto_entity.dart';
import '../../features/catalogo/presentation/pages/catalogo_page.dart';
import '../../features/catalogo/presentation/pages/producto_detalle_page.dart';
import '../../features/compra/presentation/pages/compra_page.dart';
import '../../features/historial/presentation/pages/historial_page.dart';

class _GoRouterRefreshStream extends ChangeNotifier {
  late final StreamSubscription<dynamic> _subscription;

  _GoRouterRefreshStream(Stream<dynamic> stream) {
    _subscription = stream.asBroadcastStream().listen((_) {
      notifyListeners();
    });
  }

  @override
  void dispose() {
    _subscription.cancel();
    super.dispose();
  }
}

GoRouter createAppRouter(AuthCubit authCubit) {
  return GoRouter(
    initialLocation: '/login',
    refreshListenable: _GoRouterRefreshStream(authCubit.stream),
    redirect: (context, state) {
      final authState = authCubit.state;
      final isAuthenticated = authState is AuthAuthenticated;
      final location = state.matchedLocation;
      final isPublicRoute =
          location == '/login' || location == '/otp' || location == '/registro';

      if (!isAuthenticated && !isPublicRoute) {
        return '/login';
      }
      if (isAuthenticated && isPublicRoute) {
        return '/catalogo';
      }
      return null;
    },
    routes: [
      GoRoute(
        path: '/login',
        builder: (context, state) => const LoginPage(),
      ),
      GoRoute(
        path: '/registro',
        builder: (context, state) => const RegistroPage(),
      ),
      GoRoute(
        path: '/otp',
        builder: (context, state) {
          final extra = state.extra as Map<String, dynamic>? ?? {};
          return OtpVerificationPage(
            correo: (extra['correo'] as String?) ?? '',
            mensajeInicial: (extra['mensaje'] as String?) ??
                'Ingresa el código de 6 dígitos de tu app Google Authenticator.',
            requiereConfiguracionTotp:
                (extra['requiereConfiguracionTotp'] as bool?) ?? false,
            totpSecreto: extra['totpSecreto'] as String?,
            totpOtpAuthUri: extra['totpOtpAuthUri'] as String?,
            totpQrBase64: extra['totpQrBase64'] as String?,
          );
        },
      ),
      GoRoute(
        path: '/catalogo',
        builder: (context, state) => const CatalogoPage(),
      ),
      GoRoute(
        path: '/producto-detalle',
        builder: (context, state) {
          final producto = state.extra as ProductoEntity;
          return ProductoDetallePage(producto: producto);
        },
      ),
      GoRoute(
        path: '/compra',
        builder: (context, state) {
          final producto = state.extra as ProductoEntity;
          return CompraPage(producto: producto);
        },
      ),
      GoRoute(
        path: '/historial',
        builder: (context, state) => const HistorialPage(),
      ),
    ],
  );
}

