import 'package:flutter/material.dart';
import 'package:patient_app/screens/auth/login_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'screens/main_navigator.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final prefs = await SharedPreferences.getInstance();
  final token = prefs.getString('token');
  final userId = prefs.getInt('userId');

  runApp(PatientApp(token: token, userId: userId));
}

class PatientApp extends StatelessWidget {
  final String? token;
  final int? userId;

  const PatientApp({super.key, this.token, this.userId});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'عيادتي - تطبيق المريض',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF0B8F86)),
        useMaterial3: true,
      ),
      builder: (context, child) {
        return Directionality(
          textDirection: TextDirection.rtl,
          child: child ?? const SizedBox.shrink(),
        );
      },
      home: token != null && userId != null
          ? MainNavigator(token: token!, userId: userId!)
          : const LoginScreen(),
    );
  }
}

