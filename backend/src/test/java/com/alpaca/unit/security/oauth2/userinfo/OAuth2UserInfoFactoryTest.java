package com.alpaca.unit.security.oauth2.userinfo;

import com.alpaca.exception.BadRequestException;
import com.alpaca.security.oauth2.userinfo.GoogleUserInfo;
import com.alpaca.security.oauth2.userinfo.OAuth2UserInfo;
import com.alpaca.security.oauth2.userinfo.OAuth2UserInfoFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OAuth2UserInfoFactory Unit Tests")
class OAuth2UserInfoFactoryTest {

    @Test
    void shouldReturnGoogleUserInfo_WhenRegistrationIdIsGoogle() {
        OAuth2UserInfo userInfo =
                OAuth2UserInfoFactory.getOAuth2UserInfo("google", Map.of("sub", "1234567890"));
        assertNotNull(userInfo);
        assertInstanceOf(GoogleUserInfo.class, userInfo);
        assertEquals("1234567890", userInfo.getId());
    }

    @Test
    void shouldThrowBadRequestException_WhenRegistrationIdIsNotSupported() {
        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () -> OAuth2UserInfoFactory.getOAuth2UserInfo("facebook", Map.of()));
        assertEquals("Login with facebook is not supported", ex.getReason());
    }
}
