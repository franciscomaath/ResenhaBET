package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.controller.dto.request.GroupClaimPlayerRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.GroupMemberRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.GroupRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.GroupPatchRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.GroupMemberRolePatchRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.GroupMemberResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.GroupResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.PlayerResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.PlayerStatsResponseDTO;
import com.franciscomaath.resenhaapi.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponseDTO> create(@RequestBody @Valid GroupRequestDTO dto) {
        return ResponseEntity.ok(groupService.create(dto));
    }

    @PostMapping("/join")
    public ResponseEntity<GroupResponseDTO> join(@RequestBody @Valid com.franciscomaath.resenhaapi.controller.dto.request.GroupJoinRequestDTO dto) {
        return ResponseEntity.ok(groupService.join(dto));
    }

    @GetMapping
    public ResponseEntity<List<GroupResponseDTO>> listMine() {
        return ResponseEntity.ok(groupService.listMine());
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<GroupMemberResponseDTO>> listMembers(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.listMembers(id));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<GroupResponseDTO> addMember(
            @PathVariable Long id,
            @RequestBody @Valid GroupMemberRequestDTO dto
    ) {
        return ResponseEntity.ok(groupService.addMember(id, dto));
    }

    @PostMapping("/{id}/claim-player")
    public ResponseEntity<GroupMemberResponseDTO> claimPlayer(
            @PathVariable Long id,
            @RequestBody @Valid GroupClaimPlayerRequestDTO dto
            ) {
        return ResponseEntity.ok(groupService.claimPlayer(id, dto));
    }

    @GetMapping("/{id}/players/available")
    @Operation(summary = "List group players available for claiming", description = "List players in the group available for claiming.")
    public ResponseEntity<List<PlayerResponseDTO>> getAvailablePlayers(
            @PathVariable Long id
    ){
        return ResponseEntity.ok(groupService.listAvaliablePlayers(id));
    }

    @PostMapping("/{id}/switch")
    public ResponseEntity<GroupResponseDTO> switchGroup(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.switchGroup(id));
    }

    @PostMapping("/{id}/recalculate-elo")
    @Operation(summary = "Recalculate group Elo", description = "Recalculates Elo for all players in the group using their full history. Admin only.")
    public ResponseEntity<List<PlayerResponseDTO>> recalculateElo(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.recalculateElo(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edit group", description = "Edits group name. Only group OWNER or ADMIN.")
    public ResponseEntity<GroupResponseDTO> editGroup(
            @PathVariable Long id,
            @RequestBody @Valid GroupPatchRequestDTO dto
    ) {
        return ResponseEntity.ok(groupService.editGroup(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete group", description = "Soft deletes a group. Only group OWNER.")
    public ResponseEntity<Void> softDeleteGroup(@PathVariable Long id) {
        groupService.softDeleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove group member", description = "Soft deletes a group member. Admin/Owner logic applies.")
    public ResponseEntity<Void> removeGroupMember(
            @PathVariable Long id,
            @PathVariable Long userId
    ) {
        groupService.removeGroupMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/members/{userId}/role")
    @Operation(summary = "Change group member role", description = "Promote or demote a group member. Owner logic applies.")
    public ResponseEntity<GroupMemberResponseDTO> changeMemberRole(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestBody @Valid GroupMemberRolePatchRequestDTO dto
    ) {
        return ResponseEntity.ok(groupService.changeMemberRole(id, userId, dto));
    }

    @GetMapping("/{id}/ranking")
    @Operation(summary = "Get group ranking", description = "Get ranking for the group based on player elo, goals, and tournaments won.")
    public ResponseEntity<List<PlayerStatsResponseDTO>> getRanking(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.getRanking(id));
    }
}
