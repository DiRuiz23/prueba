import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import 'core/di/injection_container.dart';
import 'core/router/app_router.dart';
import 'core/theme/app_theme.dart';
import 'features/auth/presentation/cubit/auth_cubit.dart';
import 'features/catalogo/presentation/cubit/catalogo_cubit.dart';
import 'features/compra/presentation/cubit/compra_cubit.dart';
import 'features/historial/presentation/cubit/historial_cubit.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initDependencies();

  final authCubit = sl<AuthCubit>()..verificarSesionInicial();
  final router = createAppRouter(authCubit);

  runApp(AppHasaniApp(authCubit: authCubit, router: router));
}

class AppHasaniApp extends StatelessWidget {
  final AuthCubit authCubit;
  final GoRouter router;

  const AppHasaniApp({super.key, required this.authCubit, required this.router});

  @override
  Widget build(BuildContext context) {
    return MultiBlocProvider(
      providers: [
        BlocProvider<AuthCubit>.value(value: authCubit),
        BlocProvider<CatalogoCubit>(create: (_) => sl<CatalogoCubit>()),
        BlocProvider<CompraCubit>(create: (_) => sl<CompraCubit>()),
        BlocProvider<HistorialCubit>(create: (_) => sl<HistorialCubit>()),
      ],
      child: MaterialApp.router(
        title: 'AppHasani',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.light,
        routerConfig: router,
      ),
    );
  }
}
