export interface AuthRequest {
    email: string
    password: string
}

export interface AuthResponse {
    accessToken: string,
    refreshToken: string
}

export interface UserAuth {
  id: string,
  profileId: string,
  advertiserId: string,
  username: string,
  enable: boolean,
  accountNonExpired: boolean,
  accountNonLocked: boolean,
  credentialsNonExpired: boolean,
  authorities: Authority[],
}

export interface Authority {
  authority: string
}

export enum AuthProvider {
  local = 'local',
  google = 'google',
  github = 'github',
  provider = 'provider'
}