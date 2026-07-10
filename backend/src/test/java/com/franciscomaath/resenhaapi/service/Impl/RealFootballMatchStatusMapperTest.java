package com.franciscomaath.resenhaapi.service.Impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RealFootballMatchStatusMapperTest {

    private final RealFootballMatchStatusMapper mapper = new RealFootballMatchStatusMapper();

    @Test
    void map_whenBlank_returnsNotStarted() {
        assertEquals(RealFootballMatchState.NOT_STARTED, mapper.map(null));
        assertEquals(RealFootballMatchState.NOT_STARTED, mapper.map(""));
    }

    @Test
    void map_whenLiveStatus_returnsLive() {
        assertEquals(RealFootballMatchState.LIVE, mapper.map("45'"));
        assertEquals(RealFootballMatchState.LIVE, mapper.map("90+3'"));
        assertEquals(RealFootballMatchState.LIVE, mapper.map("59"));
        assertEquals(RealFootballMatchState.LIVE, mapper.map("Half Time"));
        assertEquals(RealFootballMatchState.LIVE, mapper.map("HT"));
        assertEquals(RealFootballMatchState.LIVE, mapper.map("1st Half"));
        assertEquals(RealFootballMatchState.LIVE, mapper.map("2nd Half"));
        assertEquals(RealFootballMatchState.LIVE, mapper.map("Break"));
    }

    @Test
    void map_whenFinishedStatus_returnsFinished() {
        assertEquals(RealFootballMatchState.FINISHED, mapper.map("Finished"));
        assertEquals(RealFootballMatchState.FINISHED, mapper.map("After ET"));
        assertEquals(RealFootballMatchState.FINISHED, mapper.map("After Pen."));
    }

    @Test
    void map_whenCancelledStatus_returnsCancelled() {
        assertEquals(RealFootballMatchState.CANCELLED, mapper.map("Cancelled"));
        assertEquals(RealFootballMatchState.CANCELLED, mapper.map("Postponed"));
        assertEquals(RealFootballMatchState.CANCELLED, mapper.map("Abandoned"));
    }
}
