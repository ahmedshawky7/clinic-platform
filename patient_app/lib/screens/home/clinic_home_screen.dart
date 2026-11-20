import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import '../../config/api_config.dart';

class ClinicHomeScreen extends StatefulWidget {
  final String token;
  final int userId;
  final int clinicId;
  final int doctorId;
  final String doctorName;
  final String orgName;
  final VoidCallback onBookClick;
  final void Function(int newClinicId, String newClinicName) onSwitchBranch;

  const ClinicHomeScreen({
    super.key,
    required this.token,
    required this.userId,
    required this.clinicId,
    required this.doctorId,
    required this.doctorName,
    required this.orgName,
    required this.onBookClick,
    required this.onSwitchBranch,
  });

  @override
  State<ClinicHomeScreen> createState() => _ClinicHomeScreenState();
}

class _ClinicHomeScreenState extends State<ClinicHomeScreen> {
  Map<String, dynamic>? clinicData;
  bool _isLoading = true;
  List<Map<String, dynamic>> otherWorkingBranches = [];

  String get _todayDayName {
    switch (DateTime.now().weekday) {
      case 1: return 'MONDAY';
      case 2: return 'TUESDAY';
      case 3: return 'WEDNESDAY';
      case 4: return 'THURSDAY';
      case 5: return 'FRIDAY';
      case 6: return 'SATURDAY';
      case 7: return 'SUNDAY';
      default: return '';
    }
  }

  String _arabicDayName(String key) {
    return {
      'SATURDAY': 'السبت',
      'SUNDAY': 'الأحد',
      'MONDAY': 'الاثنين',
      'TUESDAY': 'الثلاثاء',
      'WEDNESDAY': 'الأربعاء',
      'THURSDAY': 'الخميس',
      'FRIDAY': 'الجمعة',
    }[key] ?? key;
  }

  @override
  void initState() {
    super.initState();
    _loadAll();
  }

