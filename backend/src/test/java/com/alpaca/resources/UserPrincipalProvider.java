package com.alpaca.resources;

import com.alpaca.entity.User;
import com.alpaca.model.UserPrincipal;
import java.util.HashSet;
import java.util.UUID;

public class UserPrincipalProvider {

    public static UserPrincipal firstResponse() {
        return new UserPrincipal(
                new User(
                        UUID.fromString("019e0f51-038c-7f79-96b5-be2e0b329111"),
                        "admin@admin.com",
                        "123456789",
                        true,
                        true,
                        true,
                        true,
                        false,
                        false,
                        null,
                        new HashSet<>(),
                        null,
                        null,
                        new HashSet<>(),
                        new HashSet<>()),
                null);
    }

    public static UserPrincipal alternativeResponse() {
        return new UserPrincipal(
                new User(
                        UUID.fromString("019e0f52-a9da-7560-a196-359bbcf6571c"),
                        "user@user.com",
                        "1234567890",
                        true,
                        true,
                        true,
                        true,
                        false,
                        false,
                        null,
                        new HashSet<>(),
                        null,
                        null,
                        new HashSet<>(),
                        new HashSet<>()),
                null);
    }
}
