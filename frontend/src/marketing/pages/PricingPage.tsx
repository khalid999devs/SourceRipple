import { useState } from 'react';

import { useDocumentTitle } from '@/hooks/use-document-title';
import { PlanCard } from '@/marketing/components/PlanCard';
import { PLANS, type BillingPeriod } from '@/marketing/plans';

export function PricingPage() {
  useDocumentTitle('Pricing');
  const [period, setPeriod] = useState<BillingPeriod>('monthly');

  return (
    <section className="mx-auto flex max-w-[1200px] flex-col gap-8 px-6 py-24">
      <header className="flex flex-col gap-3">
        <h1 className="text-4xl font-semibold tracking-tight">Pricing</h1>
        <p className="max-w-2xl text-muted-foreground">
          SourceRipple Cloud is not available yet; the public instance is a free preview. Cloud
          prices are illustrative until launch.
        </p>
        <div role="group" aria-label="Billing period" className="flex gap-2">
          {(['monthly', 'yearly'] as const).map((option) => (
            <button
              key={option}
              type="button"
              aria-pressed={period === option}
              onClick={() => setPeriod(option)}
              className="rounded-md border px-3 py-1 text-sm text-muted-foreground capitalize aria-pressed:bg-surface-2 aria-pressed:text-foreground"
            >
              {option}
              {option === 'yearly' && ' (2 months free)'}
            </button>
          ))}
        </div>
      </header>
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-5">
        {PLANS.map((plan) => (
          <PlanCard key={plan.id} plan={plan} period={period} />
        ))}
      </div>
    </section>
  );
}
