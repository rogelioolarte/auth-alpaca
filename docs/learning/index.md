> [README](../../README.md) — **Learning Path: 4-Pillar Journey**

## Jump to Pillar
- [Pillar 1: Concepts](./pillar-1-concepts/oauth2-flow.md)
- [Pillar 2: Architecture](./pillar-2-architecture/backend-flow.md)
- [Pillar 3: Exploration](./pillar-3-exploration/treasure-hunt.md)
- [Pillar 4: Challenges](./pillar-4-challenges/build-it-tasks.md)

---

# Auth Alpaca: A Learning Path for Modern Authentication

This learning path takes you from OAuth2 concepts to a working implementation. No black boxes, no magic — just clear explanations and real code.

Authentication and Authorization are often treated as "black boxes" provided by libraries. In this learning path, we crack those boxes open. We move from the foundations of cryptography to the intricate dance of OAuth2, and finally into the guts of a production-ready Spring Boot and Angular implementation.

## The Course Map

This journey is divided into four pillars. We follow a strict cognitive progression: **Theory $\rightarrow$ Translation $\rightarrow$ Analysis $\rightarrow$ Application.**

### Pillar 1: Concepts (The Theory)
*Establishing the mental model.*
Before looking at a single line of code, we must understand the "Why".
- **OAuth2 Flow**: The narrative of delegation.
- **JWT Deep Dive**: The anatomy of a stateless token.
- **Crypto Fundamentals**: The magic of asymmetric keys.
- **Outcome**: You will be able to sketch the authentication dance on a whiteboard from memory.

### Pillar 2: Architecture (The Translation)
*Mapping theory to implementation.*
We translate abstract concepts into the specific components of the Auth Alpaca project.
- **Backend Flow**: The "Request Odyssey" through the Spring Security Filter Chain.
- **Frontend Security**: How Angular Guards and Interceptors enforce the security contract.
- **Component Map**: A Rosetta Stone linking theoretical roles to project classes.
- **Outcome**: You will understand exactly where a request is intercepted, validated, and authorized.

### Pillar 3: Exploration (The Forensic Analysis)
*Evidence-based learning.*
Stop reading manuals; start hunting evidence.
- **The Treasure Hunt**: Directed missions to locate critical security logic in the source code.
- **Pattern Analysis**: Identifying Clean Architecture and SRP in the auth flow.
- **Outcome**: You will develop the "architectural eye" to navigate any security-heavy codebase.

### Pillar 4: Challenges (The Forge)
*Active learning through destruction and creation.*
The final test. To truly understand a system, you must be able to break it and extend it.
- **The Break-it Guide**: Intentionally sabotaging the system to observe failure modes.
- **The Build-it Tasks**: Implementing a `SUPER_ADMIN` role from scratch.
- **Outcome**: You will have proven your mastery by modifying the system's security constraints.

---

## Prerequisites

To get the most out of this learning path, you should have a basic grasp of:
- **Java 17+**: Understanding of Classes, Interfaces, and Annotations.
- **Angular**: Basic knowledge of Components, Services, and Dependency Injection.
- **HTTP Basics**: Knowledge of Verbs (GET, POST), Headers, and Status Codes (200, 401, 403).

### Additional Resources

Pillar 4 (Challenges) assumes some familiarity with Spring Boot annotations and Angular templates. If you get stuck there:

| Topic | Resource |
|---|---|
| Spring Boot annotations (`@RestController`, `@PreAuthorize`) | [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/) |
| Angular structural directives (`*ngIf`) and routing guards | [Angular Documentation](https://angular.dev/guide/templates) |

**Ready to begin? Start with [Pillar 1: Concepts](./pillar-1-concepts/oauth2-flow.md).**

> **Think Deeper**: Why is it important to learn the *concepts* before looking at the *code*? What happens to an engineer who learns the "how" (the library API) without the "why" (the protocol)?

---

| Start with [Pillar 1: Concepts](pillar-1-concepts/oauth2-flow.md) → |
|:---|
