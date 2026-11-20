import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import '../../config/api_config.dart';

class ExploreClinicsScreen extends StatefulWidget {
  final void Function(
    int clinicId,
    int doctorId,
    String clinicName,
    String doctorName,
    String orgName,
  ) onSelectDoctor;

  const ExploreClinicsScreen({super.key, required this.onSelectDoctor});

  @override
  State<ExploreClinicsScreen> createState() => _ExploreClinicsScreenState();
}

class _ExploreClinicsScreenState extends State<ExploreClinicsScreen> {
  List<dynamic> _orgs = [];
  String _search = '';
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadOrgs();
  }

  Future<void> _loadOrgs() async {
    setState(() => _loading = true);
    try {
      final res = await http.get(Uri.parse('${ApiConfig.baseUrl}/public/organizations'));
      if (res.statusCode == 200 && mounted) {
        setState(() => _orgs = jsonDecode(utf8.decode(res.bodyBytes)));
      }
    } catch (e) {
      debugPrint('Error loading organizations: $e');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _openDoctorsSheet(Map<String, dynamic> org, Map<String, dynamic> clinic) async {
    final specialties = (org['specialties'] as List<dynamic>? ?? []);
    int? selectedSpecId;
    List<dynamic> doctors = [];
    bool loadingDocs = true;

    Future<void> fetchDocs(StateSetter setModalState) async {
      setModalState(() => loadingDocs = true);
      final q = selectedSpecId != null ? '?specialtyId=$selectedSpecId' : '';
      try {
        final res = await http.get(Uri.parse('${ApiConfig.baseUrl}/public/clinics/${clinic['id']}/doctors$q'));
        if (res.statusCode == 200) {
          setModalState(() {
            doctors = jsonDecode(utf8.decode(res.bodyBytes));
            loadingDocs = false;
          });
        }
      } catch (_) {
        setModalState(() => loadingDocs = false);
      }
    }

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) {
        return StatefulBuilder(
          builder: (ctx, setModalState) {
            if (loadingDocs && doctors.isEmpty) {
              fetchDocs(setModalState);
            }
            return DraggableScrollableSheet(
              expand: false,
              initialChildSize: 0.65,
              maxChildSize: 0.9,
              builder: (_, scrollController) => Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${org['name']} — ${clinic['name']}',
                      style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 10),
                    if (specialties.isNotEmpty)
                      SizedBox(
                        height: 42,
                        child: ListView(
                          scrollDirection: Axis.horizontal,
                          children: [
                            Padding(
                              padding: const EdgeInsets.only(left: 8),
                              child: ChoiceChip(
                                label: const Text('كل التخصصات'),
                                selected: selectedSpecId == null,
                                onSelected: (_) {
                                  selectedSpecId = null;
                                  fetchDocs(setModalState);
                                },
                              ),
                            ),
                            ...specialties.map((s) => Padding(
                                  padding: const EdgeInsets.only(left: 8),
                                  child: ChoiceChip(
                                    label: Text(s['name']),
                                    selected: selectedSpecId == s['id'],
                                    onSelected: (_) {
                                      selectedSpecId = s['id'];
                                      fetchDocs(setModalState);
                                    },
                                  ),
                                )),
                          ],
                        ),
                      ),
                    const SizedBox(height: 12),
                    Expanded(
                      child: loadingDocs
                          ? const Center(child: CircularProgressIndicator())
                          : doctors.isEmpty
                              ? const Center(child: Text('لا يوجد أطباء متاحون لهذا الاختيار'))
                              : ListView.builder(
                                  controller: scrollController,
                                  itemCount: doctors.length,
                                  itemBuilder: (_, i) {
                                    final d = doctors[i];
                                    final examFee = d['examinationFee'] ?? d['consultationFee'] ?? 200;
                                    final followUpFee = d['followUpFee'] ?? 100;
                                    return Card(
                                      margin: const EdgeInsets.only(bottom: 10),
                                      child: ListTile(
                                        leading: const CircleAvatar(
                                          backgroundColor: Color(0xFFD9F2EE),
                                          child: Icon(Icons.person, color: Color(0xFF0B8F86)),
                                        ),
                                        title: Text(d['fullName'], style: const TextStyle(fontWeight: FontWeight.bold)),
                                        subtitle: Text(
                                          '${d['specialtyName'] ?? ''} • كشف: $examFee ج • استشارة: $followUpFee ج\nمواعيد الطبيب: ${d['arrivalTime']} - ${d['departureTime']}',
                                        ),
                                        isThreeLine: true,
                                        trailing: ElevatedButton(
                                          style: ElevatedButton.styleFrom(
                                            backgroundColor: const Color(0xFF0B8F86),
                                            foregroundColor: Colors.white,
                                          ),
                                          onPressed: () {
                                            Navigator.pop(ctx);
                                            widget.onSelectDoctor(
                                              clinic['id'],
                                              d['doctorId'],
                                              clinic['name'],
                                              d['fullName'],
                                              org['name'],
                                            );
                                          },
                                          child: const Text('اختيار'),
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
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final filtered = _orgs.where((o) {
      final q = _search.trim().toLowerCase();
      if (q.isEmpty) return true;
      final nameMatch = (o['name'] ?? '').toString().toLowerCase().contains(q);
      final specMatch = (o['specialties'] as List<dynamic>? ?? [])
          .any((s) => (s['name'] ?? '').toString().toLowerCase().contains(q));
      return nameMatch || specMatch;
    }).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('المراكز والعيادات', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: const Color(0xFF0B8F86),
        foregroundColor: Colors.white,
        centerTitle: true,
      ),
      body: RefreshIndicator(
        onRefresh: _loadOrgs,
        child: _loading
            ? const Center(child: CircularProgressIndicator())
            : ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  TextField(
                    onChanged: (v) => setState(() => _search = v),
                    decoration: InputDecoration(
                      hintText: 'ابحث باسم المركز، الطبيب، أو التخصص...',
                      prefixIcon: const Icon(Icons.search),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(14)),
                      filled: true,
                      fillColor: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 16),
                  if (filtered.isEmpty)
                    const Center(
                      child: Padding(
                        padding: EdgeInsets.all(32),
                        child: Text('لا توجد عيادات أو مراكز مطابقة للبحث.'),
                      ),
                    ),
                  ...filtered.map((org) {
                    final clinics = (org['clinics'] as List<dynamic>? ?? []);
                    final specialties = (org['specialties'] as List<dynamic>? ?? []);
                    final isCenter = org['type'] == 'MEDICAL_CENTER';

                    return Card(
                      elevation: 2,
                      margin: const EdgeInsets.only(bottom: 16),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
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
                                    org['name'],
                                    style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                                  ),
                                ),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                  decoration: BoxDecoration(
                                    color: isCenter ? Colors.indigo[50] : Colors.teal[50],
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                  child: Text(
                                    isCenter ? 'مركز طبي' : 'عيادة خاصة',
                                    style: TextStyle(
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold,
                                      color: isCenter ? Colors.indigo : Colors.teal[800],
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            if (specialties.isNotEmpty) ...[
                              const SizedBox(height: 8),
                              Wrap(
                                spacing: 6,
                                runSpacing: 4,
                                children: specialties
                                    .map<Widget>((s) => Chip(
                                          label: Text(s['name'], style: const TextStyle(fontSize: 12)),
                                          visualDensity: VisualDensity.compact,
                                          backgroundColor: const Color(0xFFD9F2EE),
                                        ))
                                    .toList(),
                              ),
                            ],
                            const Divider(height: 20),
                            const Text('الفروع:', style: TextStyle(fontWeight: FontWeight.bold, color: Colors.grey)),
                            ...clinics.map((c) => ListTile(
                                  contentPadding: EdgeInsets.zero,
                                  leading: Icon(
                                    Icons.location_on,
                                    color: (c['openToday'] ?? true) ? const Color(0xFF0B8F86) : Colors.red,
                                  ),
                                  title: Text(c['name'], style: const TextStyle(fontWeight: FontWeight.bold)),
                                  subtitle: Text('${c['city'] ?? ''} ${c['address'] ?? ''}'),
                                  trailing: const Icon(Icons.arrow_forward_ios, size: 16),
                                  onTap: () => _openDoctorsSheet(org, c),
                                )),
                          ],
                        ),
                      ),
                    );
                  }),
                ],
              ),
      ),
    );
  }
}