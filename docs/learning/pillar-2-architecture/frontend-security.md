> [README](../../../README.md) > [Learning Index](../index.md) > **Pillar 2: Frontend Security**

# Frontend Security: Guardians and Interceptors

Frontend security is **not** about preventing unauthorized access to data—that's the backend's job. Frontend security is about **User Experience (UX)** and **Reducing Noise**.

In Auth Alpaca, we use two primary tools in Angular: **Interceptors** and **Guards**.

## 1. HTTP Interceptors: The Automatic Courier
We don't want to manually add the `Authorization: Bearer <token>` header to every single API call in our services. That would be repetitive and error-prone.

Instead, we use an **Interceptor**. Think of it as a middleware for outgoing requests.
- The Interceptor catches every outgoing HTTP request.
- It retrieves the [JWT](../glossary.md#jwt-json-web-token) from HTTP-only cookies via the `CookieService`, not from `localStorage`.
- It clones the request and injects the `Authorization` header.
- It passes the modified request back to the Angular HTTP client.

## 2. Route Guards: The UI Gatekeepers
Route Guards (`CanActivate`) prevent the user from navigating to a page they aren't allowed to see.

For example, if a user tries to go to `/admin`, the `AdminGuard` checks the current user's roles. 
- **If Admin**: The guard returns `true`, and the page loads.
- **If Not Admin**: The guard returns `false` (or redirects to `/login`), and the user never sees the admin UI.

## The "Illusion" of Frontend Security
It is critical to remember: **Anything on the frontend can be bypassed.** A clever user can open the DevTools, modify the Angular state, and force a Guard to return `true`. 

This is why the backend **must** verify the token on every request. The Frontend Guard is there to keep the UI clean; the Backend Filter is there to keep the data safe.

> **Think Deeper**: If a user manually types `/admin` in the URL and bypasses the Guard, but the Backend returns a `403 Forbidden`, how should the Frontend Interceptor handle that response to provide a good user experience?

---

| ← [Previous](backend-flow.md) | [↑ Learning Index](../index.md) | [Next](component-map.md) → |
|:---|:---:|---:|