  Future<void> _loadAll() async {
    setState(() => _isLoading = true);
    try {
      final res = await http.get(
        Uri.parse('${ApiConfig.baseUrl}/clinics/${widget.clinicId}'),
        headers: {'Authorization': 'Bearer ${widget.token}'},
      );
      if (res.statusCode == 200 && mounted) {
        final data = jsonDecode(utf8.decode(res.bodyBytes));
        setState(() => clinicData = data);

        final orgId = data['organizationId'];
        final orgsRes = await http.get(Uri.parse('${ApiConfig.baseUrl}/public/organizations'));
        if (orgsRes.statusCode == 200 && mounted) {
          final List<dynamic> orgs = jsonDecode(utf8.decode(orgsRes.bodyBytes));
          final myOrg = orgs.where((o) => o['id'] == orgId).firstOrNull;
          if (myOrg != null) {
            final branches = (myOrg['clinics'] as List<dynamic>? ?? []);
            setState(() {
              otherWorkingBranches = branches
                  .where((c) =>
                      c['id'] != widget.clinicId &&
                      (c['openToday'] ?? true) == true &&
                      (c['workingDays'] ?? '').split(',').contains(_todayDayName))
                  .map((c) => Map<String, dynamic>.from(c))
                  .toList();
            });
          }
        }
      }
    } catch (e) {
      debugPrint('Error loading clinic info: $e');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  String _formatTime(String time24) {
    if (time24.isEmpty) return '';
    final parts = time24.split(':');
    if (parts.length < 2) return time24;
    int hour = int.tryParse(parts[0]) ?? 0;
    final minute = parts[1].padLeft(2, '0');
    final period = hour >= 12 ? 'مساءً' : 'صباحاً';
    if (hour > 12) hour -= 12;
    if (hour == 0) hour = 12;
    return '${hour.toString().padLeft(2, '0')}:$minute $period';
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    final bool isOpen = clinicData?['openToday'] ?? true;
    final String workingDaysStr = clinicData?['workingDays'] ?? '';
    final List<String> workingDaysList = workingDaysStr.split(',').where((e) => e.isNotEmpty).toList();
    final bool isWorkingDay = workingDaysList.isEmpty || workingDaysList.contains(_todayDayName);
    final bool withinBookingHours = clinicData?['withinBookingHours'] ?? true;
    final bool canBook = (clinicData?['canBookNow'] ?? (isOpen && isWorkingDay && withinBookingHours)) as bool;
    final String statusMessage = (clinicData?['bookingStatusMessage'] ?? '').toString();

    final String bookingOpenTime = clinicData?['bookingOpenTime'] ?? '00:00';
    final String bookingCloseTime = clinicData?['bookingCloseTime'] ?? '23:59';
    final String doctorArrivalTime = clinicData?['doctorArrivalTime'] ?? '17:00';

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.orgName.isNotEmpty ? widget.orgName : 'تفاصيل الفرع'),
        backgroundColor: const Color(0xFF0B8F86),
        foregroundColor: Colors.white,
        centerTitle: true,
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadAll),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadAll,
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(22),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(20),
                  boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10)],
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      clinicData?['name'] ?? 'الفرع',
                      style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Color(0xFF0B8F86)),
                    ),
                    if (widget.doctorName.isNotEmpty) ...[
                      const SizedBox(height: 6),
                      Text('الطبيب المختار: ${widget.doctorName}', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                    ],
                    if ((clinicData?['address'] ?? '').toString().isNotEmpty) ...[
                      const SizedBox(height: 6),
                      Text('📍 ${clinicData?['city'] ?? ''} - ${clinicData?['address']}', style: const TextStyle(color: Colors.grey)),
                    ],
                    const Divider(height: 28),
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: const Color(0xFFD9F2EE),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Column(
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              const Text('استقبال الحجوزات:', style: TextStyle(fontWeight: FontWeight.bold)),
                              Text(
                                '${_formatTime(bookingOpenTime)} - ${_formatTime(bookingCloseTime)}',
                                style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF075B59)),
                              ),
                            ],
                          ),
                          const Divider(height: 16),
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              const Text('موعد حضور الطبيب:', style: TextStyle(fontWeight: FontWeight.bold)),
                              Text(
                                'يبدأ الكشف الساعة ${_formatTime(doctorArrivalTime)}',
                                style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.indigo),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),
                    if (!isOpen)
                      _statusBanner(Colors.red, Icons.cancel, 'الفرع مغلق اليوم', clinicData?['closureReason'] ?? '')
                    else if (!isWorkingDay) ...[
                      _statusBanner(
                        Colors.orange,
                        Icons.event_busy,
                        'الفرع لا يعمل اليوم (${_arabicDayName(_todayDayName)})',
                        'أيام العمل: ${workingDaysList.map(_arabicDayName).join(' - ')}',
                      ),
                      if (otherWorkingBranches.isNotEmpty) ...[
                        const SizedBox(height: 12),
                        const Text('فروع أخرى مفتوحة اليوم:', style: TextStyle(fontWeight: FontWeight.bold, color: Colors.green)),
                        ...otherWorkingBranches.map((b) => Card(
                              child: ListTile(
                                title: Text(b['name']),
                                subtitle: Text(b['address'] ?? ''),
                                trailing: const Icon(Icons.swap_horiz, color: Colors.green),
                                onTap: () {
                                  widget.onSwitchBranch(b['id'], b['name']);
                                  _loadAll();
                                },
                              ),
                            )),
                      ]
                    ] else if (!withinBookingHours)
                      _statusBanner(
                        Colors.red,
                        Icons.access_time_filled,
                        'الحجز مغلق حالياً',
                        statusMessage.isNotEmpty
                            ? statusMessage
                            : 'مواعيد الحجز: من ${_formatTime(bookingOpenTime)} إلى ${_formatTime(bookingCloseTime)}',
                      )
                    else
                      _statusBanner(Colors.green, Icons.check_circle, 'الفرع متاح للحجز الآن', ''),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              ElevatedButton.icon(
                onPressed: widget.onBookClick,
                icon: const Icon(Icons.format_list_numbered),
                label: Text(canBook ? 'عرض الطابور وحجز دور' : 'عرض الطابور الحالي'),
                style: ElevatedButton.styleFrom(
                  minimumSize: const Size(double.infinity, 56),
                  backgroundColor: canBook ? const Color(0xFF0B8F86) : Colors.blueGrey,
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _statusBanner(Color color, IconData icon, String title, String subtitle) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color),
      ),
      child: Column(
        children: [
          Icon(icon, color: color, size: 36),
          const SizedBox(height: 6),
          Text(title, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 16)),
          if (subtitle.isNotEmpty) ...[
            const SizedBox(height: 4),
            Text(subtitle, style: TextStyle(color: color), textAlign: TextAlign.center),
          ],
        ],
      ),
    );
  }
}