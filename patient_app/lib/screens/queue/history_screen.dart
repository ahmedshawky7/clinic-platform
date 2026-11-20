import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../services/api_service.dart';
import '../../widgets/appointment_status_badge.dart';

class HistoryScreen extends StatefulWidget {
  final String token;
  final int userId;
  const HistoryScreen({super.key, required this.token, required this.userId});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  late final ApiService _apiService = ApiService(token: widget.token);
  List<dynamic> _appointments = [];
  List<dynamic> _prescriptions = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _fetchAll();
  }

  Future<void> _fetchAll() async {
    setState(() => _isLoading = true);
    try {
      final results = await Future.wait([
        _apiService.getMyAppointmentsHistory(),
        _apiService.getMyPrescriptions(),
      ]);
      if (mounted) {
        setState(() {
          _appointments = results[0];
          _prescriptions = results[1];
        });
      }
    } catch (e) {
      debugPrint('Error fetching history: $e');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('ملفي الطبي وسجل الحجوزات'),
          backgroundColor: const Color(0xFF0B8F86),
          foregroundColor: Colors.white,
          centerTitle: true,
          bottom: const TabBar(
            labelColor: Colors.white,
            unselectedLabelColor: Colors.white70,
            indicatorColor: Colors.amberAccent,
            tabs: [
              Tab(icon: Icon(Icons.calendar_month), text: 'سجل الحجوزات'),
              Tab(icon: Icon(Icons.receipt_long), text: 'الروشتات الطبية'),
            ],
          ),
        ),
        body: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : TabBarView(
                children: [
                  RefreshIndicator(
                    onRefresh: _fetchAll,
                    child: _appointments.isEmpty
                        ? ListView(
                            children: const [
                              SizedBox(height: 120),
                              Center(child: Text('لا يوجد حجوزات سابقة', style: TextStyle(fontSize: 18))),
                            ],
                          )
                        : ListView.builder(
                            padding: const EdgeInsets.all(16),
                            itemCount: _appointments.length,
                            itemBuilder: (context, index) {
                              final a = _appointments[index];
                              final status = (a['status'] ?? '').toString();
                              final visitType = (a['visitType'] ?? 'EXAMINATION').toString();
                              final visitLabel = visitType == 'CONSULTATION' ? 'استشارة' : 'كشف جديد';
                              final queueNum = a['queueNumber'] ?? 0;
                              final fee = a['fee'] ?? 0;

                              return Card(
                                elevation: 2,
                                margin: const EdgeInsets.only(bottom: 12),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                                child: Padding(
                                  padding: const EdgeInsets.all(16),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Row(
                                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                        children: [
                                          Expanded(
                                            child: Text(
                                              (a['clinicName'] ?? 'فرع').toString(),
                                              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                                            ),
                                          ),
                                          AppointmentStatusBadge(status: status),
                                        ],
                                      ),
                                      const SizedBox(height: 6),
                                      Text(
                                        'الطبيب: ${a['doctorName'] ?? ''}',
                                        style: const TextStyle(color: Colors.blueGrey, fontWeight: FontWeight.w600),
                                      ),
                                      const SizedBox(height: 8),
                                      Row(
                                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                        children: [
                                          Text('التاريخ: ${a['scheduledDate'] ?? ''}', style: const TextStyle(color: Colors.grey)),
                                          if (queueNum > 0)
                                            Text('رقم الدور: $queueNum', style: const TextStyle(fontWeight: FontWeight.bold)),
                                        ],
                                      ),
                                      const Divider(height: 18),
                                      Row(
                                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                        children: [
                                          Text('نوع الزيارة: $visitLabel', style: const TextStyle(fontWeight: FontWeight.bold)),
                                          Text(
                                            '$fee ج',
                                            style: const TextStyle(
                                              color: Color(0xFF0B8F86),
                                              fontWeight: FontWeight.bold,
                                              fontSize: 15,
                                            ),
                                          ),
                                        ],
                                      ),
                                    ],
                                  ),
                                ),
                              );
                            },
                          ),
                  ),
                  RefreshIndicator(
                    onRefresh: _fetchAll,
                    child: _prescriptions.isEmpty
                        ? ListView(
                            children: const [
                              SizedBox(height: 120),
                              Center(child: Text('لا يوجد روشتات سابقة', style: TextStyle(fontSize: 18))),
                            ],
                          )
                        : ListView.builder(
                            padding: const EdgeInsets.all(16),
                            itemCount: _prescriptions.length,
                            itemBuilder: (context, index) {
                              final p = _prescriptions[index];
                              final date = DateTime.parse(p['createdAt']);
                              final formattedDate = DateFormat('yyyy-MM-dd').format(date);

                              return Card(
                                elevation: 3,
                                margin: const EdgeInsets.only(bottom: 14),
                                child: Padding(
                                  padding: const EdgeInsets.all(16.0),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text('التاريخ: $formattedDate', style: const TextStyle(color: Colors.grey)),
                                      const Divider(),
                                      const Text('التشخيص:', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                                      Text(p['diagnosis'] ?? 'غير متوفر', style: const TextStyle(fontSize: 15)),
                                      const SizedBox(height: 10),
                                      const Text('الأدوية:', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                                      Text(
                                        p['medications'] ?? 'غير متوفر',
                                        style: const TextStyle(fontSize: 15, color: Color(0xFF0B8F86)),
                                      ),
                                    ],
                                  ),
                                ),
                              );
                            },
                          ),
                  ),
                ],
              ),
      ),
    );
  }
}