> [README](../../../README.md) > [Learning Index](../index.md) > **Pillar 4: Build It Tasks**

# The Forge: Building the SUPER_ADMIN

Now that you can break the system, it's time to extend it. You are tasked with implementing a "God Mode" for the application.

## The Requirements

### Task 1: The Authority
Define a new security role: `ROLE_SUPER_ADMIN`.
- Ensure that this role is distinct from the standard `ROLE_ADMIN`.
- (Optional) Modify the mock user data or the [IdP](../glossary.md#idp-identity-provider) mapping to grant this role to your own account.

### Task 2: The Protected Resource
Create a new endpoint: `GET /api/admin/god-mode`.
- This endpoint should return a sensitive piece of information (e.g., "System Kernel Access Granted").
- **Constraint**: Only users with `ROLE_SUPER_ADMIN` should be able to access it. Standard admins should receive a `403 Forbidden`.

### Task 3: The UI Gate
Update the Angular frontend:
- Create a "God Mode" button in the navigation bar.
- **Constraint**: The button must be hidden (using `*ngIf` and the `AuthService`) for anyone who is not a Super Admin.
- Implement a `SuperAdminGuard` to protect the corresponding frontend route.

### Task 4: The Verification
Prove your implementation works by performing these four tests:
1. **Guest User**: Tries to access `/god-mode` $\rightarrow$ Redirected to Login.
2. **Standard User**: Tries to access `/god-mode` $\rightarrow$ 403 Forbidden.
3. **Standard Admin**: Tries to access `/god-mode` $\rightarrow$ 403 Forbidden.
4. **Super Admin**: Tries to access `/god-mode` $\rightarrow$ 200 OK.

> **Think Deeper**: If your organization grows and you now need 50 different roles with complex hierarchies (e.g., `RegionalManager` inherits `StoreManager` permissions), would `@PreAuthorize("hasRole('...')")` still be a viable strategy? What would be a more scalable way to manage permissions?

---

| ← [Previous](../pillar-3-exploration/pattern-analysis.md) | [↑ Learning Index](../index.md) | [Next](break-it-guide.md) → |
|:---|:---:|---:|
