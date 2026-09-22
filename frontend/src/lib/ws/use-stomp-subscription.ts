'use client';

import { useEffect, useRef } from 'react';
import { getStompClient, activateClient } from './stomp-client';
import type { IMessage } from '@stomp/stompjs';

/**
 * React hook to subscribe to a STOMP topic.
 * Handles client activation, subscription on mount, and cleanup on unmount.
 * Gracefully handles cases where the client isn't yet connected by
 * deferring the subscription to the onConnect callback.
 */
export function useStompSubscription(
  topic: string | null,
  onMessage: (message: IMessage) => void,
  enabled: boolean = true
): void {
  const onMessageRef = useRef(onMessage);
  onMessageRef.current = onMessage;

  useEffect(() => {
    if (!topic || !enabled) return;

    const client = getStompClient();
    let subId: { unsubscribe: () => void } | null = null;

    const subscribe = () => {
      try {
        subId = client.subscribe(topic, (msg) => {
          onMessageRef.current(msg);
        });
      } catch (err) {
        console.warn(`[STOMP] Failed to subscribe to ${topic}:`, err);
      }
    };

    if (client.connected) {
      subscribe();
    } else {
      // Store previous onConnect to chain
      const prevOnConnect = client.onConnect;
      client.onConnect = (frame) => {
        prevOnConnect?.(frame);
        subscribe();
      };
      activateClient();
    }

    return () => {
      try {
        subId?.unsubscribe();
      } catch {
        // Client may already be disconnected
      }
    };
  }, [topic, enabled]);
}
