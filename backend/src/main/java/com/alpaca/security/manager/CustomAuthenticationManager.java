package com.alpaca.security.manager;

import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.service.IAuthService;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CustomAuthenticationManager implements AuthenticationProvider {

    private final IAuthService userDetailsService;
    private final PasswordManager passwordManager;

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        String username = authentication.getName();
        String password = Objects.requireNonNull(authentication.getCredentials()).toString();

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        validateUserDetails(password, userDetails);

        return new UsernamePasswordAuthenticationToken(
                userDetails, password, userDetails.getAuthorities());
    }

    @Override
    public boolean supports(@NonNull Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * Validates raw password against stored user details and enforces account status checks.
     *
     * @param rawPassword the entered password
     * @param userDetails stored user details
     * @throws BadRequestException if validation fails
     * @throws UnauthorizedException if account is disabled or locked
     */
    public void validateUserDetails(String rawPassword, UserDetails userDetails) {
        if (userDetails == null) {
            throw new BadRequestException("Invalid Username or Password");
        }
        if (rawPassword == null
                || rawPassword.isBlank()
                || !passwordManager.matches(rawPassword, userDetails.getPassword())) {
            throw new BadRequestException("Invalid Password");
        }
        if (!(userDetails.isEnabled()
                && userDetails.isAccountNonLocked()
                && userDetails.isAccountNonExpired()
                && userDetails.isCredentialsNonExpired())) {
            throw new UnauthorizedException("The account has been deactivated or blocked");
        }
    }
}
