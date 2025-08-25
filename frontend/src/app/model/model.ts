export enum AuthProvider {
  local = 'local',
  google = 'google',
  github = 'github',
  provider = 'provider'
}

export interface UserProfile {
  accountNonExpired: boolean,
  accountNonLocked: boolean,
  advertiserId: string,
  attributes: string,
  authorities: Authority[],
  credentialsNonExpired: boolean,
  enabled: boolean,
  id: string,
  name: string,
  password: string,
  profileId: string,
  username: string
}

export interface Authority {
  authority: string
}