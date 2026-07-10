package com.franciscomaath.resenhaapi.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SyncResult {
    private int eventsCreated;
    private int eventsUpdated;
    private int teamsLinked;
    private int roundsCreated;
    private int marketsCreated;
    private int oddsImported;
}