package com.alpaca.resources;

import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import com.alpaca.model.UserPrincipal;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class CustomUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    @NonNull
    public SecurityContext createSecurityContext(@NonNull WithMockCustomUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        User user = UserProvider.singleEntity();
        List<Role> roles = RoleProvider.listEntities();
        user.setRoles(roles);
        UserPrincipal principal = new UserPrincipal(user);

        Authentication auth =
                new UsernamePasswordAuthenticationToken(
                        principal, "password", user.getAuthorities());

        context.setAuthentication(auth);
        return context;
    }
}
