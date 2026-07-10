# ResenhaBET v2 — WebSocket & Live Match System Briefing

## Purpose

This document specifies the complete WebSocket implementation for ResenhaBET v2,
covering backend configuration, broadcast events, frontend integration, and the
"Live Now" modal on the Dashboard. It is intended for an AI agent implementing
this feature from scratch.

---

## Context and Design Decisions

### Two tournament types, one architecture

ResenhaBET supports two usage patterns:

**Long-duration tournaments:** Matches happen over days or weeks. Results are
registered by the admin after the match is played, not necessarily live.
Nobody is watching the Event page in real time waiting for updates.

**Short-duration tournaments:** Matches happen sequentially over a few hours,
everyone is present. Scores are updated live as goals are scored. Real-time
updates matter here.

The same WebSocket architecture serves both cases naturally. The connection
only exists when a user is on the Event page. For long-duration tournaments,
nobody is on that page during the match, so no connections exist and no
resources are used. For short-duration tournaments, everyone is watching live
and the WebSocket delivers instant updates.

### What WebSocket does NOT do in this project

- There is no `/topic/live` or global broadcast topic
- WebSocket does NOT connect at login
- WebSocket is NOT used to power the Dashboard "Live Now" modal
  (that uses a simple HTTP GET instead — see Dashboard section below)
- There are no dynamic odds updates via WebSocket (planned for a future phase)
- There is no cashout feature in this phase

### What WebSocket DOES do

WebSocket handles real-time updates exclusively on the Event page:
- Score updates as the admin changes the scoreboard
- Market status changes (when the event ends and market closes)
- Final event resolution (when finishEvent is called)

---

## Backend Implementation

### Dependencies

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### WebSocketConfig

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                    "http://localhost:4200",
                    "http://127.0.0.1:4200"
                )
                .withSockJS();
    }
}
```

### Topic

There is only one topic pattern in this project:

```
/topic/events/{id}
```

Where `{id}` is the event ID. The frontend subscribes to this topic when it
opens the Event page and unsubscribes when it leaves.

### Broadcast payload

All three broadcast events publish the same payload: the full `EventResponseDTO`
of the event that changed. The frontend does not need to make a separate HTTP
call after receiving a WebSocket message — the message itself contains the
complete updated event state.

**Current `EventResponseDTO` fields (as of this implementation):**

```json
{
  "id": 5,
  "tournamentId": 1,
  "roundId": 2,
  "playerHomeId": 10,
  "playerAwayId": 20,
  "gameDatetime": "2026-07-15T20:00:00",
  "status": "IN_PROGRESS",
  "homeScore": 2,
  "awayScore": 1,
  "isKnockout": false,
  "isBye": false
}
```

> **Note:** The DTO does not yet include `playerHomeName`, `playerAwayName`,
> `homeEloBefore`, or `awayEloBefore`. These fields may be added in a future
> phase by updating `EventMapper`. The WebSocket payload will automatically
> include them once the DTO is extended — no backend WebSocket changes needed.

### Where broadcasts are sent — three trigger points

All three endpoints use **POST** (not PATCH):

| Action | Endpoint | HTTP Method |
|--------|----------|-------------|
| Start event | `/api/v1/events/{id}/start` | POST |
| Update score | `/api/v1/events/{id}/score` | POST |
| Finish event | `/api/v1/events/{id}/end` | POST |

#### 1. startEvent

When the admin calls `POST /api/v1/events/{id}/start`:

```java
// after updating event.status = IN_PROGRESS and market.status = CLOSED
messagingTemplate.convertAndSend(
    "/topic/events/" + event.getId(),
    eventMapper.toResponse(event)
);
```

This tells all clients watching this event that the match has started and
betting is now closed.

#### 2. updateScore

When the admin calls `POST /api/v1/events/{id}/score`:

```java
// after saving new homeScore and awayScore
messagingTemplate.convertAndSend(
    "/topic/events/" + event.getId(),
    eventMapper.toResponse(event)
);
```

This is the main real-time update for short-duration tournaments. Every goal
scored by the admin is immediately reflected on all connected clients.

#### 3. finishEvent

When the admin calls `POST /api/v1/events/{id}/end`, after the full
@Transactional completes (bet resolution + market close + Elo update):

```java
// after all side effects are committed
messagingTemplate.convertAndSend(
    "/topic/events/" + event.getId(),
    eventMapper.toResponse(event)
);
```

This tells all clients that the match is over and bets have been resolved.
The frontend should react by re-fetching the market and the user's bet slip
to show the final result (WON/LOST). The EventResponseDTO alone does not
contain bet resolution details, so a follow-up HTTP call is needed for that.

### Broadcast implementation pattern

Instead of injecting `SimpMessagingTemplate` directly into `EventServiceImpl`,
the implementation uses Spring's event system to guarantee broadcasts happen
**after** the transaction commits:

1. `EventServiceImpl` injects `ApplicationEventPublisher` and publishes an
   `EventChangeEvent` after each mutating method (`startEvent`, `updateScore`,
   `finishEvent`) completes its work inside the `@Transactional` method.

2. `WebSocketBroadcaster` listens for `EventChangeEvent` with
   `@TransactionalEventListener(phase = AFTER_COMMIT)` and calls
   `SimpMessagingTemplate.convertAndSend(...)`.

This ensures that if a transaction rolls back, no stale data is broadcast.

```java
// EventChangeEvent.java — the event payload
public class EventChangeEvent extends ApplicationEvent {
    private final Long eventId;
    private final EventResponseDTO dto;
    // constructor, getters
}

