package com.alpaca.security.oauth2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

public class AuthRequestDeserializer extends JsonDeserializer<OAuth2AuthorizationRequest> {

  @Override
  public OAuth2AuthorizationRequest deserialize(JsonParser p, DeserializationContext ct)
      throws IOException {

    @SuppressWarnings("unchecked")
    Map<String, Object> data =
        Optional.ofNullable(p.readValueAs(Map.class))
            .orElseThrow(() -> new IOException("Invalid JSON format"));

    return OAuth2AuthorizationRequest.authorizationCode()
        .clientId(getString(data, "clientId"))
        .authorizationUri(getString(data, "authorizationUri"))
        .redirectUri(getString(data, "redirectUri"))
        .scopes(getSet(data))
        .state(getString(data, "state"))
        .attributes(getMap(data, "attributes"))
        .additionalParameters(getMap(data, "additionalParameters"))
        .build();
  }

  private String getString(Map<String, Object> data, String key) {
    return Optional.ofNullable(data.get(key))
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .orElse(null);
  }

  private Set<String> getSet(Map<String, Object> data) {
    Object value = data.get("scopes");
    if (value instanceof Collection<?>) {
      return ((Collection<?>) value)
          .stream()
              .filter(String.class::isInstance)
              .map(String.class::cast)
              .collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getMap(Map<String, Object> data, String key) {
    return Optional.ofNullable(data.get(key))
        .filter(Map.class::isInstance)
        .map(obj -> (Map<String, Object>) obj)
        .orElse(Collections.emptyMap());
  }
}
