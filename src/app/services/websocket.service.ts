import { Injectable, inject } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';

import { API_BASE_URL } from './api/api-base-url';

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private readonly apiBaseUrl = inject(API_BASE_URL) as string;

  private client: Client | null = null;
  private subjects = new Map<string, Subject<any>>();
  private pendingSubscriptions = new Map<string, (message: IMessage) => void>();

  connect(): void {
    if (this.client && this.client.active) {
      console.log('[WebSocket] Already active, skipping connect');
      return;
    }

    const wsUrl = this.deriveWebSocketUrl(this.apiBaseUrl);
    console.log('[WebSocket] Connecting to:', wsUrl);

    this.client = new Client({
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      webSocketFactory: () => {
        console.log('[WebSocket] Creating SockJS instance for:', wsUrl);
        try {
          const sock = new SockJS(wsUrl);
          console.log('[WebSocket] SockJS instance created:', sock);
          return sock;
        } catch (e) {
          console.error('[WebSocket] Failed to create SockJS:', e);
          throw e;
        }
      },
      onConnect: (frame) => {
        console.log('[WebSocket] STOMP connected. Frame:', frame);
        console.log('[WebSocket] Pending subscriptions:', this.pendingSubscriptions.size);
        this.pendingSubscriptions.forEach((callback, topic) => {
          console.log('[WebSocket] Subscribing to pending topic:', topic);
          this.client!.subscribe(topic, callback);
        });
        this.pendingSubscriptions.clear();
      },
      onDisconnect: (frame) => {
        console.log('[WebSocket] STOMP disconnected. Frame:', frame);
      },
      onStompError: (frame) => {
        console.error('[WebSocket] STOMP error:', frame.headers['message'], frame.body);
      },
      onWebSocketClose: (event) => {
        console.log('[WebSocket] WebSocket closed. Code:', event.code, 'Reason:', event.reason);
      },
      onWebSocketError: (event) => {
        console.error('[WebSocket] WebSocket error:', event);
      },
      onChangeState: (state) => {
        console.log('[WebSocket] State changed:', state);
      },
      debug: (str) => {
        console.log('[STOMP debug]', str);
      },
    });

    console.log('[WebSocket] Activating client...');
    this.client.activate();
    console.log('[WebSocket] Client activated (async)');
  }

  subscribe<T>(topic: string): Observable<T> {
    console.log('[WebSocket] subscribe() called for topic:', topic, 'client?.connected:', this.client?.connected);
    if (!this.subjects.has(topic)) {
      this.subjects.set(topic, new Subject<T>());
    }

    const subject = this.subjects.get(topic)!;
    const callback = (message: IMessage) => {
      console.log('[WebSocket] Message received on topic:', topic, 'body:', message.body);
      try {
        const body = JSON.parse(message.body) as T;
        subject.next(body);
      } catch {
        console.warn('Failed to parse WebSocket message', message.body);
      }
    };

    if (this.client?.connected) {
      console.log('[WebSocket] Client already connected, subscribing immediately');
      this.client.subscribe(topic, callback);
    } else {
      console.log('[WebSocket] Client not connected yet, queuing subscription');
      this.pendingSubscriptions.set(topic, callback);
    }

    return subject.asObservable();
  }

  disconnect(): void {
    console.log('[WebSocket] disconnect() called');
    this.subjects.forEach((subject) => subject.complete());
    this.subjects.clear();
    this.pendingSubscriptions.clear();
    this.client?.deactivate();
    this.client = null;
    console.log('[WebSocket] disconnect() complete');
  }

  private deriveWebSocketUrl(baseUrl: string): string {
    try {
      const url = new URL(baseUrl);
      // Replace /api/v1 with /ws
      const pathname = url.pathname.replace(/\/api\/v1\/?$/, '/ws');
      // If pathname is just /api/v1, replace becomes /ws; handle edge case
      return `${url.protocol}//${url.host}${pathname === '/ws' ? '/ws' : pathname.replace('/api/v1', '/ws')}`;
    } catch {
      // Fallback for non-URL strings (shouldn't happen with normal config)
      const cleaned = baseUrl.replace(/\/api\/v1\/?$/, '');
      return `${cleaned}/ws`;
    }
  }
}
