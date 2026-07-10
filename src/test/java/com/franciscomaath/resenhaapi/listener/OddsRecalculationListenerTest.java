package com.franciscomaath.resenhaapi.listener;

import com.franciscomaath.resenhaapi.controller.dto.response.EventResponseDTO;
import com.franciscomaath.resenhaapi.domain.event.EventChangeEvent;
import com.franciscomaath.resenhaapi.service.OddsUpdateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OddsRecalculationListenerTest {

    @Mock
    private OddsUpdateService oddsUpdateService;

    @InjectMocks
    private OddsRecalculationListener listener;

    @Test
    void onOddsRecalculation_delegatesToOddsUpdateService() {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setTournamentId(99L);
        EventChangeEvent event = new EventChangeEvent(this, 1L, dto);

        listener.onOddsRecalculation(event);

        verify(oddsUpdateService).recalculateFutureOdds(99L);
    }
}
