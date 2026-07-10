package com.franciscomaath.resenhaapi.config;

import com.franciscomaath.resenhaapi.domain.entity.Player;
import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.entity.Group;
import com.franciscomaath.resenhaapi.domain.entity.GroupMember;
import com.franciscomaath.resenhaapi.domain.enums.GroupRole;
import com.franciscomaath.resenhaapi.domain.enums.UserType;
import com.franciscomaath.resenhaapi.domain.repository.GroupMemberRepository;
import com.franciscomaath.resenhaapi.domain.repository.GroupRepository;
import com.franciscomaath.resenhaapi.domain.repository.PlayerRepository;
import com.franciscomaath.resenhaapi.domain.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PlayerInitializer implements CommandLineRunner {

    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Value("${resenhabet.admin.name:Francisco}")
    private String adminName;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByUserType(UserType.ADMIN)) {
            return;
        }

        User admin = userRepository.findByName(adminName)
                .orElseGet(() -> User.builder()
                        .name(adminName)
                        .firstLogin(true)
                        .pinHash(null)
                        .salt(null)
                        .build());
        admin.setUserType(UserType.ADMIN);

        admin = userRepository.save(admin);
        Group group = groupRepository.findByName("ResenhaBET")
                .orElseGet(() -> groupRepository.save(Group.builder().name("ResenhaBET").groupCode("676767").active(true).build()));

        if (!groupMemberRepository.existsByGroupIdAndUserId(group.getId(), admin.getId())) {
            groupMemberRepository.save(GroupMember.builder()
                    .group(group)
                    .user(admin)
                    .role(GroupRole.OWNER)
                    .build());
        }

        if (!playerRepository.existsByGroupIdAndUserId(group.getId(), admin.getId())) {
            Player player = Player.builder()
                    .name(adminName)
                    .active(true)
                    .user(admin)
                    .group(group)
                    .build();
            playerRepository.save(player);
        }
    }
}
