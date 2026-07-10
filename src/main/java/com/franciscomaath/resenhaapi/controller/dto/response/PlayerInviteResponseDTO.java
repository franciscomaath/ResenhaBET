package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerInviteResponseDTO {
    private String inviteToken;
    private String inviteUrl;
}
