export const ROLES = ['ADMIN', 'MAINTAINER', 'REVIEWER'] as const;

export type Role = (typeof ROLES)[number];
