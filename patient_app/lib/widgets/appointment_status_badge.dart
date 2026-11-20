import 'package:flutter/material.dart';

class AppointmentStatusBadge extends StatelessWidget {
  final String status;

  const AppointmentStatusBadge({super.key, required this.status});

  @override
  Widget build(BuildContext context) {
    final normalized = status.toUpperCase();
    late final String label;
    late final Color bg;
    late final Color fg;

    switch (normalized) {
      case 'WAITING':
        label = 'في الانتظار';
        bg = Colors.orange.shade50;
        fg = Colors.orange.shade800;
        break;
      case 'IN_PROGRESS':
        label = 'دورك الآن داخل العيادة';
        bg = Colors.blue.shade50;
        fg = Colors.blue.shade800;
        break;
      case 'COMPLETED':
        label = 'تم الكشف';
        bg = Colors.green.shade50;
        fg = Colors.green.shade800;
        break;
      case 'SKIPPED':
        label = 'تم التخطي - توجه للاستقبال';
        bg = Colors.amber.shade50;
        fg = Colors.amber.shade900;
        break;
      case 'CANCELLED':
        label = 'ملغي';
        bg = Colors.red.shade50;
        fg = Colors.red.shade700;
        break;
      case 'NO_SHOW':
        label = 'لم يحضر / منتهي (NO_SHOW)';
        bg = Colors.blueGrey.shade50;
        fg = Colors.blueGrey.shade700;
        break;
      default:
        label = status;
        bg = Colors.grey.shade100;
        fg = Colors.grey.shade700;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        label,
        style: TextStyle(color: fg, fontWeight: FontWeight.bold, fontSize: 12),
      ),
    );
  }
}