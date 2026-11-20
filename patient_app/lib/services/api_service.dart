import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../config/api_config.dart';

class ApiException implements Exception {
  final int statusCode;
  final String message;

  ApiException(this.statusCode, this.message);

  @override
  String toString() => message;
}

class ApiService {
  final String? token;

  ApiService({this.token});

  Future<Map<String, String>> _headers({bool withAuth = true}) async {
    final headers = <String, String>{
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };
    if (withAuth) {
      String? authToken = token;
      if (authToken == null || authToken.isEmpty) {
        final prefs = await SharedPreferences.getInstance();
        authToken = prefs.getString('token');
      }
      if (authToken != null && authToken.isNotEmpty) {
        headers['Authorization'] = 'Bearer $authToken';
      }
    }
    return headers;
  }

  String _parseError(http.Response res, String fallback) {
    try {
      final decoded = jsonDecode(utf8.decode(res.bodyBytes));
      if (decoded is Map) {
        final msg = decoded['detail'] ?? decoded['error'] ?? decoded['message'];
        if (msg != null && msg.toString().trim().isNotEmpty) {
          return msg.toString();
        }
      }
    } catch (_) {}
    return fallback;
  }

  Future<Map<String, dynamic>> login(String username, String password) async {
    final res = await http.post(
      Uri.parse('${ApiConfig.baseUrl}/auth/login'),
      headers: await _headers(withAuth: false),
      body: jsonEncode({'username': username, 'password': password}),
    );
    if (res.statusCode >= 200 && res.statusCode < 300) {
      final data = jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
      final prefs = await SharedPreferences.getInstance();
      if (data['token'] != null) {
        await prefs.setString('token', data['token'] as String);
      }
      if (data['userId'] != null) {
        await prefs.setInt('userId', data['userId'] as int);
      }
      return data;
    }
    throw ApiException(res.statusCode, _parseError(res, 'فشل تسجيل الدخول'));
  }

  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
  }) async {
    final res = await http.post(
      Uri.parse('${ApiConfig.baseUrl}/auth/change-password'),
      headers: await _headers(),
      body: jsonEncode({
        'currentPassword': currentPassword,
        'newPassword': newPassword,
      }),
    );
    if (res.statusCode < 200 || res.statusCode >= 300) {
      throw ApiException(res.statusCode, _parseError(res, 'تعذر تغيير كلمة المرور'));
    }
  }

  Future<List<dynamic>> getOrganizations() async {
    final res = await http.get(
      Uri.parse('${ApiConfig.baseUrl}/public/organizations'),
      headers: await _headers(withAuth: false),
    );
    if (res.statusCode >= 200 && res.statusCode < 300) {
      return jsonDecode(utf8.decode(res.bodyBytes)) as List<dynamic>;
    }
    throw ApiException(res.statusCode, _parseError(res, 'تعذر تحميل المراكز والعيادات'));
  }

  Future<List<dynamic>> getAvailableClinics() async {
    final res = await http.get(
      Uri.parse('${ApiConfig.baseUrl}/public/clinics'),
      headers: await _headers(withAuth: false),
    );
    if (res.statusCode >= 200 && res.statusCode < 300) {
      return jsonDecode(utf8.decode(res.bodyBytes)) as List<dynamic>;
    }
    throw ApiException(res.statusCode, _parseError(res, 'تعذر تحميل العيادات'));
  }

  Future<List<dynamic>> getClinicDoctors(int clinicId) async {
    final res = await http.get(
      Uri.parse('${ApiConfig.baseUrl}/public/clinics/$clinicId/doctors'),
      headers: await _headers(withAuth: false),
    );
    if (res.statusCode >= 200 && res.statusCode < 300) {
      return jsonDecode(utf8.decode(res.bodyBytes)) as List<dynamic>;
    }
    throw ApiException(res.statusCode, _parseError(res, 'تعذر تحميل أطباء الفرع'));
  }

  Future<Map<String, dynamic>> getClinicDetails(int clinicId) async {
    final res = await http.get(
      Uri.parse('${ApiConfig.baseUrl}/clinics/$clinicId'),
      headers: await _headers(),
    );
    if (res.statusCode >= 200 && res.statusCode < 300) {
      return jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
    }
    throw ApiException(res.statusCode, _parseError(res, 'تعذر تحميل بيانات الفرع'));
  }

  Future<Map<String, dynamic>> bookAppointment({
    required int clinicId,
    required int doctorId,
    String visitType = 'EXAMINATION',
    DateTime? scheduledAt,
  }) async {
    final res = await http.post(
      Uri.parse('${ApiConfig.baseUrl}/appointments/book'),
      headers: await _headers(),
      body: jsonEncode({
        'clinicId': clinicId,
        'doctorId': doctorId,
        'visitType': visitType,
        if (scheduledAt != null) 'scheduledAt': scheduledAt.toIso8601String(),
      }),
    );
    if (res.statusCode >= 200 && res.statusCode < 300) {
      return jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
    }
    throw ApiException(res.statusCode, _parseError(res, 'تعذر إتمام الحجز'));
  }

  Future<void> cancelAppointment(int appointmentId) async {
    final res = await http.post(
      Uri.parse('${ApiConfig.baseUrl}/appointments/$appointmentId/cancel'),
      headers: await _headers(),
    );
    if (res.statusCode < 200 || res.statusCode >= 300) {
      throw ApiException(res.statusCode, _parseError(res, 'تعذر إلغاء الحجز'));
    }
  }

  Future<Map<String, dynamic>?> getMyActiveAppointment() async {
    final res = await http.get(
      Uri.parse('${ApiConfig.baseUrl}/patient/my-active-appointment'),
      headers: await _headers(),
    );
    if (res.statusCode == 204) return null;
    if (res.statusCode >= 200 && res.statusCode < 300) {
      return jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
    }
    throw ApiException(res.statusCode, _parseError(res, 'تعذر جلب حالة الحجز'));
  }

  Future<List<dynamic>> getMyAppointmentsHistory() async {
    final res = await http.get(
      Uri.parse('${ApiConfig.baseUrl}/patient/appointments/history'),
      headers: await _headers(),
    );
    if (res.statusCode >= 200 && res.statusCode < 300) {
      return jsonDecode(utf8.decode(res.bodyBytes)) as List<dynamic>;
    }
    throw ApiException(res.statusCode, _parseError(res, 'تعذر تحميل سجل الحجوزات'));
  }

  Future<Map<String, dynamic>> getMyProfile(int patientId) async {
    final res = await http.get(
      Uri.parse('${ApiConfig.baseUrl}/patient/$patientId/profile'),
      headers: await _headers(),
    );
    if (res.statusCode >= 200 && res.statusCode < 300) {
      return jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
    }
    throw ApiException(res.statusCode, _parseError(res, 'تعذر تحميل الملف الشخصي'));
  }

  Future<void> updateProfile(
    int patientId, {
    required String fullName,
    required String phoneNumber,
  }) async {
    final res = await http.put(
      Uri.parse('${ApiConfig.baseUrl}/patient/$patientId/profile'),
      headers: await _headers(),
      body: jsonEncode({
        'fullName': fullName,
        'phoneNumber': phoneNumber,
      }),
    );
    if (res.statusCode < 200 || res.statusCode >= 300) {
      throw ApiException(res.statusCode, _parseError(res, 'تعذر تحديث الملف الشخصي'));
    }
  }

  Future<List<dynamic>> getMyPrescriptions() async {
    final res = await http.get(
      Uri.parse('${ApiConfig.baseUrl}/patient/prescriptions'),
      headers: await _headers(),
    );
    if (res.statusCode >= 200 && res.statusCode < 300) {
      return jsonDecode(utf8.decode(res.bodyBytes)) as List<dynamic>;
    }
    throw ApiException(res.statusCode, _parseError(res, 'تعذر تحميل الروشتات'));
  }
}