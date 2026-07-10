package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.controller.dto.request.*;
import com.franciscomaath.resenhaapi.controller.dto.response.GroupMemberResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.GroupResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.PlayerResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.PlayerStatsResponseDTO;

import java.util.List;

public interface GroupService {
    GroupResponseDTO create(GroupRequestDTO dto);

    List<GroupResponseDTO> listMine();

    List<GroupMemberResponseDTO> listMembers(Long groupId);

    List<PlayerResponseDTO> listAvaliablePlayers(Long groupId);

    GroupResponseDTO addMember(Long groupId, GroupMemberRequestDTO dto);

    GroupMemberResponseDTO claimPlayer(Long groupId, GroupClaimPlayerRequestDTO dto);

    GroupResponseDTO switchGroup(Long groupId);

    List<PlayerResponseDTO> recalculateElo(Long groupId);

    GroupResponseDTO editGroup(Long groupId, GroupPatchRequestDTO dto);

    void softDeleteGroup(Long groupId);

    void removeGroupMember(Long groupId, Long userId);

    GroupMemberResponseDTO changeMemberRole(Long groupId, Long userId, GroupMemberRolePatchRequestDTO dto);

    GroupResponseDTO join(GroupJoinRequestDTO dto);

    List<PlayerStatsResponseDTO> getRanking(Long groupId);
}
