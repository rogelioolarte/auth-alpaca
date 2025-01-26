package com.example.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    public String jwtPrivateKey;
    public String jwtPublicKey;
    public String jwtUserGenerator;
    public String jwtTimeExpiration;
    public String oauth2AuthorizedRedirectURI;
    public String frontendURI;

    public Set<String> getAuthorizedRedirectURIs() {
        return Set.of(oauth2AuthorizedRedirectURI);
    }
}
