package com.franciscomaath.resenhaapi.config;

import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.entity.Group;
import com.franciscomaath.resenhaapi.domain.enums.UserType;
import com.franciscomaath.resenhaapi.domain.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUserContext {

    private final ThreadLocal<User> currentUser = new ThreadLocal<>();
    private final ThreadLocal<Group> currentGroup = new ThreadLocal<>();
    private final ThreadLocal<UUID> currentToken = new ThreadLocal<>();

    public void set(User user, Group group, UUID token) {
        currentUser.set(user);
        currentGroup.set(group);
        currentToken.set(token);
    }

    public User getRequiredUser() {
        User user = currentUser.get();
        if (user == null) {
            throw new UnauthorizedException("Usuario nao autenticado.");
        }
        return user;
    }

    public UUID getRequiredToken() {
        UUID token = currentToken.get();
        if (token == null) {
            throw new UnauthorizedException("Sessao nao autenticada.");
        }
        return token;
    }

    public Group getRequiredGroup() {
        Group group = currentGroup.get();
        if (group == null) {
            throw new UnauthorizedException("Grupo ativo nao selecionado.");
        }
        return group;
    }

    public Long getRequiredGroupId() {
        return getRequiredGroup().getId();
    }

    public void requireAdmin() {
        User user = getRequiredUser();
        if (user.getUserType() != UserType.ADMIN) {
            throw new UnauthorizedException("Acesso restrito ao administrador.");
        }
    }

    public void clear() {
        currentUser.remove();
        currentGroup.remove();
        currentToken.remove();
    }
}
