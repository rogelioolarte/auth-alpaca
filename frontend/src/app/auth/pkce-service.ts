import { Injectable } from '@angular/core';

@Injectable({ 
  providedIn: 'root' 
})
export class PkceService {

  private static readonly VERIFIER_KEY = 'pkce_verifier';
  private static readonly VERIFIER_LENGTH = 64;
  private codeVerifier: string | null = null;

  generateCodeVerifier(): string {
    const randomBytes = new Uint8Array(PkceService.VERIFIER_LENGTH);
    crypto.getRandomValues(randomBytes);
    const verifier = this.base64UrlEncode(randomBytes);

    sessionStorage.setItem(PkceService.VERIFIER_KEY, verifier);
    this.codeVerifier = verifier;

    return verifier;
  }

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

  async generateCodeChallenge(verifier: string): Promise<string> {
    this.assertVerifier(verifier);

    const data = new TextEncoder().encode(verifier);
    const digest = await crypto.subtle.digest('SHA-256', data);

    return this.base64UrlEncode(new Uint8Array(digest));
  }

  clear(): void {
    this.codeVerifier = null;
    sessionStorage.removeItem(PkceService.VERIFIER_KEY);
  }

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
  
  private base64UrlEncode(bytes: Uint8Array): string {
    let binary = '';
    const chunkSize = 0x8000; // evita stack overflow en arrays grandes

    for (let i = 0; i < bytes.length; i += chunkSize) {
      binary += String.fromCharCode(...bytes.subarray(i, i + chunkSize));
    }

    return btoa(binary)
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  }
}