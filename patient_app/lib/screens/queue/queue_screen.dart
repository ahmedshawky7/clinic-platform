import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:qr_flutter/qr_flutter.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import '../../config/api_config.dart';
import '../../services/api_service.dart';
import '../../widgets/active_appointment_card.dart';

class QueueScreen extends StatefulWidget {
  final String token;
  final int clinicId;
  final int doctorId;
  final int userId;
  final String clinicName;
  final String doctorName;
  final void Function(int clinicId, int doctorId) onGoToActiveTicket;

  const QueueScreen({
    super.key,
    required this.token,
    required this.clinicId,
    required this.doctorId,
    required this.userId,
    required this.clinicName,
    required this.doctorName,
    required this.onGoToActiveTicket,
  });

  @override
  State<QueueScreen> createState() => _QueueScreenState();
}

class _QueueScreenState extends State<QueueScreen> {
  late final ApiService _apiService = ApiService(token: widget.token);
  StompClient? _stompClient;
  Timer? _refreshTimer;

  int? _currentQueueNumber;
  int _waitingCount = 0;
  int _estimatedMinutes = 0;
  String _doctorArrivalTime = '17:00';
  bool _isConnected = false;
  bool _isBooking = false;

  Map<String, dynamic>? _activeAppointment;
  String? _myTicketNumber;
  int? _activeClinicId;
  int? _activeDoctorId;
  String _activeStatus = '';

  String _selectedVisitType = 'EXAMINATION';
  num? _examinationFee;
  num? _followUpFee;

  bool _canBook = false;
  bool _loadingStatus = true;
  String _bookingOpenTime = '00:00';
  String _bookingCloseTime = '23:59';
  String _bookingClosedReason = '';

  @override
  void initState() {
    super.initState();
    _fetchInitialQueue();
    _fetchActiveAppointment();
    _fetchClinicStatus();
    _fetchDoctorFees();
    _connectWebSocket();

    _refreshTimer = Timer.periodic(const Duration(seconds: 30), (_) {
      _fetchInitialQueue();
      _fetchClinicStatus();
      _fetchActiveAppointment();
    });
  }

