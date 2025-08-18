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

    private static final String sub = "123";
    private static final String firstName = "FirstName";
    private static final String lastName = "LastName";
    private static final String email = "example@example.com";
    private static final String picture = "http://example.com/image.jpg";
    private static final String name = firstName + " " + lastName;

    @BeforeEach
    void setUp() {
        userInfo =
                new GoogleUserInfo(
                        Map.of(
                                "sub", sub,
                                "name", name,
                                "given_name", firstName,
                                "family_name", lastName,
                                "email", email,
                                "picture", picture,
                                "email_verified", true));
    }

    @Test
    void shouldReturnId() {
        assertEquals(sub, userInfo.getId());
    }

    @Test
    void shouldReturnFullName() {
        assertEquals(name, userInfo.getFullName());
    }

    @Test
    void shouldReturnFirstName() {
        assertEquals(firstName, userInfo.getFirstName());
    }

    @Test
    void shouldReturnLastName() {
        assertEquals(lastName, userInfo.getLastName());
    }

    @Test
    void shouldReturnEmail() {
        assertEquals(email, userInfo.getEmail());
    }

    @Test
    void shouldReturnImageUrl() {
        assertEquals(picture, userInfo.getImageUrl());
    }

    @Test
    void shouldReturnEmailVerified() {
        assertTrue(userInfo.getEmailVerified());
    }
}
