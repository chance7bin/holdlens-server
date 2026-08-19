package com.echoamoy.holdlens.server.trigger.http.auth;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUserContext {

    private CurrentUserContext() {
    }

    public static CurrentUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CurrentUser currentUser)
                || currentUser.userId() == null || currentUser.userId() <= 0) {
            throw new AccessDeniedException("缺少可信用户身份");
        }
        return currentUser;
    }

    public static Long requireMatchingUserId(Long requestedUserId) {
        CurrentUser currentUser = requireCurrentUser();
        if (requestedUserId != null && !currentUser.userId().equals(requestedUserId)) {
            throw new AccessDeniedException("请求用户与当前身份不一致");
        }
        return currentUser.userId();
    }
}
