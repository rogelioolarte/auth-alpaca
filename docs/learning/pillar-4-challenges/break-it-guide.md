# 🔨 The Break-it Guide: Learning through Destruction

The best way to understand a security system is to see how it fails. In these challenges, your goal is to intentionally sabotage the system to observe the "failure modes".

## Challenge 1: The Expiration Paradox
**Objective**: Experience the "flicker" of a session timeout.

1. **The Sabotage**: Find the configuration where the token lifespan is defined (or mock the `JwtDecoder` to treat any token older than 10 seconds as expired).
2. **The Action**: Log in to the application and wait 11 seconds. Then, try to click a protected link or refresh the page.
3. **The Observation**: 
   - Does the app crash? 
   - Does the Interceptor handle the 401 response? 
   - Does the user get redirected to login automatically?
4. **The Analysis**: Discuss the trade-off. Short tokens are more secure (less time for an attacker to use a stolen token), but they create a worse UX (frequent logouts). How do "Refresh Tokens" solve this?

## Challenge 2: Cryptographic Sabotage
**Objective**: Understand why the "Trust Chain" is absolute.

1. **The Sabotage**: In the backend configuration, replace the IdP's public key (or the JWK set URI) with a completely random string or a different public key.
2. **The Action**: Attempt to log in with a valid token from Google.
3. **The Observation**: Look at the server logs. You will see a `JwtException` or `SignatureException`.
4. **The Analysis**: The token is perfectly valid (it was signed by Google), but the server no longer "trusts" the signer. This proves that the security is not in the *token*, but in the *shared trust* of the public key.

> **Think Deeper**: In a real production environment, if you accidentally deploy a backend with the wrong public key, you've effectively locked out 100% of your users. How can you implement a "Health Check" for your security configuration to prevent this from happening in production?