// EventServiceImpl.java — publish inside @Transactional method
EventResponseDTO response = eventMapper.toResponse(event);
eventPublisher.publishEvent(new EventChangeEvent(this, eventId, response));
return response;

// WebSocketBroadcaster.java — send after commit
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onEventChange(EventChangeEvent event) {
    messagingTemplate.convertAndSend(
        "/topic/events/" + event.getEventId(), event.getDto());
}
```

### Broadcast ordering rule

Always send the broadcast AFTER the database transaction commits. Never inside
the @Transactional before commit. If the broadcast is sent inside the transaction
and the transaction rolls back, clients will have received stale or incorrect data.

For `finishEvent`, which has a complex @Transactional, the safest approach is to
return the saved event from the transactional method and broadcast from the caller,
or use Spring's `@TransactionalEventListener(phase = AFTER_COMMIT)`.

---

## Dashboard — "Live Now" Modal

### How it works

The Dashboard shows a modal when there are events currently IN_PROGRESS.
This uses a plain HTTP GET, not WebSocket. The modal appears for all users
regardless of whether they are players or bettors.

### Implementation

On Dashboard component initialization, after the user is authenticated:

```typescript
// on ngOnInit or after auth resolves
this.eventsApi.getLiveEvents().subscribe(events => {
  if (events.length > 0) {
    this.liveEvents.set(events);
    this.showLiveModal.set(true);
  }
});
```

The API call:

```
GET /api/v1/events?status=IN_PROGRESS
```

This endpoint is already implemented. The `GET /api/v1/events` endpoint accepts
optional `?tournamentId=` and `?status=` query parameters (individually or
combined) using Spring Data JPA Specifications.

### Modal behavior

- Appears automatically on Dashboard load if there are live events
- Shows each live event as a card: home player vs away player, current score,
  tournament name, round name
- Each card links to `/events/{id}`
- Dismissible — user can close the modal
- Once dismissed, does NOT reappear during the same session (store dismissed
  state in a component signal, not localStorage — it should recheck on next login)
- If no live events exist on load, the modal does not appear at all
- There is NO automatic re-check or polling — if a match starts after the user
  has already loaded the Dashboard, they will only see it when they navigate
  away and return, or manually refresh

### Why no real-time update for the modal

A WebSocket subscription on the Dashboard to detect when new matches go live
would only be useful if the admin starts a match remotely while users are
already on the Dashboard. In practice:
- Short tournaments: everyone is in the same room, they know a match started
- Long tournaments: results are registered after the fact, not live

The HTTP GET on page load is sufficient. The complexity of a global broadcast
topic is not justified.

---

## Frontend Implementation

### Dependencies

Install the STOMP client:

```bash
npm install @stomp/stompjs
```

### WebSocketService

```typescript
import { Injectable } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { Observable, Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class WebSocketService {

  private client: Client | null = null;
  private subjects = new Map<string, Subject<any>>();

  connect(): void {
    if (this.client?.connected) return;

    this.client = new Client({
      brokerURL: 'ws://localhost:8080/ws/websocket',
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('WebSocket connected');
      },
      onDisconnect: () => {
        console.log('WebSocket disconnected');
      }
    });

    this.client.activate();
  }

  subscribe<T>(topic: string): Observable<T> {
    if (!this.subjects.has(topic)) {
      this.subjects.set(topic, new Subject<T>());
    }

    const subject = this.subjects.get(topic)!;

    this.client?.subscribe(topic, (message: IMessage) => {
      subject.next(JSON.parse(message.body) as T);
    });

    return subject.asObservable();
  }

  disconnect(): void {
    this.subjects.forEach(subject => subject.complete());
    this.subjects.clear();
    this.client?.deactivate();
    this.client = null;
  }
}
```

### Event Page integration

The Event page connects WebSocket on init and disconnects on destroy.
It does NOT connect at login or on any other page.

```typescript
@Component({ ... })
export class EventPageComponent implements OnInit, OnDestroy {

  private webSocketService = inject(WebSocketService);
  private eventsApi = inject(EventsApi);
  private route = inject(ActivatedRoute);

