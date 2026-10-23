import { Permission } from './permission';

export type Role = {
  id: string;
  name: string;
  description: string;
  permissions: Permission[];
}

export type RoleRequest = {
  name: string;
  description: string;
  permissions: Permission[];
}