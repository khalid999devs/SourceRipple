export type PlanId = 'COMMUNITY' | 'CLOUD_HOBBY' | 'CLOUD_PRO' | 'CLOUD_BUSINESS' | 'ENTERPRISE';

export type WaitlistPlan = 'CLOUD_PRO' | 'CLOUD_BUSINESS' | 'ENTERPRISE';

export type CtaState = 'available' | 'waitlist' | 'contact';

export type PlanPrice =
  | { kind: 'free'; label: string }
  | { kind: 'monthly'; usdPerMonth: number }
  | { kind: 'custom'; label: string };

export interface PlanCta {
  label: string;
  state: CtaState;
  href?: string;
  waitlistPlan?: WaitlistPlan;
  note?: string;
}

export interface Plan {
  id: PlanId;
  name: string;
  hosting: 'self-hosted' | 'cloud';
  price: PlanPrice;
  illustrative: boolean;
  audience: string;
  includes: readonly string[];
  comingSoon: readonly string[];
  recommended: boolean;
  cta: PlanCta;
}

export const YEARLY_BILLED_MONTHS = 10;

export const PLANS: readonly Plan[] = [
  {
    id: 'COMMUNITY',
    name: 'Community',
    hosting: 'self-hosted',
    price: { kind: 'free', label: 'Free forever' },
    illustrative: false,
    audience: 'Developers who run it themselves',
    includes: [
      'Every core feature',
      'Apache-2.0 license',
      'Limits set by your own infrastructure',
      'Community support on GitHub',
    ],
    comingSoon: [],
    recommended: false,
    cta: { label: 'Self-host on GitHub', state: 'available' },
  },
  {
    id: 'CLOUD_HOBBY',
    name: 'Cloud Hobby',
    hosting: 'cloud',
    price: { kind: 'free', label: '$0' },
    illustrative: false,
    audience: 'Trying it hosted',
    includes: [
      '1 organization',
      '3 projects',
      '10 watches',
      'Checks at least 24 h apart',
      '30-day history',
      'Dashboard and webhooks',
    ],
    comingSoon: [],
    recommended: false,
    cta: { label: 'Start free', state: 'available', href: '/register', note: 'Public preview' },
  },
  {
    id: 'CLOUD_PRO',
    name: 'Cloud Pro',
    hosting: 'cloud',
    price: { kind: 'monthly', usdPerMonth: 19 },
    illustrative: true,
    audience: 'Small teams and agencies',
    includes: [
      '5 members',
      '100 watches',
      'Checks at least 1 h apart',
      '1-year history',
      'API keys and MCP',
      'Email support',
    ],
    comingSoon: [],
    recommended: true,
    cta: { label: 'Join waitlist', state: 'waitlist', waitlistPlan: 'CLOUD_PRO' },
  },
  {
    id: 'CLOUD_BUSINESS',
    name: 'Cloud Business',
    hosting: 'cloud',
    price: { kind: 'monthly', usdPerMonth: 99 },
    illustrative: true,
    audience: 'Growing teams',
    includes: ['25 members', '1,000 watches', 'Checks at least 15 min apart', 'Priority support'],
    comingSoon: ['JS rendering', 'SSO', 'Audit export'],
    recommended: false,
    cta: { label: 'Join waitlist', state: 'waitlist', waitlistPlan: 'CLOUD_BUSINESS' },
  },
  {
    id: 'ENTERPRISE',
    name: 'Enterprise',
    hosting: 'cloud',
    price: { kind: 'custom', label: 'Custom' },
    illustrative: false,
    audience: 'Large or regulated organizations',
    includes: ['Support contract for self-hosting or a dedicated cloud', 'SLA', 'Custom limits'],
    comingSoon: [],
    recommended: false,
    cta: { label: 'Contact us', state: 'contact', waitlistPlan: 'ENTERPRISE' },
  },
];

export type BillingPeriod = 'monthly' | 'yearly';

export function formatPlanPrice(price: PlanPrice, period: BillingPeriod): string {
  switch (price.kind) {
    case 'free':
    case 'custom':
      return price.label;
    case 'monthly':
      return period === 'monthly'
        ? `$${price.usdPerMonth} / month`
        : `$${price.usdPerMonth * YEARLY_BILLED_MONTHS} / year`;
  }
}
