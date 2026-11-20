import 'package:flutter/material.dart';
import '../services/api_service.dart';

class ActiveAppointmentCard extends StatelessWidget {
  final Map<String, dynamic>? appointment;
  final ApiService? apiService;
  final VoidCallback? onRefresh;
  final VoidCallback? onCancelled;

  const ActiveAppointmentCard({
    super.key,
    required this.appointment,
    this.apiService,
    this.onRefresh,
    this.onCancelled,
  });

  Future<void> _confirmAndCancel(BuildContext context, int appointmentId) async {
    final service = apiService ?? ApiService();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => Directionality(
        textDirection: TextDirection.rtl,
        child: AlertDialog(
          title: const Text('تأكيد إلغاء الحجز'),
          content: const Text('هل أنت متأكد من رغبتك في إلغاء حجزك اليوم؟'),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('تراجع'),
            ),
            ElevatedButton(
              onPressed: () => Navigator.pop(ctx, true),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.red,
                foregroundColor: Colors.white,
              ),
              child: const Text('تأكيد الإلغاء'),
            ),
          ],
        ),
      ),
    );

    if (confirmed != true || !context.mounted) return;

    try {
      await service.cancelAppointment(appointmentId);
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('تم إلغاء الحجز بنجاح'),
          backgroundColor: Colors.green,
        ),
      );
      onCancelled?.call();
      onRefresh?.call();
    } on ApiException catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.message), backgroundColor: Colors.red),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    if (appointment == null) {
      return const SizedBox.shrink();
    }

    final status = (appointment!['status'] ?? '').toString().toUpperCase();
    final int? appointmentId = appointment!['appointmentId'] is int
        ? appointment!['appointmentId'] as int
        : int.tryParse('${appointment!['appointmentId']}');
    final queueNumber = appointment!['queueNumber'] ?? 0;
    final clinicName = (appointment!['clinicName'] ?? '').toString();
    final closureReason = (appointment!['closureReason'] ?? '').toString().trim();
    final visitType = (appointment!['visitType'] ?? 'EXAMINATION').toString();
    final fee = appointment!['fee'];
    final visitLabel = visitType == 'CONSULTATION' ? 'استشارة' : 'كشف جديد';

    if (status == 'CANCELLED') {
      return Card(
        color: Colors.red.shade50,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(color: Colors.red.shade200),
        ),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(Icons.cancel_rounded, color: Colors.red.shade700, size: 28),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'تم إلغاء الحجز${clinicName.isNotEmpty ? ' - $clinicName' : ''}',
                      style: TextStyle(
                        color: Colors.red.shade800,
                        fontWeight: FontWeight.bold,
                        fontSize: 16,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                closureReason.isNotEmpty
                    ? 'السبب: $closureReason'
                    : 'تم إلغاء الحجز اليوم. يمكنك الحجز في موعد آخر.',
                style: TextStyle(color: Colors.red.shade900, fontSize: 14),
              ),
            ],
          ),
        ),
      );
    }

    if (status == 'SKIPPED') {
      return Card(
        color: Colors.orange.shade50,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(color: Colors.orange.shade400, width: 1.5),
        ),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(Icons.warning_amber_rounded, color: Colors.orange.shade800, size: 30),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'تم تخطي دورك (رقم $queueNumber) لعدم التواجد',
                      style: TextStyle(
                        color: Colors.orange.shade900,
                        fontWeight: FontWeight.bold,
                        fontSize: 16,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                'يرجى التوجه إلى مكتب الاستقبال فور وصولك للعيادة لإعادة إدراجك في الطابور.',
                style: TextStyle(color: Colors.orange.shade900, fontSize: 14, height: 1.4),
              ),
              if (appointmentId != null) ...[
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: OutlinedButton.icon(
                    onPressed: () => _confirmAndCancel(context, appointmentId),
                    icon: const Icon(Icons.close, color: Colors.red),
                    label: const Text('إلغاء الحجز', style: TextStyle(color: Colors.red, fontWeight: FontWeight.bold)),
                    style: OutlinedButton.styleFrom(side: const BorderSide(color: Colors.red)),
                  ),
                ),
              ],
            ],
          ),
        ),
      );
    }

    String statusLabel;
    Color accentColor;
    switch (status) {
      case 'IN_PROGRESS':
        statusLabel = 'دورك الآن داخل غرفة الكشف';
        accentColor = Colors.green;
        break;
      case 'COMPLETED':
        statusLabel = 'تم الكشف بنجاح';
        accentColor = Colors.teal;
        break;
      default:
        statusLabel = 'في انتظار دورك';
        accentColor = Colors.indigo;
    }

    final bool canCancel = status == 'WAITING' && appointmentId != null;

    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Row(
              children: [
                CircleAvatar(
                  radius: 28,
                  backgroundColor: accentColor.withValues(alpha: 0.12),
                  child: Text(
                    '$queueNumber',
                    style: TextStyle(
                      color: accentColor,
                      fontSize: 22,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if (clinicName.isNotEmpty)
                        Text(clinicName, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                      const SizedBox(height: 4),
                      Text(
                        statusLabel,
                        style: TextStyle(color: accentColor, fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        fee != null ? '$visitLabel • $fee ج' : visitLabel,
                        style: const TextStyle(color: Colors.blueGrey, fontSize: 12, fontWeight: FontWeight.w600),
                      ),
                    ],
                  ),
                ),
                if (onRefresh != null)
                  IconButton(
                    onPressed: onRefresh,
                    icon: const Icon(Icons.refresh),
                    tooltip: 'تحديث الحالة',
                  ),
              ],
            ),
            if (canCancel) ...[
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: () => _confirmAndCancel(context, appointmentId),
                  icon: const Icon(Icons.cancel_outlined, color: Colors.red),
                  label: const Text('إلغاء الحجز', style: TextStyle(color: Colors.red, fontWeight: FontWeight.bold)),
                  style: OutlinedButton.styleFrom(
                    side: BorderSide(color: Colors.red.shade300),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  static Future<void> handleBookAction({
    required BuildContext context,
    required ApiService apiService,
    required int clinicId,
    required int doctorId,
    String visitType = 'EXAMINATION',
    VoidCallback? onBooked,
  }) async {
    try {
      final result = await apiService.bookAppointment(
        clinicId: clinicId,
        doctorId: doctorId,
        visitType: visitType,
      );
      if (!context.mounted) return;
      final qNum = result['queueNumber'] ?? 0;
      await showDialog<void>(
        context: context,
        builder: (ctx) => Directionality(
          textDirection: TextDirection.rtl,
          child: AlertDialog(
            title: const Text('تم تأكيد الحجز'),
            content: Text('تم حجز دورك بنجاح. رقمك في الطابور هو: $qNum'),
            actions: [
              TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('حسناً')),
            ],
          ),
        ),
      );
      onBooked?.call();
    } on ApiException catch (e) {
      if (!context.mounted) return;
      final isDuplicateOrBlocked = e.statusCode == 409 || e.statusCode == 403;
      await showDialog<void>(
        context: context,
        builder: (ctx) => Directionality(
          textDirection: TextDirection.rtl,
          child: AlertDialog(
            title: Text(isDuplicateOrBlocked ? 'تنبيه الحجز' : 'تعذر إتمام الحجز'),
            content: Text(e.message),
            actions: [
              TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('موافق')),
            ],
          ),
        ),
      );
      if (e.statusCode == 409) {
        onBooked?.call();
      }
    }
  }
}