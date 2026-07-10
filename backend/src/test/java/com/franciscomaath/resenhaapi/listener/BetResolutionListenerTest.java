package com.franciscomaath.resenhaapi.listener;

import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.domain.event.EventCompletedEvent;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.service.BetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetResolutionListenerTest {

    @Mock
    private BetService betService;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private BetResolutionListener listener;

    @Test
    void onEventCompleted_whenStatusCompleted_resolvesBets() {
        EventCompletedEvent event = new EventCompletedEvent(this, 1L, EventStatus.COMPLETED);

        listener.onEventCompleted(event);

        verify(betService).resolveBetsForEvent(1L);
        verify(betService, never()).cancelBetsForEvent(any());
    }

    @Test
    void onEventCompleted_whenStatusCancelled_cancelsBets() {
        EventCompletedEvent event = new EventCompletedEvent(this, 1L, EventStatus.CANCELLED);

        listener.onEventCompleted(event);

        verify(betService).cancelBetsForEvent(1L);
        verify(betService, never()).resolveBetsForEvent(any());
    }

    @Test
    void onEventCompleted_whenStatusCreated_doesNothing() {
        EventCompletedEvent event = new EventCompletedEvent(this, 1L, EventStatus.CREATED);

        listener.onEventCompleted(event);

        verify(betService, never()).resolveBetsForEvent(any());
        verify(betService, never()).cancelBetsForEvent(any());
    }
}
