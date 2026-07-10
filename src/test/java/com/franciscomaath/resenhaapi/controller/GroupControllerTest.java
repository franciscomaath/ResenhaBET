package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.controller.dto.response.GroupResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.GroupMemberResponseDTO;
import com.franciscomaath.resenhaapi.controller.exception.GlobalExceptionHandler;
import com.franciscomaath.resenhaapi.domain.enums.GroupRole;
import com.franciscomaath.resenhaapi.service.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

    @Mock
    private GroupService groupService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new GroupController(groupService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_shouldReturnCreatedGroup() throws Exception {
        when(groupService.create(any())).thenReturn(response(10L, "Copa Gege", GroupRole.OWNER));

        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Copa Gege\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Copa Gege"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void listMine_shouldReturnGroups() throws Exception {
        when(groupService.listMine()).thenReturn(List.of(response(10L, "Copa Gege", GroupRole.OWNER)));

        mockMvc.perform(get("/api/v1/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    @Test
    void addMember_shouldDelegateToService() throws Exception {
        when(groupService.addMember(org.mockito.Mockito.eq(10L), any()))
                .thenReturn(response(10L, "Copa Gege", GroupRole.MEMBER));

        mockMvc.perform(post("/api/v1/groups/10/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2,\"role\":\"MEMBER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"));

        verify(groupService).addMember(org.mockito.Mockito.eq(10L), any());
    }

    @Test
    void listMembers_shouldReturnGroupMembers() throws Exception {
        GroupMemberResponseDTO member = new GroupMemberResponseDTO();
        member.setUserId(2L);
        member.setUserName("Member");
        member.setRole(GroupRole.MEMBER);
        when(groupService.listMembers(10L)).thenReturn(List.of(member));

        mockMvc.perform(get("/api/v1/groups/10/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].userName").value("Member"))
                .andExpect(jsonPath("$[0].role").value("MEMBER"));

        verify(groupService).listMembers(10L);
    }

    @Test
    void switchGroup_shouldDelegateToService() throws Exception {
        when(groupService.switchGroup(10L)).thenReturn(response(10L, "Copa Gege", GroupRole.ADMIN));

        mockMvc.perform(post("/api/v1/groups/10/switch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void recalculateElo_shouldDelegateToService() throws Exception {
        when(groupService.recalculateElo(10L)).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/groups/10/recalculate-elo"))
                .andExpect(status().isOk());

        verify(groupService).recalculateElo(10L);
    }

    private GroupResponseDTO response(Long id, String name, GroupRole role) {
        GroupResponseDTO response = new GroupResponseDTO();
        response.setId(id);
        response.setName(name);
        response.setRole(role);
        return response;
    }
}
