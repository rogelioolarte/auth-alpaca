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

> **Think Deeper**: Since the Payload is only Base64 encoded (not encrypted), anyone who intercepts the token can read the user's email and roles. Based on this, should you ever store a user's password or a credit card number inside a JWT payload? Why or why not?

---

| ← [Previous](oauth2-flow.md) | [↑ Learning Index](../index.md) | [Next](crypto-fundamentals.md) → |
|:---|:---:|---:|
