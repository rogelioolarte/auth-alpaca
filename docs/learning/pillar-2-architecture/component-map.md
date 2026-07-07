> [README](../../../README.md) > [Learning Index](../index.md) > **Pillar 2: Component Map**

# The Component Map: Theory to Code

To master the project, you must be able to map the abstract concepts of the [OAuth2](../glossary.md#oauth2-open-authorization)/[JWT](../glossary.md#jwt-json-web-token) world to the actual classes and configurations in the `auth-alpaca` codebase.

| Theoretical Concept | Project Component (Backend) | Project Component (Frontend) | Role / Responsibility |
| :--- | :--- | :--- | :--- |
| [**Identity Provider (IdP)**](../glossary.md#idp-identity-provider) | `application.yml` (issuer-uri) | `auth.service.ts` (loginUrl) | The source of truth for user identity (e.g., Google). |
| [**Resource Server**](../glossary.md#resource-server) | `@SpringBootApplication` | N/A | The server protecting the data (Auth Alpaca). |
| [**Access Token**](../glossary.md#access-token) | `Jwt` / `JwtClaimsSet` | `accessToken` (string) | The "Key" used to access protected resources. |
| **Token Validator** | `JJwtManager` | N/A | Creates and validates JWTs with JJWT using ECDSA P-256 (ES256). |
| **Security Context** | `SecurityContextHolder` | `AuthService.currentUser` | The "Session" state for the current request. |
| [**Authority/Role**](../glossary.md#authoritygrantedauthority) | `GrantedAuthority` | `userRoles` (array) | The permissions assigned to the user. |
| **Authorization Logic** | `@PreAuthorize` | `AdminGuard` | The logic that decides if a user can access a resource. |
| **The Handshake** | `SecurityFilterChain` | `AuthInterceptor` | The machinery that handles the token exchange/injection. |

## How to use this map
When you are reading the code and see a class like `JwtAuthenticationFilter`, refer back to this map. You'll realize: *"Ah, this is part of the 'Handshake' machinery that implements the 'Token Validator' logic."*

> **Think Deeper**: If we decided to move from a "Role-based" system (`ROLE_ADMIN`) to a "Permission-based" system (`can_edit_users`), which columns in this table would need to change? Would the `JwtDecoder` care about the difference?

---

| ← [Previous](frontend-security.md) | [↑ Learning Index](../index.md) | [Next](../pillar-3-exploration/treasure-hunt.md) → |
|:---|:---:|---:|
