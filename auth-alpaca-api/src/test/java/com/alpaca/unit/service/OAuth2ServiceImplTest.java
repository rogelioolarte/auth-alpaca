package com.alpaca.unit.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.OAuth2AuthenticationProcessingException;
import com.alpaca.resources.provider.ProfileProvider;
import com.alpaca.resources.provider.UserProvider;
import com.alpaca.service.IAuthService;
import com.alpaca.service.impl.OAuth2ServiceImpl;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

/** Unit tests for {@link OAuth2ServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class OAuth2ServiceImplTest {

    @Mock private IAuthService authService;
    @Mock private OAuth2UserRequest request;
    @Mock private OAuth2User oauth2User;
    @Mock private ClientRegistration clientRegistration;

    @InjectMocks private OAuth2ServiceImpl service;

    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        User user = UserProvider.singleEntity();
        Profile profile = ProfileProvider.singleEntity();
        user.setProfile(profile);
        attributes = new HashMap<>();
        attributes.put("email", user.getEmail());
        attributes.put("given_name", user.getProfile().getFirstName());
        attributes.put("family_name", user.getProfile().getLastName());
        attributes.put("picture", user.getProfile().getAvatarUrl());
        attributes.put("email_verified", user.isEmailVerified());
    }

    @Test
    void processOAuth2UserShouldThrowOAuth2AuthenticationProcessingExceptionWhenEmailIsMissing() {
        attributes.remove("email");

        when(request.getClientRegistration()).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn("google");
        when(oauth2User.getAttributes()).thenReturn(attributes);

        assertThatThrownBy(() -> service.processOAuth2User(request, oauth2User))
                .isInstanceOf(OAuth2AuthenticationProcessingException.class)
                .hasMessage("The account does not have enough information");

        verifyNoInteractions(authService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "given_name", "family_name", "picture"})
    void processOAuth2UserShouldThrowOAuth2AuthenticationProcessingExceptionWhenEmailIsBlank(
            String attribute) {
        attributes.put("email", " ");
        attributes.put(attribute, " ");

        when(request.getClientRegistration()).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn("google");
        when(oauth2User.getAttributes()).thenReturn(attributes);

        assertThatThrownBy(() -> service.processOAuth2User(request, oauth2User))
                .isInstanceOf(OAuth2AuthenticationProcessingException.class)
                .hasMessage("The account does not have enough information");

        verifyNoInteractions(authService);
    }

    @Test
    void processOAuth2UserShouldThrowBadRequestExceptionWhenEmailHasText() {
        attributes.put("email", "");
        when(request.getClientRegistration()).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn("google");
        when(oauth2User.getAttributes()).thenReturn(attributes);

        assertThatThrownBy(() -> service.processOAuth2User(request, oauth2User))
                .isInstanceOf(OAuth2AuthenticationProcessingException.class)
                .hasMessageContaining("The account does not have enough information");

        verifyNoInteractions(authService);
    }

    @Test
    void processOAuth2UserShouldRegisterUserWhenAllInformationIsBlank() {
        attributes.put("email", " ");
        attributes.put("given_name", " ");
        attributes.put("family_name", " ");
        attributes.put("picture", " ");

        when(request.getClientRegistration()).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn("google");
        when(oauth2User.getAttributes()).thenReturn(attributes);

        assertThatThrownBy(() -> service.processOAuth2User(request, oauth2User))
                .isInstanceOf(OAuth2AuthenticationProcessingException.class)
                .hasMessage("The account does not have enough information");

        verifyNoInteractions(authService);
    }

    @Test
    void processOAuth2UserShouldNotRegisterUserBecauseCurrentValidationIsUnreachable() {
        attributes.put("given_name", "");
        attributes.put("family_name", "");
        when(request.getClientRegistration()).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn("google");
        when(oauth2User.getAttributes()).thenReturn(attributes);

        assertThatThrownBy(() -> service.processOAuth2User(request, oauth2User))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("The account does not have enough information");

        verify(authService, never()).registerOAuth2User(any());
    }
}
