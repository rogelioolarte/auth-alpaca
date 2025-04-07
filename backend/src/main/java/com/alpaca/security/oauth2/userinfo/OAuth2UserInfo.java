package com.alpaca.security.oauth2.userinfo;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class OAuth2UserInfo {

  protected Map<String, Object> attributes;

  public abstract String getId();

  public abstract String getFullName();

  public abstract String getFirstName();

  public abstract String getLastName();

  public abstract String getEmail();

  public abstract String getImageUrl();

  public abstract boolean getEmailVerified();
}
