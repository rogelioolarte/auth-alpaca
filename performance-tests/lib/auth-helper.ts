import http from 'k6/http';
import { CONFIG } from './config.ts';

/**
 * Authenticates a user and returns access and refresh tokens.
 * @param user User credentials
 * @returns { accessToken: string, refreshToken: string }
 */
export function login(user: { email: string, pass: string }): 
  ({ accessToken: string, refreshToken: string }) {
  const payload = JSON.stringify({
    email: user.email,
    password: user.pass,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Client-Id': CONFIG.clientId,
    },
  };

  const res = http.post(`${CONFIG.baseURL}/api/auth/login`, payload, params);

  if (res.status !== 200) {
    throw new Error(`Login failed with status ${res.status}: ${res.body}`);
  }

  return res.json() as { accessToken: string, refreshToken: string };
}

/**
 * Retrieves the current user's profile.
 * @param token Access token
 * @returns User profile data
 */
export function getMe(token: string) {
  const params = {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  };

  const res = http.get(`${CONFIG.baseURL}/api/auth/me`, params);

  if (res.status !== 200) {
    throw new Error(`getMe failed with status ${res.status}: ${res.body}`);
  }

  return res.json();
}

/**
 * Rotates the access token using a refresh token.
 * @param accessToken Current access token for authentication
 * @param refreshToken Valid refresh token
 * @returns { accessToken: string, refreshToken: string }
 */
export function rotateToken(accessToken: string, refreshToken: string): 
  ({ accessToken: string, refreshToken: string }) {
  const params = {
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'X-Refresh-Token': refreshToken,
      'X-Client-Id': CONFIG.clientId,
    },
  };

  const res = http.post(`${CONFIG.baseURL}/api/auth/rotate`, null, params);

  if (res.status !== 200) {
    throw new Error(`rotateToken failed with status ${res.status}: ${res.body}`);
  }

  return res.json() as { accessToken: string, refreshToken: string };
}

/**
 * Logs out the user by invalidating the session.
 * @param refreshToken Refresh token to be invalidated
 */
export function logout(refreshToken: string): void {
  const params = {
    headers: {
      'X-Refresh-Token': refreshToken,
      'X-Client-Id': CONFIG.clientId,
    },
  };

  const res = http.post(`${CONFIG.baseURL}/api/auth/logout`, null, params);

  if (res.status !== 200) {
    throw new Error(`logout failed with status ${res.status}: ${res.body}`);
  }
}
