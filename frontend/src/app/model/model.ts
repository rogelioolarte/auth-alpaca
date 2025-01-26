export enum AuthProvider {
  local = 'local',
  google = 'google',
  github = 'github',
  provider = 'provider'
}

export interface UserProfile {
  id: number,
  email: string,
  firstname: string,
  lastname: string
}
