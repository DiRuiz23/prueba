import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:get_it/get_it.dart';

import '../../features/auth/data/datasources/auth_remote_datasource.dart';
import '../../features/auth/data/repositories/auth_repository_impl.dart';
import '../../features/auth/domain/repositories/auth_repository.dart';
import '../../features/auth/domain/usecases/auth_usecases.dart';
import '../../features/auth/presentation/cubit/auth_cubit.dart';
import '../../features/catalogo/data/datasources/catalogo_remote_datasource.dart';
import '../../features/catalogo/data/repositories/catalogo_repository_impl.dart';
import '../../features/catalogo/domain/repositories/catalogo_repository.dart';
import '../../features/catalogo/presentation/cubit/catalogo_cubit.dart';
import '../../features/compra/data/datasources/compra_remote_datasource.dart';
import '../../features/compra/data/repositories/compra_repository_impl.dart';
import '../../features/compra/domain/repositories/compra_repository.dart';
import '../../features/compra/presentation/cubit/compra_cubit.dart';
import '../../features/historial/data/datasources/historial_remote_datasource.dart';
import '../../features/historial/data/repositories/historial_repository_impl.dart';
import '../../features/historial/domain/repositories/historial_repository.dart';
import '../../features/historial/presentation/cubit/historial_cubit.dart';
import '../network/auth_interceptor.dart';
import '../network/dio_client.dart';

final GetIt sl = GetIt.instance;

Future<void> initDependencies() async {
  // Core
  sl.registerLazySingleton<FlutterSecureStorage>(
    () => const FlutterSecureStorage(
      aOptions: AndroidOptions(encryptedSharedPreferences: true),
    ),
  );
  sl.registerLazySingleton<AuthInterceptor>(
    () => AuthInterceptor(secureStorage: sl()),
  );
  sl.registerLazySingleton<DioClient>(
    () => DioClient(authInterceptor: sl()),
  );

  // Auth
  sl.registerLazySingleton<AuthRemoteDataSource>(
    () => AuthRemoteDataSourceImpl(dioClient: sl()),
  );
  sl.registerLazySingleton<AuthRepository>(
    () => AuthRepositoryImpl(remoteDataSource: sl(), secureStorage: sl()),
  );
  sl.registerLazySingleton(() => LoginUseCase(sl()));
  sl.registerLazySingleton(() => VerificarOtpUseCase(sl()));
  sl.registerLazySingleton(() => LogoutUseCase(sl()));
  sl.registerFactory<AuthCubit>(
    () => AuthCubit(
      loginUseCase: sl(),
      verificarOtpUseCase: sl(),
      logoutUseCase: sl(),
      authRepository: sl(),
      sessionExpiredStream: sl<AuthInterceptor>().onSessionExpired,
    ),
  );

  // Catálogo
  sl.registerLazySingleton<CatalogoRemoteDataSource>(
    () => CatalogoRemoteDataSourceImpl(dioClient: sl()),
  );
  sl.registerLazySingleton<CatalogoRepository>(
    () => CatalogoRepositoryImpl(remoteDataSource: sl()),
  );
  sl.registerFactory<CatalogoCubit>(
    () => CatalogoCubit(repository: sl()),
  );

  // Compra
  sl.registerLazySingleton<CompraRemoteDataSource>(
    () => CompraRemoteDataSourceImpl(dioClient: sl()),
  );
  sl.registerLazySingleton<CompraRepository>(
    () => CompraRepositoryImpl(remoteDataSource: sl()),
  );
  sl.registerFactory<CompraCubit>(
    () => CompraCubit(repository: sl()),
  );

  // Historial
  sl.registerLazySingleton<HistorialRemoteDataSource>(
    () => HistorialRemoteDataSourceImpl(dioClient: sl()),
  );
  sl.registerLazySingleton<HistorialRepository>(
    () => HistorialRepositoryImpl(remoteDataSource: sl()),
  );
  sl.registerFactory<HistorialCubit>(
    () => HistorialCubit(repository: sl()),
  );
}

