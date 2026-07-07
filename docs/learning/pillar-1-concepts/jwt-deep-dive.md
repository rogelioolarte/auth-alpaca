> [README](../../../README.md) > [Learning Index](../index.md) > **Pillar 1: JWT Deep Dive**

# [JWT](../glossary.md#jwt-json-web-token) Deep Dive: The Anatomy of a Stateless Token

A JSON Web Token (JWT) is not just a string of random characters. It is a digitally signed piece of information that allows a server to verify a user's identity without needing to look them up in a database on every single request.

## The Structure: Three Parts, One Dot

A JWT consists of three parts separated by dots: `Header.Payload.Signature`

### 1. The Header (The "How")
The header typically contains two pieces of information: the type of token (JWT) and the signing algorithm being used (e.g., RS256 or HS256).
*Example:* `{"alg": "RS256", "typ": "JWT"}`

### 2. The Payload (The "What")
This is the heart of the token. It contains [**Claims**](../glossary.md#claim). Claims are statements about the user and the token itself.
- **Registered Claims**: Standardized fields like `sub` (subject/user ID), `iat` (issued at), and `exp` (expiration time).
- **Custom Claims**: Project-specific data, like `roles: ["ADMIN", "USER"]` or `email: "alice@example.com"`.
*Example:* `{"sub": "12345", "name": "Alice", "role": "ADMIN", "exp": 1718100000}`

### 3. The Signature (The "Truth")
The [signature](../glossary.md#digital-signature) is what makes the token secure. It is created by taking the encoded header, the encoded payload, and a **secret key**, and running them through the algorithm specified in the header.

$$\text{Signature} = \text{Algorithm}(\text{Base64(Header)} + \text{Base64(Payload)}, \text{SecretKey})$$

## The Power of Statelessness

In traditional sessions, the server stores a `sessionId` in a database. Every request requires a DB lookup. 

With JWTs, the server doesn't store anything. When the server receives a token, it simply re-calculates the signature using its secret key. If the calculated signature matches the one on the token, the server **knows** the payload hasn't been tampered with.

**The logic is simple**: If the signature is valid, the data inside is the truth.

## A Note on Algorithm Choice: ES256 vs RS256

The [Auth Alpaca](https://github.com/rogelioolarte/auth-alpaca) project signs its JWTs with **ES256** — ECDSA using the P-256 (secp256r1) elliptic curve with SHA-256. You may be more familiar with **RS256** (RSA with SHA-256), which is still the default in many OIDC providers and legacy systems. This project chose ES256 deliberately, and here is why.

### Security at Smaller Key Sizes

The most common argument for elliptic curve cryptography is that it delivers equivalent security with drastically smaller keys. A 256-bit P-256 key provides roughly **128 bits of security** — the same security margin as a **3072-bit RSA key**. In practice:

| Security Level | RSA Key Size | EC Key Size (ES256) |
|---|---:|---:|
| 128-bit | 3072 bits | 256 bits |

### Smaller Tokens, Less Bandwidth

Because ES256 signatures are only **64 bytes** (vs 256 bytes for a 2048-bit RS256 signature), the JWTs this project issues are noticeably smaller. In an API that attaches a Bearer token to every request, that saving compounds quickly — less bandwidth, lower latency, smaller cookie headers.

| Property | RS256 (RSA) | ES256 (ECDSA) |
|---|---|---|
| Key size for 128-bit security | 3072 bits | 256 bits |
| Signature size | ~256 bytes | 64 bytes |
| Signing speed | Slow | Fast |
| Verification speed | Fast | Moderate |
| Token size impact | +342 bytes | +86 bytes |

### Faster Signing for Token Issuance

ES256 signing is **several times faster** than RS256 at the same security level. This matters most on the authorization server, where tokens are created on every login. For a system issuing thousands of tokens per second, the difference is measurable.

Verification speed is comparable between the two — RSA has efficient public-key verification, and ECDSA verification is slightly more expensive than signing but still very fast for per-request use.

### Modern Standard for New Systems

ES256 (and ECDSA generally) is increasingly the recommended default for new authentication systems. It has first-class support in Java 11+ (which this project targets via Java 25), all major JWT libraries, and every mainstream OIDC provider including Auth0, Okta, and Apple.

A common concern is that ES256 requires a high-quality random nonce per signature (a bad RNG can leak the private key). Modern libraries including JJWT handle this internally with CSPRNGs — you get the security benefit without the footgun.

### Caveat: No Silver Bullet

RS256 is not obsolete. It remains the right choice when:
- Interoperating with legacy OIDC providers that only publish RSA JWKS keys.
- Operating in environments where ECDSA hardware acceleration is unavailable.
- Complying with requirements that mandate RSA (e.g., some FIPS 140-2 profiles).

For this project, none of those constraints apply. ES256 gives us smaller tokens, faster signing, and stronger security per bit — the right trade for a modern auth system.

> For a deeper dive into the cryptographic math behind ES256 and RSA, see [Crypto Fundamentals](crypto-fundamentals.md).

> **Think Deeper**: Since the Payload is only Base64 encoded (not encrypted), anyone who intercepts the token can read the user's email and roles. Based on this, should you ever store a user's password or a credit card number inside a JWT payload? Why or why not?

---

| ← [Previous](oauth2-flow.md) | [↑ Learning Index](../index.md) | [Next](crypto-fundamentals.md) → |
|:---|:---:|---:|
