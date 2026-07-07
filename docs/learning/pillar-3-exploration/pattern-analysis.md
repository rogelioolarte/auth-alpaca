> [README](../../../README.md) > [Learning Index](../index.md) > **Pillar 3: Pattern Analysis**

# Pattern Analysis: Clean Architecture in Auth

The `auth-alpaca` project isn't just about security; it's a study in professional software engineering. Let's analyze the patterns used to keep the security logic maintainable.

## 1. The Interception Pattern (Cross-Cutting Concerns)
Security is a "cross-cutting concern"—it affects every single endpoint. If we put `if (user.isAdmin())` at the start of every controller method, our code would be a mess of duplication.

**The Solution**: Use Filters (Backend) and Interceptors (Frontend).
By moving the security check into a separate "layer" that wraps the request, the business logic in the Controller remains "pure". The Controller assumes that if the request reached it, the user is already authenticated.

## 2. Single Responsibility Principle (SRP)
Look at how the token process is split:
- `JwtDecoder`: Only cares about the **math** (Is the [signature](../glossary.md#digital-signature) valid?).
- `JwtAuthenticationFilter`: Only cares about the **plumbing** (Extract token $\rightarrow$ call decoder $\rightarrow$ set context).
- `AuthorizationFilter`: Only cares about the **policy** (Does this user have the right role?).

Each class has exactly one reason to change. If we change our signing algorithm, only the `Decoder` changes. If we change our role names, only the `AuthorizationFilter` (or the annotations) changes.

## 3. Dependency Inversion
The security configuration doesn't depend on a specific User database. It depends on an `AuthenticationManager` and `UserDetailsService` interface. This means we could swap Google Auth for a local database or LDAP without changing a single line of the Filter chain logic.

> **Think Deeper**: Imagine we wanted to add a feature that logs every "Unauthorized (403)" attempt to a database for security auditing. Where would be the most "Clean Architecture" place to put this logic? Inside the Controller, or as a new Filter in the chain? Why?

---

| ← [Previous](treasure-hunt.md) | [↑ Learning Index](../index.md) | [Next](../pillar-4-challenges/break-it-guide.md) → |
|:---|:---:|---:|
