package com.alpaca.resources;

import com.alpaca.entity.User;
import com.alpaca.model.UserPrincipal;
import java.util.HashSet;
import java.util.UUID;

public class UserPrincipalProvider {

    public static UserPrincipal firstResponse() {
        return new UserPrincipal(
                new User(
                        UUID.fromString("1632eb79-63a4-4213-b905-0ad176f0004a"),
                        "admin@admin.com",
                        "123456789",
                        true,
                        true,
                        true,
                        true,
                        false,
                        false,
                        new HashSet<>(),
                        null,
                        null),
                null);
    }

    public static UserPrincipal alternativeResponse() {
        return new UserPrincipal(
                new User(
                        UUID.fromString("982a1001-b033-48f6-b2e6-6b327f0a61eb"),
                        "user@user.com",
                        "1234567890",
                        true,
                        true,
                        true,
                        true,
                        false,
                        false,
                        new HashSet<>(),
                        null,
                        null),
                null);
    }
}
