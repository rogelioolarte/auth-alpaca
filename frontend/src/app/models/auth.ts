import { TokenDecode } from './decode';

export interface AuthRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
}

export interface UserPrincipal {
  id: string;
  profileId: string;
  advertiserId: string;
  username: string;
  enable: boolean;
  accountNonExpired: boolean;
  accountNonLocked: boolean;
  credentialsNonExpired: boolean;
  authorities: Authority[];
}

export interface Authority {
  authority: string;
}

export interface UserAuth {
  id: string;
  profileId?: string;
  advertiserId?: string;
  username: string;
  authorities: string[];
}

export enum AuthProvider {
  local = 'local',
  google = 'google',
  github = 'github',
  provider = 'provider',
}

export function convertDecodeToUserAuth(token: TokenDecode): UserAuth {
  return {
    id: token.userId,
    username: token.sub,
    advertiserId: token.advertiserId,
    profileId: token.profileId,
    authorities:
      token.authorities && token.authorities?.length > 0 ? token.authorities?.split(' ') : [],
  };
}

export function convertPrincipalToUserAuth(principal: UserPrincipal): UserAuth {
  return {
    id: principal.id,
    username: principal.username,
    advertiserId: principal.advertiserId,
    profileId: principal.profileId,
    authorities: principal.authorities.map((i) => i.authority),
  };
}
