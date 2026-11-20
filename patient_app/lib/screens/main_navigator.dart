import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:patient_app/screens/profile/profile_screen.dart';

import '../config/api_config.dart';
import 'home/explore_clinics_screen.dart';
import 'home/clinic_home_screen.dart';
import 'queue/queue_screen.dart';
import 'queue/history_screen.dart';

class MainNavigator extends StatefulWidget {
  final String token;
  final int userId;

  const MainNavigator({
    super.key,
    required this.token,
    required this.userId,
  });

  @override
  State<MainNavigator> createState() => _MainNavigatorState();
}

class _MainNavigatorState extends State<MainNavigator> {
  int _currentIndex = 0;

  int? _selectedClinicId;
  int? _selectedDoctorId;
  String _selectedClinicName = '';
  String _selectedDoctorName = '';
  String _selectedOrgName = '';

  @override
  void initState() {
    super.initState();
    _checkActiveAppointment();
  }

  Future<void> _checkActiveAppointment() async {
    try {
      final res = await http.get(
        Uri.parse('${ApiConfig.baseUrl}/appointments/active/${widget.userId}'),
        headers: {'Authorization': 'Bearer ${widget.token}'},
      );
      if (res.statusCode == 200) {
        final data = jsonDecode(utf8.decode(res.bodyBytes));
        if (!mounted) return;
        setState(() {
          _selectedClinicId = data['clinicId'];
          _selectedDoctorId = data['doctorId'];
          _currentIndex = 2; // الانتقال مباشرة للطابور إذا لديه تذكرة نشطة اليوم
        });
      }
    } catch (_) {}
  }

  void _openBranchDetails({
    required int clinicId,
    required int doctorId,
    required String clinicName,
    required String doctorName,
    required String orgName,
    bool goDirectToQueue = false,
  }) {
    setState(() {
      _selectedClinicId = clinicId;
      _selectedDoctorId = doctorId;
      _selectedClinicName = clinicName;
      _selectedDoctorName = doctorName;
      _selectedOrgName = orgName;
      _currentIndex = goDirectToQueue ? 2 : 1;
    });
  }

  @override
  Widget build(BuildContext context) {
    final List<Widget> screens = [
      ExploreClinicsScreen(
        onSelectDoctor: (clinicId, doctorId, clinicName, doctorName, orgName) {
          _openBranchDetails(
            clinicId: clinicId,
            doctorId: doctorId,
            clinicName: clinicName,
            doctorName: doctorName,
            orgName: orgName,
          );
        },
      ),
      _selectedClinicId != null && _selectedDoctorId != null
          ? ClinicHomeScreen(
              key: ValueKey('clinic-$_selectedClinicId-$_selectedDoctorId'),
              token: widget.token,
              userId: widget.userId,
              clinicId: _selectedClinicId!,
              doctorId: _selectedDoctorId!,
              doctorName: _selectedDoctorName,
              orgName: _selectedOrgName,
              onBookClick: () => setState(() => _currentIndex = 2),
              onSwitchBranch: (newClinicId, newClinicName) {
                setState(() {
                  _selectedClinicId = newClinicId;
                  _selectedClinicName = newClinicName;
                });
              },
            )
          : _buildSelectFirstPlaceholder(),
      _selectedClinicId != null && _selectedDoctorId != null
          ? QueueScreen(
              key: ValueKey('queue-$_selectedClinicId-$_selectedDoctorId'),
              token: widget.token,
              clinicId: _selectedClinicId!,
              doctorId: _selectedDoctorId!,
              userId: widget.userId,
              clinicName: _selectedClinicName,
              doctorName: _selectedDoctorName,
              onGoToActiveTicket: (cId, dId) {
                setState(() {
                  _selectedClinicId = cId;
                  _selectedDoctorId = dId;
                });
              },
            )
          : _buildSelectFirstPlaceholder(),
      HistoryScreen(token: widget.token, userId: widget.userId),
      PatientProfileScreen(token: widget.token, userId: widget.userId),
    ];

    return Scaffold(
      body: screens[_currentIndex],
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (index) => setState(() => _currentIndex = index),
        selectedItemColor: const Color(0xFF0B8F86),
        unselectedItemColor: Colors.grey,
        type: BottomNavigationBarType.fixed,
        selectedLabelStyle: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12),
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.explore), label: 'المراكز'),
          BottomNavigationBarItem(icon: Icon(Icons.local_hospital), label: 'العيادة'),
          BottomNavigationBarItem(icon: Icon(Icons.format_list_numbered), label: 'الطابور'),
          BottomNavigationBarItem(icon: Icon(Icons.history), label: 'ملفي الطبي'),
          BottomNavigationBarItem(icon: Icon(Icons.person), label: 'حسابي'),
        ],
      ),
    );
  }

  Widget _buildSelectFirstPlaceholder() {
    return Scaffold(
      appBar: AppBar(title: const Text('اختر منشأة أولاً'), centerTitle: true),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.local_hospital_outlined, size: 72, color: Colors.grey),
              const SizedBox(height: 16),
              const Text(
                'لم تختر عيادة أو طبيباً بعد',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              const Text('تصفح المراكز والعيادات واختر الفرع والتخصص والطبيب المناسب.'),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: () => setState(() => _currentIndex = 0),
                child: const Text('استعراض المراكز والعيادات'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}