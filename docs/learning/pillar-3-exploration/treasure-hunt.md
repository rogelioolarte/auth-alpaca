> [README](../../../README.md) > [Learning Index](../index.md) > **Pillar 3: Code Treasure Hunt**

# The Code Treasure Hunt: Forensic Missions

Stop reading the documentation. It's time to find the evidence. Use your IDE's search tools (`Cmd+Shift+F` or `Ctrl+Shift+F`) to complete these missions. 

**Rule**: Do not ask for the answer. Find the line of code, read the surrounding logic, and deduce how it works.

### Mission 1: The Expiration Detective
**The Goal**: Find the exact place where the backend decides a token is "too old" and rejects the request.
- **Clue**: Look for classes that handle `Jwt` decoding or validation. Search for the term `exp` or `expiration`.
- **Question**: Does the system throw a specific exception when a token is expired? What is the name of that exception?

### Mission 2: The Role Gatekeeper
**The Goal**: Locate the "Lock" that protects the admin endpoints.
- **Clue**: Search for the string `@PreAuthorize` or look into the `SecurityFilterChain` configuration class.
- **Question**: Which specific role name is required to access the admin dashboard? Is it `ADMIN`, `ROLE_ADMIN`, or something else?

### Mission 3: The Token Vault
**The Goal**: Find where the frontend stores the [JWT](../glossary.md#jwt-json-web-token) and how it retrieves it.
- **Clue**: Search for the `storage` variable and the `CookieService` within the `src/app/auth` folder. Trace how the JWT is persisted between page refreshes.
- **Question**: Why did the developers choose HTTP-only cookies instead of `localStorage`? What security advantage does this provide against XSS attacks? What happens to the token when the user refreshes the page?

### Mission 4: The Request Interceptor
**The Goal**: Find the logic that automatically attaches the token to every outgoing request.
- **Clue**: Look for a class that implements `HttpInterceptor`. Search for the string `Authorization: Bearer`.
- **Question**: What happens if the token is missing from storage? Does the interceptor still send the request, or does it block it?

> **Think Deeper**: Now that you've found these four pieces, can you trace the "Life of a Token" from the moment it's stored in the browser to the moment it's validated by the backend? Which of these components fails first if the token is modified by a user?

---

| ← [Previous](../pillar-2-architecture/component-map.md) | [↑ Learning Index](../index.md) | [Next](pattern-analysis.md) → |
|:---|:---:|---:|
