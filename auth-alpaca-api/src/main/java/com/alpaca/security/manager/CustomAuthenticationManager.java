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

/**
 * An {@link AuthenticationProvider} that validates username/password credentials against the
 * application's user store.
 *
 * <p>This provider handles {@link UsernamePasswordAuthenticationToken} requests by:
 *
 * <ul>
 *   <li>Loading the {@link UserDetails} via {@link IAuthService#loadUserByUsername(String)}.
 *   <li>Verifying the raw password against the stored hash using {@link PasswordManager}.
 *   <li>Validating account status (enabled, non-locked, non-expired, credentials non-expired).
 *   <li>Returning a fully authenticated token with granted authorities.
 * </ul>
 *
 * <p>On failure, distinct exception types are used to distinguish bad credentials from disabled
 * accounts, allowing the caller to choose appropriate error responses.
 *
 * @see AuthenticationProvider
 * @see IAuthService
 * @see PasswordManager
 */
@Component
@AllArgsConstructor
public class CustomAuthenticationManager implements AuthenticationProvider {

    private final IAuthService userDetailsService;
    private final PasswordManager passwordManager;

    /**
     * Attempts to authenticate the user with the provided username and password.
     *
     * <p>The flow is: load user details → validate password hash match → check account status →
     * return authenticated token.
     *
     * @param authentication the authentication request object containing credentials
     * @return a fully authenticated {@link UsernamePasswordAuthenticationToken} with authorities
     * @throws AuthenticationException if authentication fails (invalid credentials or disabled
     *     account)
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>This provider only supports {@link UsernamePasswordAuthenticationToken}. Other token types
     * are ignored, allowing downstream providers in the chain to handle them.
     */
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
