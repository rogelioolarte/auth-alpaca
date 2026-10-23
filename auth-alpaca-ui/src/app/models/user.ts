import { Advertiser } from './advertiser';
import { Profile } from './profile';
import { Role } from './role';

export type UserRequest = {
  email: string;
  password?: string;
  roles: string[];
}

export type User = {
  id: string;
  email: string;
  roles: Role[];
  profile: Profile;
  advertiser: Advertiser;
}

export enum AuthProvider {
  local = 'local',
  google = 'google',
  github = 'github',
  provider = 'provider',
}

export type ChangePassword = {
  currentPassword: string;
  newPassword: string;
  reNewPassword: string;
}