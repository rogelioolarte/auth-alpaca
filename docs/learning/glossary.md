> [README](../../README.md) > [Learning Index](index.md) — **Glossary**

# The Language of Authentication: Glossary

To speak the language of security, you must master these terms.

### Core Protocols
- **OAuth2 (Open Authorization)**: An industry-standard framework for authorization. It allows a website or application to access resources hosted by other web apps on behalf of a user.
- **OIDC (OpenID Connect)**: A simple identity layer on top of the OAuth 2.0 protocol. While OAuth2 is about *Authorization* (what you can do), OIDC is about *Authentication* (who you are).

### Tokens & Identity
- **JWT (JSON Web Token)**: A compact, URL-safe means of representing claims to be transferred between two parties.
- **Bearer Token**: A token that grants access to whoever "bears" (possesses) it. No further identification is required.
- **Access Token**: A token used by the client to make authenticated requests to the Resource Server.
- **ID Token**: A token (specific to OIDC) that contains user profile information.
- **Claim**: A piece of information asserted about a subject (e.g., `"email": "bob@example.com"`).
- **Scope**: A mechanism to limit an application's access to a user's account (e.g., `read:calendar`, `write:profile`).

### Cryptography
- **Symmetric Encryption**: Encryption where the same key is used for both encryption and decryption.
- **Asymmetric Encryption (RSA)**: Encryption using a public key for encryption/verification and a private key for decryption/signing.
- **Digital Signature**: A mathematical scheme for demonstrating the authenticity of a digital message.
- **Public Key**: A key that can be shared with anyone; used to verify signatures.
- **Private Key**: A secret key that must be guarded; used to create signatures.

### Project Specifics
- **IdP (Identity Provider)**: The system that creates, maintains, and manages identity information (e.g., Google, Okta).
- **Resource Server**: The server that hosts the protected data and accepts Access Tokens to grant access (Auth Alpaca).
- **Principal**: The currently authenticated user.
- **Authority/GrantedAuthority**: A permission or role granted to the Principal (e.g., `ROLE_ADMIN`).
- **Filter Chain**: A sequence of filters that a request must pass through before reaching the controller.

---

| [← Back to Learning Index](index.md) |
|:---:|
