package com.franciscomaath.resenhaapi.mapper;

import com.franciscomaath.resenhaapi.controller.dto.response.GroupMemberResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.GroupResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Group;
import com.franciscomaath.resenhaapi.domain.entity.GroupMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GroupMapper {

    @Mapping(target = "id", source = "group.id")
    @Mapping(target = "name", source = "group.name")
    @Mapping(target = "groupCode", source = "group.groupCode")
    GroupResponseDTO toResponse(Group group);

    @Mapping(target = "id", source = "group.id")
    @Mapping(target = "name", source = "group.name")
    @Mapping(target = "groupCode", source = "group.groupCode")
    @Mapping(target = "playerClaimed", source = "playerClaimed")
    GroupResponseDTO toResponse(GroupMember member);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.name")
    @Mapping(target = "playerClaimed", source = "member.playerClaimed")
    GroupMemberResponseDTO toMemberResponse(GroupMember member);
}
