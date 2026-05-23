package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.OAuth2AuthenticationProcessingException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.resources.ProfileProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.service.IProfileService;
import com.alpaca.service.IRoleService;
import com.alpaca.service.IUserService;
import com.alpaca.service.impl.OAuth2ServiceImpl;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

/** Unit tests for {@link OAuth2ServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class OAuth2ServiceImplTest {

    @Mock private IRoleService roleService;
    @Mock private IUserService userService;
    @Mock private IProfileService profileService;

    @InjectMocks private OAuth2ServiceImpl service;

    private Profile profile;

    @BeforeEach
    void setUp() {
        profile = ProfileProvider.alternativeEntity();
    }

    // ---------------------------------------
    // processOAuth2User
    // ---------------------------------------
    @Test
    void processOAuth2User_whenEmailMissing_throwsOAuth2ProcessingException() {
        // Arrange: build attributes without email
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("given_name", "First");
        attributes.put("family_name", "Last");
        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getAttributes()).thenReturn(attributes);

        OAuth2UserRequest request = mock(OAuth2UserRequest.class);
        ClientRegistration reg = mock(ClientRegistration.class);
        when(reg.getRegistrationId()).thenReturn("google");
        when(request.getClientRegistration()).thenReturn(reg);

        // Act / Assert
        OAuth2AuthenticationProcessingException ex =
                assertThrows(
                        OAuth2AuthenticationProcessingException.class,
                        () -> service.processOAuth2User(request, oauth2User));
        assertTrue(ex.getMessage().contains("Email not found from OAuth2 Provider"));
    }

    @Test
    void processOAuth2User_validAttributes_newUserPath_registersAndReturnsOAuthUser() {
        // Arrange - attributes returned by provider
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "new@example.com");
        attributes.put("given_name", "First");
        attributes.put("family_name", "Last");
        attributes.put("picture", "http://img");
        attributes.put("email_verified", true);

        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getAttributes()).thenReturn(attributes);

        OAuth2UserRequest request = mock(OAuth2UserRequest.class);
        ClientRegistration reg = mock(ClientRegistration.class);
        when(reg.getRegistrationId()).thenReturn("google");
        when(request.getClientRegistration()).thenReturn(reg);

        // Stubs for register flow
        when(userService.existsByEmail("new@example.com")).thenReturn(false);
        when(roleService.getUserRoles()).thenReturn(Set.of()); // empty set ok
        User registered = new User("new@example.com", null, true, true, Set.of());
        registered.setId(UUID.randomUUID());
        when(userService.save(any(User.class))).thenReturn(registered);
        // profile created inside registerProfile -> simulate profileService.save
        when(profileService.save(any(Profile.class))).thenReturn(profile);

        // Act
        OAuth2User result =
                service.processOAuth2User(
                        request, oauth2User); // should call registerOrLoginOAuth2 internally

        // Assert
        assertNotNull(result);
        // result is UserPrincipal (which implements OAuth2User) OR other OAuth2User from
        // registerOrLogin
        // In our implementation process returns UserPrincipal (via registerOrLoginOAuth2)
        assertInstanceOf(UserPrincipal.class, result);

        // verify registration happened with encoded password
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(userCaptor.capture());
        User passed = userCaptor.getValue();
        assertEquals("new@example.com", passed.getEmail());
        assertNull(passed.getPassword());
        // profile saved with given names and image
        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(profileService).save(profileCaptor.capture());
        Profile savedProfile = profileCaptor.getValue();
        assertEquals("First", savedProfile.getFirstName());
        assertEquals("Last", savedProfile.getLastName());
        assertEquals("http://img", savedProfile.getAvatarUrl());
    }

    // ---------------------------------------
    // registerOrLoginOAuth2 - validations & branches
    // ---------------------------------------
    @Test
    void registerOrLoginOAuth2_whenMissingFields_throwsBadRequest() {
        String emailA = "e";
        String firstA = "f";
        String firstB = " ";
        String lastA = "l";
        String imgA = "img";
        Map<String, Object> attr = Map.of();
        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () -> service.registerOrLoginOAuth2(null, firstA, lastA, imgA, true, attr));
        assertTrue(ex.getMessage().contains("The account does not have enough information"));

        ex =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                service.registerOrLoginOAuth2(
                                        emailA, firstB, lastA, imgA, true, attr));
        assertTrue(ex.getMessage().contains("The account does not have enough information"));

        ex =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                service.registerOrLoginOAuth2(
                                        emailA, firstA, null, imgA, true, attr));
        assertTrue(ex.getMessage().contains("The account does not have enough information"));

        ex =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                service.registerOrLoginOAuth2(
                                        emailA, firstA, lastA, null, true, attr));
        assertTrue(ex.getMessage().contains("The account does not have enough information"));
    }

    @ParameterizedTest(name = "Stage {index}: email={0}, firstName={1}, lastName={2}, imageURL={3}")
    @MethodSource("provideInvalidUserInputs")
    void registerOrLoginOAuth2_ShouldThrowBadRequestException_WhenInputsAreInvalid(
            String email, String firstName, String lastName, String imageURL) {

        // Act & Assert
        BadRequestException ex =
                assertThrowsExactly(
                        BadRequestException.class,
                        () ->
                                service.registerOrLoginOAuth2(
                                        email, firstName, lastName, imageURL, true, Map.of()));
        assertTrue(ex.getMessage().contains("The account does not have enough information"));
    }

    // Proveedor de datos para el test parametrizado
    private static Stream<Arguments> provideInvalidUserInputs() {
        return Stream.of(
                // Casos para email (null, vacío, espacios)
                Arguments.of(null, "John", "Doe", "http://image.url"),
                Arguments.of("", "John", "Doe", "http://image.url"),
                Arguments.of("   ", "John", "Doe", "http://image.url"),

                // Casos para firstName
                Arguments.of("john@example.com", null, "Doe", "http://image.url"),
                Arguments.of("john@example.com", "", "Doe", "http://image.url"),
                Arguments.of("john@example.com", "   ", "Doe", "http://image.url"),

                // Casos para lastName
                Arguments.of("john@example.com", "John", null, "http://image.url"),
                Arguments.of("john@example.com", "John", "", "http://image.url"),
                Arguments.of("john@example.com", "John", "   ", "http://image.url"),

                // Casos para imageURL
                Arguments.of("john@example.com", "John", "Doe", null),
                Arguments.of("john@example.com", "John", "Doe", ""),
                Arguments.of("john@example.com", "John", "Doe", "   "));
    }

    @Test
    void registerOrLoginOAuth2_existingUser_callsCheckExistingUser_andReturnsUserPrincipal() {
        // Arrange
        Map<String, Object> attributes = Map.of("extra", "value");
        String email = "exists@example.com";
        when(userService.existsByEmail(email)).thenReturn(true);

        User existing = UserProvider.alternativeEntity();
        existing.setEmail(email);
        // user not google connected but will be registered in checkExistingUser
        existing.setGoogleConnected(false);
        when(userService.findByEmail(email)).thenReturn(existing);
        when(userService.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        UserPrincipal principal =
                service.registerOrLoginOAuth2(email, "First", "Last", "img", true, attributes);

        // Assert
        assertNotNull(principal);
        assertEquals(email, principal.getUsername());
        verify(userService).existsByEmail(email);
        verify(userService).findByEmail(email);
    }

    @Test
    void registerOrLoginOAuth2_newUser_encodesPassword_andRegisters() {
        // Arrange
        Map<String, Object> attributes = Map.of("a", "b");
        String email = "brandnew@example.com";
        when(userService.existsByEmail(email)).thenReturn(false);
        when(roleService.getUserRoles()).thenReturn(Set.of());
        User registered = new User(email, "encoded-secret", true, true, Set.of());
        registered.setId(UUID.randomUUID());
        when(userService.save(any(User.class))).thenReturn(registered);
        when(profileService.save(any(Profile.class))).thenReturn(profile);

        // Act
        UserPrincipal principal =
                service.registerOrLoginOAuth2(email, "First", "Last", "img", true, attributes);

        // Assert
        assertNotNull(principal);
        assertEquals(email, principal.getUsername());
        verify(userService).save(any(User.class));
        verify(profileService).save(any(Profile.class));
    }

    // ---------------------------------------
    // registerProfile validations + success
    // ---------------------------------------
    @Test
    void registerProfile_whenInvalid_throwsBadRequest() {
        User userA = new User();
        String firstA = "f";
        String lastA = "l";
        String imgA = "img";
        // user null
        assertThrows(
                BadRequestException.class,
                () -> service.registerProfile(null, firstA, lastA, imgA));

        // user.id null
        assertThrows(
                BadRequestException.class,
                () -> service.registerProfile(userA, firstA, lastA, imgA));

        // firstName null
        User hasId = new User();
        hasId.setId(UUID.randomUUID());
        assertThrows(
                BadRequestException.class, () -> service.registerProfile(hasId, null, lastA, imgA));

        // lastName null
        assertThrows(
                BadRequestException.class,
                () -> service.registerProfile(hasId, firstA, null, imgA));

        // imageURL null
        assertThrows(
                BadRequestException.class,
                () -> service.registerProfile(hasId, firstA, lastA, null));
    }

    @Test
    void registerProfile_success_savesProfileOnUser() {
        User u = new User();
        u.setId(UUID.randomUUID());
        when(profileService.save(any(Profile.class))).thenReturn(profile);

        User result = service.registerProfile(u, "First", "Last", "img");

        assertNotNull(result);
        assertNotNull(result.getProfile());
        assertEquals(profile, result.getProfile());
        verify(profileService).save(any(Profile.class));
    }

    // ---------------------------------------
    // checkExistingUser - branches
    // ---------------------------------------
    @Test
    void checkExistingUser_whenNotAllowed_throwsUnauthorized() {
        User u = UserProvider.singleEntity();
        u.setAccountNonExpired(false);
        u.setAccountNonLocked(false);
        u.setCredentialNonExpired(false);
        assertThrows(UnauthorizedException.class, () -> service.checkExistingUser(u, true));
    }

    @Test
    void checkExistingUser_whenNotGoogleConnected_registersAndReturns() {
        User u = UserProvider.singleEntity();
        u.setGoogleConnected(false);
        when(userService.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User res = service.checkExistingUser(u, true);

        assertTrue(res.isGoogleConnected());
        verify(userService).save(any(User.class));
    }

    @Test
    void checkExistingUser_whenEmailVerifiedChanged_registersAndReturns() {
        User u = UserProvider.singleEntity();
        u.setGoogleConnected(true);
        u.setEmailVerified(false);
        when(userService.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User res = service.checkExistingUser(u, true);

        assertTrue(res.isEmailVerified());
        verify(userService).save(any(User.class));
    }

    @Test
    void checkExistingUser_whenNoChange_returnsSameUser() {
        User u = UserProvider.singleEntity();
        u.setGoogleConnected(true);
        u.setEmailVerified(true);

        User res = service.checkExistingUser(u, true);

        assertSame(u, res);
        verify(userService, never()).save(any());
    }
}
