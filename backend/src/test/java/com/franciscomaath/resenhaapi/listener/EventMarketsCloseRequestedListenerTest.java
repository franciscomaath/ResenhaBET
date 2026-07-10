package com.franciscomaath.resenhaapi.listener;

import com.franciscomaath.resenhaapi.domain.event.EventMarketsCloseRequestedEvent;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.service.MarketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventMarketsCloseRequestedListenerTest {

    @Mock
    private MarketService marketService;

    @InjectMocks
    private EventMarketsCloseRequestedListener listener;

    @Test
    void onEventMarketsCloseRequested_closesMarket() {
        EventMarketsCloseRequestedEvent event = new EventMarketsCloseRequestedEvent(this, 1L);

        listener.onEventMarketsCloseRequested(event);

        verify(marketService).closeMarket(1L);
    }

    @Test
    void onEventMarketsCloseRequested_whenMarketNotFound_doesNotThrow() {
        EventMarketsCloseRequestedEvent event = new EventMarketsCloseRequestedEvent(this, 1L);
        doThrow(new ResourceNotFoundException("Market not found for event id: 1"))
                .when(marketService).closeMarket(1L);

        listener.onEventMarketsCloseRequested(event);

        verify(marketService).closeMarket(1L);
    }
}
