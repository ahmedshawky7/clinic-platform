import { useEffect, useRef, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Activity, Stethoscope, Users, Volume2, VolumeX, Building2 } from 'lucide-react';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export default function TvScreen() {
  const [searchParams] = useSearchParams();
  const clinicId = Number(searchParams.get('clinicId')) || 1;
  const rawDoctorId = searchParams.get('doctorId');
  const singleDoctorId = rawDoctorId && rawDoctorId.trim() !== '' ? Number(rawDoctorId) : null;

  const [tvInfo, setTvInfo] = useState(null);
  const [queuesByDoctor, setQueuesByDoctor] = useState({});
  const [connected, setConnected] = useState(false);
  const [soundEnabled, setSoundEnabled] = useState(true);
  const [highlightedDoctorId, setHighlightedDoctorId] = useState(null);

  const lastSpokenRef = useRef({});
  const audioCtxRef = useRef(null);
  const soundEnabledRef = useRef(true);

  useEffect(() => {
    soundEnabledRef.current = soundEnabled;
  }, [soundEnabled]);

  const playChime = useCallback(() => {
    if (!soundEnabledRef.current) return;
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (!AudioCtx) return;
      if (!audioCtxRef.current) {
        audioCtxRef.current = new AudioCtx();
      }
      const ctx = audioCtxRef.current;
      if (ctx.state === 'suspended') {
        ctx.resume();
      }
      const now = ctx.currentTime;
      const notes = [
        { freq: 880, start: 0, duration: 0.22 },
        { freq: 1174.66, start: 0.16, duration: 0.4 },
      ];
      notes.forEach(({ freq, start, duration }) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, now + start);
        gain.gain.setValueAtTime(0.001, now + start);
        gain.gain.exponentialRampToValueAtTime(0.25, now + start + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.001, now + start + duration);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(now + start);
        osc.stop(now + start + duration);
      });
    } catch {
      // ignore
    }
  }, []);

  const announceTurn = useCallback((doctorId, queueNumber, doctorName) => {
    if (!queueNumber || !soundEnabledRef.current) return;
    if (lastSpokenRef.current[doctorId] === queueNumber) return;

    lastSpokenRef.current[doctorId] = queueNumber;
    setHighlightedDoctorId(doctorId);
    setTimeout(() => {
      setHighlightedDoctorId((prev) => (prev === doctorId ? null : prev));
    }, 3500);

    playChime();

    try {
      window.speechSynthesis.cancel();
      const text = doctorName
        ? `رقم ${queueNumber} يتفضل بالدخول إلى ${doctorName}`
        : `رقم ${queueNumber} يتفضل يدخل`;
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = 'ar-EG';
      utterance.rate = 0.9;
      setTimeout(() => {
        try {
          window.speechSynthesis.speak(utterance);
        } catch {
          // ignore
        }
      }, 320);
    } catch {
      // ignore if speech synthesis is unsupported
    }
  }, [playChime]);

  const toggleSound = () => {
    const next = !soundEnabled;
    setSoundEnabled(next);
    if (next) {
      try {
        const AudioCtx = window.AudioContext || window.webkitAudioContext;
        if (AudioCtx) {
          if (!audioCtxRef.current) audioCtxRef.current = new AudioCtx();
          if (audioCtxRef.current.state === 'suspended') audioCtxRef.current.resume();
        }
      } catch {
        // ignore
      }
    }
  };

  useEffect(() => {
    let cancelled = false;
    let stompClient = null;

    const init = async () => {
      try {
        const infoRes = await fetch(`${API_BASE}/api/public/clinics/${clinicId}/tv-info`);
        if (!infoRes.ok) return;
        const info = await infoRes.json();
        if (cancelled) return;
        setTvInfo(info);

        const allDocs = info.doctors || [];
        const targetDocs = singleDoctorId
          ? allDocs.filter((d) => Number(d.doctorId) === singleDoctorId)
          : allDocs;

        const effectiveDocs =
          targetDocs.length > 0
            ? targetDocs
            : singleDoctorId
              ? [{ doctorId: singleDoctorId, fullName: '', specialtyName: '' }]
              : [];

        const initialEntries = await Promise.all(
          effectiveDocs.map(async (doc) => {
            try {
              const qRes = await fetch(`${API_BASE}/api/queues/${clinicId}?doctorId=${doc.doctorId}`);
              if (qRes.ok) {
                const qData = await qRes.json();
                if (qData?.currentQueueNumber) {
                  lastSpokenRef.current[doc.doctorId] = qData.currentQueueNumber;
                }
                return [doc.doctorId, qData];
              }
            } catch {
              // ignore
            }
            return [doc.doctorId, { currentQueueNumber: null, waitingCount: 0 }];
          })
        );

        if (!cancelled) {
          setQueuesByDoctor(Object.fromEntries(initialEntries));
        }

        stompClient = new Client({
          webSocketFactory: () => new SockJS(`${API_BASE}/ws`),
          reconnectDelay: 5000,
          onConnect: () => {
            if (cancelled) return;
            setConnected(true);
            effectiveDocs.forEach((doc) => {
              stompClient.subscribe(
                `/topic/clinic/${clinicId}/doctor/${doc.doctorId}/queue`,
                (message) => {
                  if (!message.body || cancelled) return;
                  const payload = JSON.parse(message.body);
                  setQueuesByDoctor((prev) => ({ ...prev, [doc.doctorId]: payload }));
                  if (payload.currentQueueNumber) {
                    announceTurn(doc.doctorId, payload.currentQueueNumber, doc.fullName);
                  }
                }
              );
            });
          },
          onDisconnect: () => {
            if (!cancelled) setConnected(false);
          },
        });

        stompClient.activate();
      } catch (err) {
        console.error('TV screen initialization failed:', err);
      }
    };

    init();

    return () => {
      cancelled = true;
      if (stompClient) stompClient.deactivate();
    };
  }, [clinicId, singleDoctorId, announceTurn]);

  const displayedDoctors = tvInfo
    ? singleDoctorId
      ? (tvInfo.doctors || []).filter((d) => Number(d.doctorId) === singleDoctorId)
      : (tvInfo.doctors || [])
    : [];

  const singleDoc = singleDoctorId
    ? displayedDoctors[0] || { doctorId: singleDoctorId, fullName: '', specialtyName: '' }
    : null;

  const isMultiMode = !singleDoctorId;

  return (
    <div className="min-h-screen bg-[#070b18] flex flex-col text-white relative overflow-hidden p-6 md:p-10" dir="rtl">
      {/* الشريط العلوي */}
      <header className="flex flex-wrap items-center justify-between gap-4 pb-6 border-b border-slate-800/80 mb-8">
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg">
            <Activity size={28} />
          </div>
          <div>
            <div className="flex items-center gap-2 text-cyan-400 font-bold text-sm">
              <Building2 size={16} />
              <span>{tvInfo?.organizationName || 'المنصة الطبية'}</span>
            </div>
            <h1 className="text-2xl md:text-4xl font-black text-white">
              صالة انتظار {tvInfo?.clinicName ? `— ${tvInfo.clinicName}` : ''}
            </h1>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 bg-slate-900/80 border border-slate-700/60 px-4 py-2.5 rounded-full text-sm font-bold">
            <span className={`w-2.5 h-2.5 rounded-full live-dot ${connected ? 'bg-emerald-400' : 'bg-red-500'}`} />
            <span className="text-slate-300">{connected ? 'مباشر (Live)' : 'جاري الاتصال...'}</span>
          </div>

          <button
            onClick={toggleSound}
            className={`flex items-center gap-2 px-4 py-2.5 rounded-full text-sm font-bold border transition-all ${
              soundEnabled
                ? 'bg-emerald-500/15 border-emerald-500/40 text-emerald-300'
                : 'bg-slate-900 border-slate-700 text-slate-400'
            }`}
          >
            {soundEnabled ? <Volume2 size={18} /> : <VolumeX size={18} />}
            <span>{soundEnabled ? 'الصوت مفعّل' : 'الصوت صامت'}</span>
          </button>
        </div>
      </header>

      {/* المحتوى */}
      {isMultiMode ? (
        <div className="flex-1 flex flex-col justify-center">
          {displayedDoctors.length === 0 ? (
            <div className="text-center text-slate-400 text-2xl font-bold py-20">
              لا يوجد أطباء مسكنون في هذا الفرع حالياً.
            </div>
          ) : (
            <div
              className={`grid gap-6 ${
                displayedDoctors.length === 1
                  ? 'grid-cols-1 max-w-2xl mx-auto w-full'
                  : displayedDoctors.length === 2
                    ? 'grid-cols-1 md:grid-cols-2'
                    : 'grid-cols-1 md:grid-cols-2 xl:grid-cols-3'
              }`}
            >
              {displayedDoctors.map((doc) => {
                const q = queuesByDoctor[doc.doctorId] || {};
                const isHighlighted = highlightedDoctorId === doc.doctorId;
                return (
                  <div
                    key={doc.doctorId}
                    className={`rounded-[2.25rem] p-8 flex flex-col justify-between transition-all duration-500 border ${
                      isHighlighted
                        ? 'bg-indigo-950/90 border-cyan-400 shadow-[0_0_50px_rgba(34,211,238,0.35)] scale-[1.02]'
                        : 'bg-slate-900/75 border-slate-700/60 shadow-2xl'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-3 mb-6">
                      <div>
                        <h2 className="text-2xl md:text-3xl font-black text-white flex items-center gap-2">
                          <Stethoscope className="text-cyan-400 shrink-0" size={24} />
                          <span>{doc.fullName}</span>
                        </h2>
                        {doc.specialtyName && (
                          <span className="inline-block mt-2 px-3.5 py-1 rounded-full bg-cyan-500/15 text-cyan-300 text-sm font-bold">
                            {doc.specialtyName}
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="text-center py-6 bg-slate-950/50 rounded-3xl border border-slate-800/70 mb-6">
                      <p className="text-lg text-cyan-400 font-bold mb-2">الرقم الحالي</p>
                      {q.currentQueueNumber ? (
                        <p
                          key={`${doc.doctorId}-${q.currentQueueNumber}`}
                          className="text-8xl md:text-9xl leading-none font-black text-white animate-number-pop"
                        >
                          {q.currentQueueNumber}
                        </p>
                      ) : (
                        <p className="text-8xl leading-none font-black text-slate-700">-</p>
                      )}
                    </div>

                    <div className="flex items-center justify-between bg-slate-950/40 px-5 py-3.5 rounded-2xl border border-slate-800/60">
                      <span className="text-slate-300 font-bold flex items-center gap-2">
                        <Users size={18} className="text-amber-400" />
                        المنتظرون
                      </span>
                      <span className="text-3xl font-black text-amber-400">{q.waitingCount ?? 0}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      ) : (
        <div className="flex-1 flex flex-col items-center justify-center">
          {singleDoc?.fullName && (
            <div className="mb-8 text-center">
              <div className="inline-flex items-center gap-3 bg-slate-900/90 border border-slate-700/70 px-8 py-3.5 rounded-full shadow-xl">
                <Stethoscope size={24} className="text-cyan-400" />
                <span className="text-2xl md:text-3xl font-black text-white">{singleDoc.fullName}</span>
                {singleDoc.specialtyName && (
                  <span className="bg-cyan-500/20 text-cyan-300 px-3.5 py-1 rounded-full text-base font-bold">
                    {singleDoc.specialtyName}
                  </span>
                )}
              </div>
            </div>
          )}

          <div
            className={`bg-slate-900/75 border rounded-[3rem] px-20 py-14 text-center shadow-2xl mb-10 transition-all duration-500 ${
              highlightedDoctorId === singleDoc?.doctorId
                ? 'border-cyan-400 shadow-[0_0_70px_rgba(34,211,238,0.4)]'
                : 'border-slate-700/60'
            }`}
          >
            <p className="text-3xl mb-6 text-cyan-400 font-bold">الرقم الحالي</p>
            {queuesByDoctor[singleDoc?.doctorId]?.currentQueueNumber ? (
              <p
                key={queuesByDoctor[singleDoc?.doctorId]?.currentQueueNumber}
                className="text-[180px] md:text-[220px] leading-none font-black text-white animate-number-pop"
              >
                {queuesByDoctor[singleDoc?.doctorId]?.currentQueueNumber}
              </p>
            ) : (
              <p className="text-[160px] leading-none font-black text-slate-700">-</p>
            )}
          </div>

          <div className="bg-slate-900/75 border border-slate-700/60 px-14 py-6 rounded-full shadow-2xl">
            <p className="text-2xl md:text-3xl text-slate-300 font-bold flex items-center gap-3">
              <span className="w-3 h-3 rounded-full bg-emerald-400 live-dot" />
              عدد المنتظرين:
              <span className="text-white font-black text-4xl">
                {queuesByDoctor[singleDoc?.doctorId]?.waitingCount ?? 0}
              </span>
            </p>
          </div>
        </div>
      )}
    </div>
  );
}