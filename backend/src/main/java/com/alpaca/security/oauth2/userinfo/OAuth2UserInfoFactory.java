package com.alpaca.security.oauth2.userinfo;

import com.alpaca.exception.BadRequestException;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(
            String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase("google")) {
            return new GoogleUserInfo(attributes);
        } else {
            throw new BadRequestException(
                    String.format("Login with %s is not supported", registrationId));
        }
    }
}
