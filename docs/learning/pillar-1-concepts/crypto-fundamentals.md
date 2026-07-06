> 🏠 [README](../../../README.md) > 🎓 [Learning Index](../index.md) > **Pillar 1: Crypto Fundamentals**

# 🛡️ Crypto Fundamentals: [Asymmetric](../glossary.md#asymmetric-encryption-rsa) Encryption

To understand how the [Resource Server](../glossary.md#resource-server) (Auth Alpaca) trusts a token issued by the [Identity Provider](../glossary.md#idp-identity-provider) (Google), you must understand the difference between Symmetric and Asymmetric cryptography.

## [Symmetric](../glossary.md#symmetric-encryption) Encryption: The Shared Secret
Symmetric encryption is like a traditional padlock. You use the **same key** to lock the box and to unlock it.
- **Pros**: Very fast.
- **Cons**: You have to share the key. If you want 10 servers to verify tokens, all 10 must have the secret key. If one server is compromised, the entire system is compromised.

## Asymmetric Encryption: The Public/Private Key Pair
Asymmetric encryption (like RSA) uses a **pair of keys**. What one key locks, only the other can unlock.

### The "Post Office" Analogy
Imagine the Identity Provider (IdP) has a [**Public Key**](../glossary.md#public-key) and a [**Private Key**](../glossary.md#private-key).

1. **The Public Key (The Open Lockbox)**: The IdP gives the Public Key to the whole world. Anyone can have it. The Public Key is like an open lockbox that anyone can put a message into and snap shut.
2. **The Private Key (The Master Key)**: The IdP keeps the Private Key locked in a vault. It is the *only* key in existence that can open those lockboxes.

### How it works in Auth Alpaca
In our project, we use this for [**Digital Signatures**](../glossary.md#digital-signature):
- **The IdP signs the JWT using its PRIVATE key**. This is like stamping the token with a seal that only the IdP possesses.
- **The Resource Server verifies the JWT using the IdP's PUBLIC key**. The public key can check if the seal is authentic, but it *cannot* be used to create a new seal.

This is revolutionary because the Resource Server **never needs the private key**. Even if our backend is completely hacked, the attacker cannot issue new tokens because they don't have Google's private key.

> **Think Deeper**: If we decided to rotate our keys (generate a new pair), what would happen to the tokens that were issued using the old private key? Would they still be valid? How should a system handle a "Key Rotation" without logging out every single user?

---

| ← [Previous](jwt-deep-dive.md) | [↑ Learning Index](../index.md) | [Next](../pillar-2-architecture/backend-flow.md) → |
|:---|:---:|---:|
