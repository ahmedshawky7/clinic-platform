import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';
import DashboardLayout from '../components/DashboardLayout';
import { Users, Activity, Calendar, TrendingUp, Banknote } from 'lucide-react';
import {
  PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend, CartesianGrid
} from 'recharts';

const COLORS = ['#0b8f86', '#f2b45f'];

export default function AnalyticsDashboard() {
  const { selectedClinicId, selectedDoctorId } = useAuth();
  const [stats, setStats] = useState({
    totalPatients: 0,
    todayAppointments: 0,
    waitingCount: 0,
    walkInCount: 0,
    onlineCount: 0,
    todayRevenue: 0,
    completedRevenue: 0,
    examinationCount: 0,
    consultationCount: 0,
    examinationRevenue: 0,
    consultationRevenue: 0,
    weekly: [],
  });

  useEffect(() => {
    if (!selectedClinicId || !selectedDoctorId) return;
    const fetchStats = async () => {
      try {
        const res = await axiosClient.get(`/queues/${selectedClinicId}/analytics?doctorId=${selectedDoctorId}`);
        setStats(res.data);
      } catch (error) {
        console.error(error);
      }
    };
    fetchStats();
  }, [selectedClinicId, selectedDoctorId]);

  const pieData = [
    { name: 'حجز إلكتروني', value: stats.onlineCount || 0 },
    { name: 'زيارة العيادة (Walk-in)', value: stats.walkInCount || 0 },
  ];

  return (
    <DashboardLayout title="لوحة الإحصائيات والإيرادات">
      <div className="max-w-6xl mx-auto space-y-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 stagger">
          <div className="rounded-3xl p-6 text-white bg-gradient-to-br from-blue-600 to-indigo-700 shadow-lg flex justify-between items-start">
            <div>
              <p className="font-bold text-blue-100 mb-2">إجمالي مرضى المنشأة</p>
              <p className="text-4xl font-black">{stats.totalPatients}</p>
            </div>
            <Users size={32} className="text-blue-100" />
          </div>

          <div className="card p-6 flex justify-between items-start">
            <div>
              <p className="font-bold text-slate-400 mb-2">حجوزات الفرع اليوم</p>
              <p className="text-4xl font-black text-slate-800">{stats.todayAppointments}</p>
            </div>
            <Calendar size={32} className="text-blue-500" />
          </div>

          <div className="card p-6 flex justify-between items-start">
            <div>
              <p className="font-bold text-slate-400 mb-2">منتظرين الآن عند الطبيب</p>
              <p className="text-4xl font-black text-orange-500">{stats.waitingCount}</p>
            </div>
            <Activity size={32} className="text-orange-500" />
          </div>

          <div className="rounded-3xl p-6 text-white bg-gradient-to-br from-emerald-500 to-teal-700 shadow-lg flex justify-between items-start">
            <div>
              <p className="font-bold text-emerald-100 mb-2">إيرادات اليوم المتوقعة</p>
              <p className="text-4xl font-black">{stats.todayRevenue ?? 0} ج</p>
              <p className="text-xs text-emerald-100 mt-1 font-bold">
                المحصّل فعلياً (مكتمل): {stats.completedRevenue ?? 0} ج
              </p>
            </div>
            <Banknote size={32} className="text-emerald-100" />
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="card p-6 flex justify-between items-center">
            <div>
              <p className="font-bold text-slate-400 text-sm">كشف جديد اليوم ({stats.examinationCount ?? 0} حالة)</p>
              <p className="text-3xl font-black text-indigo-700 mt-1">{stats.examinationRevenue ?? 0} ج</p>
            </div>
            <span className="px-4 py-2 rounded-2xl bg-indigo-50 text-indigo-700 font-black text-sm">EXAMINATION</span>
          </div>

          <div className="card p-6 flex justify-between items-center">
            <div>
              <p className="font-bold text-slate-400 text-sm">استشارات اليوم ({stats.consultationCount ?? 0} حالة)</p>
              <p className="text-3xl font-black text-teal-700 mt-1">{stats.consultationRevenue ?? 0} ج</p>
            </div>
            <span className="px-4 py-2 rounded-2xl bg-teal-50 text-teal-700 font-black text-sm">CONSULTATION</span>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <div className="card p-8">
            <h3 className="text-xl font-black text-slate-800 mb-6 flex items-center gap-2">
              <TrendingUp className="text-indigo-500" /> مصدر الحجوزات اليوم
            </h3>
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={pieData} cx="50%" cy="50%" innerRadius={65} outerRadius={105} paddingAngle={6} dataKey="value">
                    {pieData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                  </Pie>
                  <Tooltip />
                  <Legend verticalAlign="bottom" height={36} />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="card p-8">
            <h3 className="text-xl font-black text-slate-800 mb-6 flex items-center gap-2">
              <TrendingUp className="text-indigo-500" /> حركة الحجوزات (آخر 7 أيام)
            </h3>
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={stats.weekly}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="day" />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Bar dataKey="appointments" fill="#0b8f86" radius={[10, 10, 0, 0]} barSize={38} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}