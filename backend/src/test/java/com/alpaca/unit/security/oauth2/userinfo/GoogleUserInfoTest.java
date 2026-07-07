package com.alpaca.unit.security.oauth2.userinfo;

import com.alpaca.security.oauth2.userinfo.GoogleUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GoogleUserInfo Unit Tests")
class GoogleUserInfoTest {

    private GoogleUserInfo userInfo;

    private static final String SUB = "123";
    private static final String FIRST_NAME = "FirstName";
    private static final String LAST_NAME = "LastName";
    private static final String EMAIL_EXAMPLE = "example@example.com";
    private static final String PICTURE_EXAMPLE = "https://example.com/image.jpg";
    private static final String FULL_NAME = FIRST_NAME + " " + LAST_NAME;

    @BeforeEach
    void setUp() {
        userInfo =
                new GoogleUserInfo(
                        Map.of(
                                "sub", SUB,
                                "name", FULL_NAME,
                                "given_name", FIRST_NAME,
                                "family_name", LAST_NAME,
                                "email", EMAIL_EXAMPLE,
                                "picture", PICTURE_EXAMPLE,
                                "email_verified", true));
    }

    @Test
    void shouldReturnId() {
        assertEquals(SUB, userInfo.getId());
    }

    @Test
    void shouldReturnFullName() {
        assertEquals(FULL_NAME, userInfo.getFullName());
    }

    @Test
    void shouldReturnFirstName() {
        assertEquals(FIRST_NAME, userInfo.getFirstName());
    }

    @Test
    void shouldReturnLastName() {
        assertEquals(LAST_NAME, userInfo.getLastName());
    }

    @Test
    void shouldReturnEmail() {
        assertEquals(EMAIL_EXAMPLE, userInfo.getEmail());
    }

    @Test
    void shouldReturnImageUrl() {
        assertEquals(PICTURE_EXAMPLE, userInfo.getImageUrl());
    }

    @Test
    void shouldReturnEmailVerified() {
        assertTrue(userInfo.getEmailVerified());
    }
}
