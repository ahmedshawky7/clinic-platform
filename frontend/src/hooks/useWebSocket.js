import { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axiosClient from '../api/axiosClient';

export const useWebSocket = (clinicId, doctorId) => {
  const [queueData, setQueueData] = useState(null);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    if (!clinicId || !doctorId) {
      setQueueData(null);
      setConnected(false);
      return;
    }

    const fetchInitialState = async () => {
      try {
        const response = await axiosClient.get(`/queues/${clinicId}?doctorId=${doctorId}`);
        setQueueData(response.data);
      } catch (error) {
        console.error('Failed to fetch initial queue state:', error);
      }
    };

    fetchInitialState();

    const wsBase = import.meta.env.VITE_API_URL || 'http://localhost:8080';
    const client = new Client({
      webSocketFactory: () => new SockJS(`${wsBase}/ws`),
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/clinic/${clinicId}/doctor/${doctorId}/queue`, (message) => {
          if (message.body) {
            setQueueData(JSON.parse(message.body));
          }
        });
      },
      onDisconnect: () => setConnected(false),
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [clinicId, doctorId]);

  return { queueData, connected };
};