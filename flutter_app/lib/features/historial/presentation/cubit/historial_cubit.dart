import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/error/exceptions.dart';
import '../../../compra/domain/entities/transaccion_entity.dart';
import '../../domain/repositories/historial_repository.dart';
import 'historial_state.dart';

class HistorialCubit extends Cubit<HistorialState> {
  final HistorialRepository repository;
  final List<TransaccionEntity> _ticketsLocales = [];

  HistorialCubit({required this.repository}) : super(const HistorialInitial());

  void registrarTicketLocal(TransaccionEntity ticket) {
    _ticketsLocales.removeWhere(
      (t) => t.upc == ticket.upc && t.fechaTransaccion == ticket.fechaTransaccion,
    );
    _ticketsLocales.insert(0, ticket);
    if (_ticketsLocales.length > 30) {
      _ticketsLocales.removeLast();
    }
    final current = state;
    if (current is HistorialLoaded) {
      final actualizados = [
        ticket,
        ...current.items.where(
          (t) => !(t.upc == ticket.upc && t.fechaTransaccion == ticket.fechaTransaccion),
        ),
      ];
      emit(HistorialLoaded(
        items: actualizados,
        paginaActual: current.paginaActual,
        esUltima: current.esUltima,
        cargandoMas: current.cargandoMas,
      ));
    }
  }

  void limpiarTicketsLocales() {
    _ticketsLocales.clear();
  }

  Future<void> cargarInicial() async {
    emit(const HistorialLoading());
    try {
      final page = await repository.obtenerHistorialExitoso(pagina: 0, tamanio: 10);
      final combinados = [
        ..._ticketsLocales.where(
          (loc) => !page.items.any((r) => r.upc == loc.upc && r.idTransaccion > 0),
        ),
        ...page.items,
      ];
      emit(HistorialLoaded(
        items: combinados,
        paginaActual: page.paginaActual,
        esUltima: page.esUltima,
      ));
    } on ServerException catch (e) {
      emit(HistorialError(e.message));
    } on NetworkTimeoutException catch (e) {
      emit(HistorialError(e.message));
    } on NetworkConnectionException catch (e) {
      emit(HistorialError(e.message));
    } on UnauthorizedException catch (e) {
      emit(HistorialError(e.message));
    } catch (_) {
      emit(const HistorialError('No fue posible cargar el historial de compras.'));
    }
  }

  Future<void> cargarSiguientePagina() async {
    final current = state;
    if (current is! HistorialLoaded || current.esUltima || current.cargandoMas) {
      return;
    }

    emit(HistorialLoaded(
      items: current.items,
      paginaActual: current.paginaActual,
      esUltima: current.esUltima,
      cargandoMas: true,
    ));

    try {
      final nextPage = await repository.obtenerHistorialExitoso(
        pagina: current.paginaActual + 1,
        tamanio: 10,
      );
      final combinados = List<TransaccionEntity>.from(current.items)..addAll(nextPage.items);
      emit(HistorialLoaded(
        items: combinados,
        paginaActual: nextPage.paginaActual,
        esUltima: nextPage.esUltima,
      ));
    } catch (_) {
      emit(HistorialLoaded(
        items: current.items,
        paginaActual: current.paginaActual,
        esUltima: current.esUltima,
      ));
    }
  }
}