  String _formatArabicTime(String time24) {
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

  String _formatWaitDuration(int totalMinutes) {
    if (totalMinutes <= 0) return '0 دقيقة';
    if (totalMinutes < 60) return '$totalMinutes دقيقة';
    final hours = totalMinutes ~/ 60;
    final mins = totalMinutes % 60;
    final hourText = hours == 1 ? 'ساعة' : (hours == 2 ? 'ساعتين' : '$hours ساعات');
    if (mins == 0) return hourText;
    return '$hourText و $mins دقيقة';
  }

  bool _isLocalTimeWithinWindow(String openStr, String closeStr) {
    final openParts = openStr.split(':');
    final closeParts = closeStr.split(':');
    if (openParts.length < 2 || closeParts.length < 2) return true;

    final openMin = (int.tryParse(openParts[0]) ?? 0) * 60 + (int.tryParse(openParts[1]) ?? 0);
    final closeMin = (int.tryParse(closeParts[0]) ?? 23) * 60 + (int.tryParse(closeParts[1]) ?? 59);
    final now = DateTime.now();
    final nowMin = now.hour * 60 + now.minute;

    if (openMin <= closeMin) {
      return nowMin >= openMin && nowMin <= closeMin;
    } else {
      return nowMin >= openMin || nowMin <= closeMin;
    }
  }

  Future<void> _fetchDoctorFees() async {
    try {
      final doctors = await _apiService.getClinicDoctors(widget.clinicId);
      final match = doctors.where((d) => d['doctorId'] == widget.doctorId).firstOrNull;
      if (match != null && mounted) {
        setState(() {
          _examinationFee = (match['examinationFee'] ?? match['consultationFee']) as num?;
          _followUpFee = match['followUpFee'] as num?;
        });
      }
    } catch (_) {}
  }

  Future<void> _fetchClinicStatus() async {
    try {
      final res = await http.get(
        Uri.parse('${ApiConfig.baseUrl}/clinics/${widget.clinicId}'),
        headers: {'Authorization': 'Bearer ${widget.token}'},
      );
      if (res.statusCode == 200 && mounted) {
        final data = jsonDecode(utf8.decode(res.bodyBytes));
        final String openTime = (data['bookingOpenTime'] ?? '00:00').toString();
        final String closeTime = (data['bookingCloseTime'] ?? '23:59').toString();
        final String arrivalTime = (data['doctorArrivalTime'] ?? _doctorArrivalTime).toString();

        final bool serverCanBook = data['canBookNow'] ?? true;
        final bool withinHours = (data['withinBookingHours'] ?? true) &&
            _isLocalTimeWithinWindow(openTime, closeTime);

        final bool finalCanBook = serverCanBook && withinHours;
        String statusMsg = (data['bookingStatusMessage'] ?? '').toString();
        if (!finalCanBook && statusMsg.isEmpty) {
          statusMsg = 'الحجز متاح يومياً من ${_formatArabicTime(openTime)} حتى ${_formatArabicTime(closeTime)}';
        }

        setState(() {
          _bookingOpenTime = openTime;
          _bookingCloseTime = closeTime;
          _doctorArrivalTime = arrivalTime;
          _canBook = finalCanBook;
          _bookingClosedReason = statusMsg;
          _loadingStatus = false;
        });
      }
    } catch (e) {
      debugPrint('Error fetching clinic status: $e');
      if (mounted) setState(() => _loadingStatus = false);
    }
  }

  void _connectWebSocket() {
    _stompClient = StompClient(
      config: StompConfig(
        url: ApiConfig.wsUrl,
        onConnect: (frame) {
          if (!mounted) return;
          setState(() => _isConnected = true);
          _stompClient?.subscribe(
            destination: '/topic/clinic/${widget.clinicId}/doctor/${widget.doctorId}/queue',
            callback: (frame) {
              if (frame.body != null && mounted) {
                final data = jsonDecode(frame.body!);
                setState(() {
                  _currentQueueNumber = data['currentQueueNumber'];
                  _waitingCount = data['waitingCount'] ?? 0;
                  _estimatedMinutes = (data['estimatedWaitMinutes'] ?? 0) as int;
                  if (data['doctorArrivalTime'] != null) {
                    _doctorArrivalTime = data['doctorArrivalTime'].toString();
                  }
                });
                _fetchActiveAppointment();
              }
            },
          );
        },
        onWebSocketError: (dynamic error) => debugPrint('WS Error: $error'),
        onDisconnect: (frame) {
          if (mounted) setState(() => _isConnected = false);
        },
      ),
    );
    _stompClient?.activate();
  }

  Future<void> _fetchActiveAppointment() async {
    try {
      final data = await _apiService.getMyActiveAppointment();
      if (!mounted) return;
      if (data == null) {
        setState(() {
          _activeAppointment = null;
          _myTicketNumber = null;
          _activeClinicId = null;
          _activeDoctorId = null;
          _activeStatus = '';
        });
      } else {
        setState(() {
          _activeAppointment = data;
          _myTicketNumber = data['queueNumber']?.toString();
          _activeClinicId = data['clinicId'] as int?;
          _activeDoctorId = data['doctorId'] as int?;
          _activeStatus = (data['status'] ?? '').toString().toUpperCase();
        });
      }
    } catch (e) {
      debugPrint('Error fetching active appointment: $e');
    }
  }

  Future<void> _fetchInitialQueue() async {
    try {
      final res = await http.get(
        Uri.parse('${ApiConfig.baseUrl}/queues/${widget.clinicId}?doctorId=${widget.doctorId}'),
        headers: {'Authorization': 'Bearer ${widget.token}'},
      );
      if (res.statusCode == 200 && mounted) {
        final data = jsonDecode(utf8.decode(res.bodyBytes));
        setState(() {
          _currentQueueNumber = data['currentQueueNumber'];
          _waitingCount = data['waitingCount'] ?? 0;
          _estimatedMinutes = (data['estimatedWaitMinutes'] ?? 0) as int;
          if (data['doctorArrivalTime'] != null) {
            _doctorArrivalTime = data['doctorArrivalTime'].toString();
          }
        });
      }
    } catch (e) {
      debugPrint('Queue API Exception: $e');
    }
  }

  Future<void> _bookAppointment() async {
    await _fetchClinicStatus();
    if (!_canBook) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(_bookingClosedReason.isNotEmpty ? _bookingClosedReason : 'الحجز مغلق حالياً')),
        );
      }
      return;
    }

    setState(() => _isBooking = true);
    try {
      await ActiveAppointmentCard.handleBookAction(
        context: context,
        apiService: _apiService,
        clinicId: widget.clinicId,
        doctorId: widget.doctorId,
        visitType: _selectedVisitType,
        onBooked: () async {
          await _fetchActiveAppointment();
          await _fetchInitialQueue();
        },
      );
    } finally {
      if (mounted) setState(() => _isBooking = false);
    }
  }

  @override
  void dispose() {
    _refreshTimer?.cancel();
    _stompClient?.deactivate();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bool isSameDoctorAndClinic =
        _activeAppointment != null && _activeClinicId == widget.clinicId && _activeDoctorId == widget.doctorId;

    final bool isLiveTicketHere = isSameDoctorAndClinic &&
        (_activeStatus == 'WAITING' || _activeStatus == 'IN_PROGRESS' || _activeStatus == 'SKIPPED');

    final bool isCancelledHere = isSameDoctorAndClinic && _activeStatus == 'CANCELLED';

    final bool hasTicketElsewhere = _activeAppointment != null &&
        !isSameDoctorAndClinic &&
        (_activeStatus == 'WAITING' || _activeStatus == 'IN_PROGRESS' || _activeStatus == 'SKIPPED') &&
        _activeClinicId != null &&
        _activeDoctorId != null;

    return Scaffold(
      backgroundColor: Colors.grey[100],
      appBar: AppBar(
        title: Text(
          widget.doctorName.isNotEmpty ? 'طابور ${widget.doctorName}' : 'الطابور الحي',
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
        backgroundColor: _isConnected ? const Color(0xFF0B8F86) : Colors.red[600],
        foregroundColor: Colors.white,
        centerTitle: true,
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          await _fetchInitialQueue();
          await _fetchActiveAppointment();
          await _fetchClinicStatus();
          await _fetchDoctorFees();
        },
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(20.0),
          child: Column(
            children: [
              if (widget.clinicName.isNotEmpty)
                Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: Text(widget.clinicName, style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.blueGrey)),
                ),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFF0B8F86), Color(0xFF075B59)],
                    begin: Alignment.topRight,
                    end: Alignment.bottomLeft,
                  ),
                  borderRadius: BorderRadius.circular(24),
                ),
                child: Column(
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: [
                        Column(
                          children: [
                            const Text('الرقم الحالي', style: TextStyle(color: Colors.white70, fontWeight: FontWeight.bold)),
                            const SizedBox(height: 8),
                            Text('${_currentQueueNumber ?? "-"}',
                                style: const TextStyle(fontSize: 54, fontWeight: FontWeight.bold, color: Colors.white)),
                          ],
                        ),
                        Container(width: 1, height: 60, color: Colors.white30),
                        Column(
                          children: [
                            const Text('المنتظرون', style: TextStyle(color: Colors.white70, fontWeight: FontWeight.bold)),
                            const SizedBox(height: 8),
                            Text('$_waitingCount',
                                style: const TextStyle(fontSize: 54, fontWeight: FontWeight.bold, color: Colors.amberAccent)),
                          ],
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.14),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Column(
                        children: [
                          Text(
                            'الانتظار المتوقع: ${_formatWaitDuration(_estimatedMinutes)}',
                            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'يبدأ الكشف الساعة ${_formatArabicTime(_doctorArrivalTime)}',
                            style: const TextStyle(color: Colors.amberAccent, fontWeight: FontWeight.bold, fontSize: 13),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),

              if (isCancelledHere) ...[
                ActiveAppointmentCard(
                  appointment: _activeAppointment,
                  apiService: _apiService,
                  onRefresh: _fetchActiveAppointment,
                ),
                const SizedBox(height: 16),
              ],

              if (isLiveTicketHere) ...[
                ActiveAppointmentCard(
                  appointment: _activeAppointment,
                  apiService: _apiService,
                  onRefresh: () async {
                    await _fetchActiveAppointment();
                    await _fetchInitialQueue();
                  },
                  onCancelled: () async {
                    await _fetchActiveAppointment();
                    await _fetchInitialQueue();
                  },
                ),
                if (_activeStatus == 'WAITING' || _activeStatus == 'IN_PROGRESS') ...[
                  const SizedBox(height: 16),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(24),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(24),
                      boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 15)],
                    ),
                    child: Column(
                      children: [
                        const Text('تذكرتك الإلكترونية', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                        const SizedBox(height: 16),
                        QrImageView(
                          data: 'clinic:${widget.clinicId}|doctor:${widget.doctorId}|ticket:${_myTicketNumber ?? 0}',
                          version: QrVersions.auto,
                          size: 180.0,
                        ),
                        const SizedBox(height: 12),
                        Text(
                          'رقم دورك: ${_myTicketNumber ?? "-"}',
                          style: const TextStyle(fontSize: 26, color: Color(0xFF0B8F86), fontWeight: FontWeight.w900),
                        ),
                      ],
                    ),
                  ),
                ],
              ] else ...[
                if (hasTicketElsewhere)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 16),
                    child: Card(
                      color: Colors.amber[50],
                      child: ListTile(
                        leading: const Icon(Icons.confirmation_number, color: Colors.orange),
                        title: Text('لديك تذكرة نشطة اليوم برقم ${_myTicketNumber ?? "-"}'),
                        trailing: TextButton(
                          onPressed: () => widget.onGoToActiveTicket(_activeClinicId!, _activeDoctorId!),
                          child: const Text('عرض التذكرة'),
                        ),
                      ),
                    ),
                  ),
                if (_loadingStatus)
                  const Padding(
                    padding: EdgeInsets.all(16),
                    child: CircularProgressIndicator(),
                  )
                else if (!_canBook)
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(18),
                    decoration: BoxDecoration(
                      color: Colors.red[50],
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: Colors.red[300]!),
                    ),
                    child: Column(
                      children: [
                        const Icon(Icons.access_time_filled, color: Colors.red, size: 38),
                        const SizedBox(height: 8),
                        const Text(
                          'الحجز غير متاح حالياً',
                          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.red),
                        ),
                        const SizedBox(height: 6),
                        Text(
                          _bookingClosedReason.isNotEmpty
                              ? _bookingClosedReason
                              : 'انتهى وقت استقبال الحجوزات للفرع.',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: Colors.red[900], fontWeight: FontWeight.w600, height: 1.5),
                        ),
                        const SizedBox(height: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Text(
                            'الحجز: ${_formatArabicTime(_bookingOpenTime)} - ${_formatArabicTime(_bookingCloseTime)}\nبدء الكشف: ${_formatArabicTime(_doctorArrivalTime)}',
                            textAlign: TextAlign.center,
                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: Color(0xFF075B59)),
                          ),
                        ),
                      ],
                    ),
                  )
                else ...[
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(16),
                    margin: const EdgeInsets.only(bottom: 16),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: Colors.grey.shade200),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'اختر نوع الزيارة:',
                          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                        ),
                        const SizedBox(height: 10),
                        Row(
                          children: [
                            Expanded(
                              child: ChoiceChip(
                                label: SizedBox(
                                  width: double.infinity,
                                  child: Text(
                                    _examinationFee != null
                                        ? 'كشف جديد ($_examinationFee ج)'
                                        : 'كشف جديد',
                                    textAlign: TextAlign.center,
                                    style: const TextStyle(fontWeight: FontWeight.bold),
                                  ),
                                ),
                                selected: _selectedVisitType == 'EXAMINATION',
                                selectedColor: const Color(0xFFD9F2EE),
                                onSelected: (_) => setState(() => _selectedVisitType = 'EXAMINATION'),
                              ),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: ChoiceChip(
                                label: SizedBox(
                                  width: double.infinity,
                                  child: Text(
                                    _followUpFee != null
                                        ? 'استشارة ($_followUpFee ج)'
                                        : 'استشارة',
                                    textAlign: TextAlign.center,
                                    style: const TextStyle(fontWeight: FontWeight.bold),
                                  ),
                                ),
                                selected: _selectedVisitType == 'CONSULTATION',
                                selectedColor: const Color(0xFFD9F2EE),
                                onSelected: (_) => setState(() => _selectedVisitType = 'CONSULTATION'),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  ElevatedButton.icon(
                    onPressed: _isBooking ? null : _bookAppointment,
                    icon: const Icon(Icons.add),
                    label: Text(
                      _isBooking ? 'جاري الحجز...' : 'احجز دور الآن',
                      style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold),
                    ),
                    style: ElevatedButton.styleFrom(
                      minimumSize: const Size(double.infinity, 56),
                      backgroundColor: const Color(0xFF0B8F86),
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                    ),
                  ),
                ],
              ],
            ],
          ),
        ),
      ),
    );
  }
}