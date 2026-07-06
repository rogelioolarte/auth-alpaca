import { Injectable } from '@angular/core';

@Injectable({ 
  providedIn: 'root' 
})
/**
 * Generates and manages PKCE (Proof Key for Code Exchange) code verifiers and challenges
 * per RFC 7636. Uses `sessionStorage` to persist the verifier across page refreshes
 * during the OAuth2 redirect flow.
 */
export class PkceService {

  /** SessionStorage key used to persist the code verifier across tabs and page loads. */
  private static readonly VERIFIER_KEY = 'pkce_verifier';

  /** Number of cryptographically random bytes (64) used to generate the code verifier. */
  private static readonly VERIFIER_LENGTH = 64;

  /** In-memory cache of the code verifier to avoid repeated sessionStorage reads. */
  private codeVerifier: string | null = null;

  /**
   * Generates a cryptographically random code verifier using `crypto.getRandomValues`,
   * encodes it as base64url, persists it to both the in-memory cache and sessionStorage,
   * and returns the verifier string.
   *
   * @returns A base64url-encoded code verifier (64 random bytes).
   */
  generateCodeVerifier(): string {
    const randomBytes = new Uint8Array(PkceService.VERIFIER_LENGTH);
    crypto.getRandomValues(randomBytes);
    const verifier = this.base64UrlEncode(randomBytes);

    sessionStorage.setItem(PkceService.VERIFIER_KEY, verifier);
    this.codeVerifier = verifier;

    return verifier;
  }

  /**
   * Retrieves the stored code verifier from the in-memory cache first, then falls back
   * to sessionStorage. This allows the verifier to survive page refreshes during the
   * OAuth2 redirect flow.
   *
   * @returns The stored code verifier string.
   * @throws Error if no verifier is found in either cache or sessionStorage.
   */
  getCodeVerifier(): string {
    if (this.codeVerifier) {
      return this.codeVerifier;
    }

    const stored = sessionStorage.getItem(PkceService.VERIFIER_KEY);

    if (!stored) {
      throw new Error('PKCE code_verifier not found');
    }

    this.codeVerifier = stored;
    return stored;
  }

  /**
   * Generates a new code verifier, validates it, then produces the corresponding
   * code challenge by hashing the verifier with SHA-256 via `crypto.subtle.digest`
   * and encoding the digest as base64url.
   *
   * @returns A Promise that resolves to the base64url-encoded SHA-256 code challenge.
   */
  async generateCodeChallenge(): Promise<string> {
    const codeVerifier = this.generateCodeVerifier();
    this.assertVerifier(codeVerifier);

    const data = new TextEncoder().encode(codeVerifier);
    const digest = await crypto.subtle.digest('SHA-256', data);

    return this.base64UrlEncode(new Uint8Array(digest));
  }

  /**
   * Clears the in-memory code verifier cache and removes the verifier from sessionStorage.
   * Call this after the OAuth2 flow completes to clean up sensitive data.
   */
  clear(): void {
    this.codeVerifier = null;
    sessionStorage.removeItem(PkceService.VERIFIER_KEY);
  }

  /**
   * Validates a code verifier against RFC 7636 rules: length must be between 43 and 128
   * characters, and must consist only of unreserved characters (`[A-Za-z0-9-._~]`).
   *
   * @param verifier The code verifier string to validate.
   * @throws Error if the verifier is empty, out of length bounds, or contains invalid characters.
   */
  private assertVerifier(verifier: string): void {
    if (!verifier) {
      throw new Error('code_verifier is required');
    }

    if (verifier.length < 43 || verifier.length > 128) {
      throw new Error('Invalid code_verifier length');
    }

    if (!/^[A-Za-z0-9\-._~]+$/.test(verifier)) {
      throw new Error('Invalid code_verifier format');
    }
  }
  
  /**
   * Encodes a `Uint8Array` as a URL-safe base64 string. Replaces `+` with `-`, `/` with `_`,
   * and strips padding (`=`) per the base64url specification (RFC 4648 §5).
   * Processes the array in chunks to avoid stack overflow on large inputs.
   *
   * @param bytes The byte array to encode.
   * @returns A URL-safe base64 string without padding.
   */
  private base64UrlEncode(bytes: Uint8Array): string {
    let binary = '';
    const chunkSize = 0x8000; // Prevents stack overflow on large arrays

    for (let i = 0; i < bytes.length; i += chunkSize) {
      binary += String.fromCharCode(...bytes.subarray(i, i + chunkSize));
    }

    return btoa(binary)
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  }
}