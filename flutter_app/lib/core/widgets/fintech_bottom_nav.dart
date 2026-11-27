import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../theme/app_theme.dart';

class FintechBottomNav extends StatelessWidget {
  final int currentIndex;

  const FintechBottomNav({super.key, required this.currentIndex});

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: const BoxDecoration(
        border: Border(top: BorderSide(color: AppColors.line)),
      ),
      child: NavigationBar(
        selectedIndex: currentIndex,
        onDestinationSelected: (idx) {
          if (idx == currentIndex) return;
          context.go(idx == 0 ? '/catalogo' : '/historial');
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.grid_view_outlined),
            selectedIcon: Icon(Icons.grid_view_rounded, color: AppColors.brand),
            label: 'Servicios',
          ),
          NavigationDestination(
            icon: Icon(Icons.schedule_outlined),
            selectedIcon: Icon(Icons.schedule_rounded, color: AppColors.brand),
            label: 'Historial',
          ),
        ],
      ),
    );
  }
}
