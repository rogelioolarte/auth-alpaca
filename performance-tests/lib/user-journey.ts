import { login, getMe, rotateToken, logout } from './auth-helper.ts';

export interface User {
  email: string;
  pass: string;
}

export interface JourneyResult {
  success: boolean;
  error?: string;
  steps: {
    login: any;
    getMe: any;
    rotate: any;
    logout: any;
  };
}

/**
 * Orchestrates the full user journey: Login -> Get Me -> Rotate -> Logout.
 * @param user User credentials
 * @returns JourneyResult containing the status and responses of each step
 */
export function userJourney(user: User): JourneyResult {
  const steps: JourneyResult['steps'] = {};

  try {
    // 1. Login
    const auth = login(user);
    steps.login = auth;
    const { accessToken, refreshToken } = auth;

    // 2. Get Me
    steps.getMe = getMe(accessToken);

    // 3. Rotate Token
    const rotated = rotateToken(accessToken, refreshToken);
    steps.rotate = rotated;
    const { refreshToken: newRefreshToken } = rotated;

    // 4. Logout
    logout(newRefreshToken);
    steps.logout = { status: 'success' };

    return {
      success: true,
      steps,
    };
  } catch (e: any) {
    return {
      success: false,
      error: e.message,
      steps,
    };
  }
}

/**
 * Simplified journey for the warm-up phase to prime JIT and connection pools.
 * Focuses on high-impact endpoints without completing the full cycle.
 * @param user User credentials
 */
export function warmup(user: User): void {
  try {
    const auth = login(user);
    getMe(auth.accessToken);
    // We skip rotation and logout during warmup to reduce noise
    // and focus on priming the most used paths.
  } catch (e) {
    // Warmup failures are logged but typically not treated as fatal 
    // in the same way as baseline tests.
    console.warn(`Warmup failed for ${user.email}: ${e.message}`);
  }
}
