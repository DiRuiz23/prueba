import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/error/exceptions.dart';
import '../../domain/repositories/catalogo_repository.dart';
import 'catalogo_state.dart';

class CatalogoCubit extends Cubit<CatalogoState> {
  final CatalogoRepository repository;

  CatalogoCubit({required this.repository}) : super(const CatalogoInitial());

  Future<void> cargarProductos({bool forzarRecarga = false}) async {
    emit(const CatalogoLoading());
    try {
      final lista = await repository.obtenerProductos(forzarRecarga: forzarRecarga);
      emit(CatalogoLoaded(todos: lista, filtrados: lista));
    } on ServerException catch (e) {
      emit(CatalogoError(e.message));
    } on NetworkTimeoutException catch (e) {
      emit(CatalogoError(e.message));
    } on NetworkConnectionException catch (e) {
      emit(CatalogoError(e.message));
    } on UnauthorizedException catch (e) {
      emit(CatalogoError(e.message));
    } catch (_) {
      emit(const CatalogoError('No fue posible cargar el catálogo.'));
    }
  }

  void aplicarFiltros({String? busqueda, CategoriaFiltro? categoria}) {
    final current = state;
    if (current is! CatalogoLoaded) return;

    final q = (busqueda ?? current.busqueda).trim().toLowerCase();
    final cat = categoria ?? current.categoria;

    final filtrados = current.todos.where((p) {
      final coincideTipo = cat == CategoriaFiltro.todos ||
          (cat == CategoriaFiltro.recargas && p.esRecarga) ||
          (cat == CategoriaFiltro.servicios && !p.esRecarga);

      final coincideTexto = q.isEmpty ||
          p.producto.toLowerCase().contains(q) ||
          p.servicio.toLowerCase().contains(q);

      return coincideTipo && coincideTexto;
    }).toList();

    emit(CatalogoLoaded(
      todos: current.todos,
      filtrados: filtrados,
      busqueda: busqueda ?? current.busqueda,
      categoria: cat,
    ));
  }
}

