# AuthAlpacaUi

Example frontend for the OAuth2 + JWT authentication flow. Built with **Angular 21**, managed with **Bun 1.3.11**.

---

## Prerequisites

- **Bun 1.3.11** — [Install Bun](https://bun.sh/docs/installation)

---

## Development server

```bash
# From project root:
cd auth-alpaca-ui && bun install && bun run start

# Or from auth-alpaca-ui/ directly:
bun install
bun run start
```

The dev server runs at `http://localhost:4200/` and auto-reloads on source changes.

---

## Code scaffolding

Angular CLI is available via Bun:

```bash
bun run ng generate component component-name
```

To see all available schematics:

```bash
bun run ng generate --help
```

---

## Building

```bash
bun run build
```

Compiled assets are written to `dist/auth-alpaca-ui/browser`.

---

## Local Production Build Preview

```bash
# Build + serve via http-server
bun run build
bunx http-server dist/auth-alpaca-ui/browser -p 4200 -o
```

---

## Running unit tests

```bash
bun run test
```

Unit tests use [Vitest](https://vitest.dev/) as the test runner.

> [!NOTE]
> **No Unit Tests exist for the UI.**
> Because this frontend is designed as a direct integration example to demonstrate the Spring Boot OAuth2/JWT authentication flow, it does not include comprehensive frontend unit test suites. The `.spec.ts` files contain only boilerplate test skeletons.

---

## Additional Resources

- [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli)
- [Frontend Architecture](../docs/frontend-architecture.md)
