import { useCallback, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import { IngestionProgressEvent } from '../types';

export function useIngestionJobProgress() {
  const clientRef = useRef<Client | null>(null);

  const subscribe = useCallback((jobId: string, onEvent: (event: IngestionProgressEvent) => void) => {
    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }

    let wsURL = import.meta.env.VITE_WS_URL;
    if (!wsURL) {
      const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
      const isDev = window.location.port && window.location.port !== '80' && window.location.port !== '443';
      wsURL = isDev
        ? `${proto}://${window.location.hostname}:8080/ws`
        : `${proto}://${window.location.host}/ws`;
    }

    const client = new Client({
      brokerURL: wsURL,
      reconnectDelay: 2000,
      onConnect: () => {
        client.subscribe(`/topic/ingestion/jobs/${jobId}`, (frame) => {
          try {
            onEvent(JSON.parse(frame.body) as IngestionProgressEvent);
          } catch {
            // ignore malformed payload
          }
        });
      },
    });

    client.activate();
    clientRef.current = client;
  }, []);

  const unsubscribe = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }
  }, []);

  return { subscribe, unsubscribe };
}