  readonly event = signal<EventResponseDTO | null>(null);
  readonly market = signal<MarketResponseDTO | null>(null);

  ngOnInit(): void {
    const eventId = this.route.snapshot.paramMap.get('id')!;

    // Initial HTTP load
    this.eventsApi.getById(eventId).subscribe(e => this.event.set(e));
    this.marketsApi.getMarket(eventId).subscribe(m => this.market.set(m));

    // WebSocket for real-time updates
    this.webSocketService.connect();
    this.webSocketService
      .subscribe<EventResponseDTO>(`/topic/events/${eventId}`)
      .subscribe(updatedEvent => {
        this.event.set(updatedEvent);

        // If event just completed, re-fetch market and user's bet slip
        // to get resolution details (WON/LOST)
        if (updatedEvent.status === 'COMPLETED') {
          this.marketsApi.getMarket(eventId).subscribe(m => this.market.set(m));
          this.betsApi.getMyBets().subscribe(slips => {
            // find the relevant slip and update local state
          });
        }
      });
  }

  ngOnDestroy(): void {
    this.webSocketService.disconnect();
  }
}
```

### Why re-fetch on COMPLETED

The WebSocket payload is `EventResponseDTO`, which contains the final score and
event status but NOT the bet resolution details (WON/LOST per slip item).
When the frontend receives `status = "COMPLETED"` via WebSocket, it should
trigger additional HTTP calls to get the updated market status and the user's
bet slip results. This is intentional — the WebSocket message is a notification
that something changed, not a complete data transfer.

---

## Connection lifecycle summary

```
User opens /events/{id}
  └── ngOnInit
      ├── GET /api/v1/events/{id}         → initial event state
      ├── GET /api/v1/markets/{eventId}   → initial market + odds
      └── WebSocket.connect()
          └── subscribe /topic/events/{id}
              ├── on startEvent broadcast  → update event signal (status=IN_PROGRESS)
              ├── on updateScore broadcast → update event signal (new scores)
              └── on finishEvent broadcast → update event signal (status=COMPLETED)
                                           → re-fetch market
                                           → re-fetch user bet slips

User leaves /events/{id}
  └── ngOnDestroy
      └── WebSocket.disconnect()
```

---

## State transitions the frontend must handle

| Received event.status | Received market.status | What frontend should show |
|---|---|---|
| `CREATED` | `OPEN` | Score 0-0, bet panel visible, admin can start |
| `IN_PROGRESS` | `CLOSED` | Live score updating, bet panel hidden ("Betting closed"), admin can update score and end match |
| `COMPLETED` | `CLOSED` | Final scoreboard, no admin controls, re-fetch bet slips to show WON/LOST |

The frontend should never assume market status from event status alone.
Always read both fields independently.

---

## Backend tasks checklist

- [x] Add `spring-boot-starter-websocket` to `pom.xml`
- [x] Create `WebSocketConfig` with `/ws` endpoint and `/topic` broker
- [x] Create `EventChangeEvent` (ApplicationEvent) and `WebSocketBroadcaster` (`@TransactionalEventListener`)
- [x] Inject `ApplicationEventPublisher` in `EventServiceImpl`
- [x] Add broadcast after `startEvent` completes
- [x] Add broadcast after `updateScore` saves
- [x] Add broadcast after `finishEvent` (fires after `@Transactional` commit via `AFTER_COMMIT` phase)
- [x] Add `?status=` filter to `GET /api/v1/events` using Spring Data JPA Specifications

## Frontend tasks checklist

- [ ] Install `@stomp/stompjs`
- [ ] Create `WebSocketService` (connect, subscribe, disconnect)
- [ ] On Event page `ngOnInit`: connect WebSocket, subscribe to `/topic/events/{id}`
- [ ] On Event page `ngOnDestroy`: disconnect WebSocket
- [ ] Handle `startEvent` broadcast: update event signal, hide bet panel
- [ ] Handle `updateScore` broadcast: update score display
- [ ] Handle `finishEvent` broadcast: show final state, re-fetch market and bet slips
- [ ] On Dashboard `ngOnInit`: `GET /api/v1/events?status=IN_PROGRESS`
- [ ] Show "Live Now" modal if live events exist
- [ ] Each live event card links to `/events/{id}`
- [ ] Modal is dismissible, does not reappear in the same session
- [x] Verify `GET /api/v1/events?status=IN_PROGRESS` filter exists in backend

---

## What is explicitly out of scope for this phase

- Dynamic odds via WebSocket (future phase, requires `outcome_odd_history` table)
- Cashout feature (depends on dynamic odds)
- Global `/topic/live` broadcast topic
- WebSocket connection at login
- Real-time Dashboard updates when a new match goes live
- MarketScheduler for automatic market suspension by time
  (deferred to a future group — currently market closes when admin starts event)
