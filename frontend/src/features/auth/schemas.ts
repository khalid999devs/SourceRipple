import { z } from 'zod';

const email = z
  .email({ error: 'Enter a valid email address.' })
  .max(320, { error: 'Use at most 320 characters.' });

export const loginSchema = z.object({
  email,
  password: z.string().min(1, { error: 'Enter your password.' }),
});

export const registerSchema = z.object({
  displayName: z
    .string()
    .trim()
    .min(1, { error: 'Enter your name.' })
    .max(100, { error: 'Use at most 100 characters.' }),
  email,
  password: z
    .string()
    .min(10, { error: 'Use at least 10 characters.' })
    .max(72, { error: 'Use at most 72 characters.' }),
  accountType: z.enum(['MAINTAINER', 'REVIEWER'], {
    error: 'Choose how you will use SourceRipple.',
  }),
});

export type LoginValues = z.infer<typeof loginSchema>;
export type RegisterValues = z.infer<typeof registerSchema>;
