import { Client } from '@stomp/stompjs';
import { getStoredToken } from '@/lib/api-client';

const WS_BASE_URL = process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:8080/ws-evault';

let stompClient: Client | null = null;

/**
 * Returns the singleton STOMP client instance.
 * Creates a new one if none exists, configured with JWT auth.
 */
export function getStompClient(): Client {
  if (stompClient) return stompClient;

  const token = getStoredToken();

  stompClient = new Client({
    brokerURL: `${WS_BASE_URL}${token ? `?token=${token}` : ''}`,
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    // Graceful error handling — don't crash the UI if the socket drops
    onStompError: (frame) => {
      console.error('[STOMP] Broker error:', frame.headers['message'], frame.body);
    },
    onWebSocketError: (event) => {
      console.warn('[STOMP] WebSocket error — will attempt reconnect:', event);
    },
    onDisconnect: () => {
      console.info('[STOMP] Disconnected from E-Vault broker');
    },
  });

  return stompClient;
}

/**
 * Activates the STOMP client connection.
 * Safe to call multiple times — only activates if not already active.
 */
export function activateClient(): void {
  const client = getStompClient();
  if (!client.active) {
    // Refresh token before connecting
    const token = getStoredToken();
    if (token) {
      client.brokerURL = `${WS_BASE_URL}?token=${token}`;
      client.connectHeaders = { Authorization: `Bearer ${token}` };
    }
    client.activate();
  }
}

/**
 * Deactivates the STOMP client and clears the singleton.
 */
export function deactivateClient(): void {
  if (stompClient) {
    stompClient.deactivate();
    stompClient = null;
  }
}
