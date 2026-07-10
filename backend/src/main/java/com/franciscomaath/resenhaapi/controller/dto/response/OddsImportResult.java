package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OddsImportResult {

    private int marketsCreated;
    private int marketsUpdated;
    private int outcomesCreated;
    private int oddsUpdated;

}
