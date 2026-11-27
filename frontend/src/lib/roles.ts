export const ROLES = ['ADMIN', 'MAINTAINER', 'REVIEWER'] as const;

export type Role = (typeof ROLES)[number];

export function hasRole(roles: readonly Role[] | undefined, role: Role): boolean {
  return roles?.includes(role) ?? false;
}
