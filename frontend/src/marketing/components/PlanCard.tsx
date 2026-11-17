import { Link } from 'react-router';

import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { formatPlanPrice, type BillingPeriod, type Plan } from '@/marketing/plans';

interface PlanCardProps {
  plan: Plan;
  period: BillingPeriod;
}

export function PlanCard({ plan, period }: PlanCardProps) {
  const { cta } = plan;

  return (
    <article
      className={cn(
        'flex flex-col gap-4 rounded-lg border bg-card p-6',
        plan.recommended && 'border-primary/60',
      )}
    >
      <header className="flex flex-col gap-1">
        <div className="flex items-center justify-between gap-2">
          <h2 className="text-lg font-semibold">{plan.name}</h2>
          {plan.recommended && (
            <span className="rounded-full bg-primary/15 px-2 py-0.5 text-xs text-primary">
              Recommended
            </span>
          )}
        </div>
        <p className="text-sm text-muted-foreground">{plan.audience}</p>
      </header>
      <p className="text-2xl font-semibold tabular-nums">
        {formatPlanPrice(plan.price, period)}
        {plan.illustrative && (
          <span className="ml-2 align-middle text-xs font-normal text-muted-foreground">
            illustrative
          </span>
        )}
      </p>
      <ul className="flex flex-1 flex-col gap-2 text-sm text-muted-foreground">
        {plan.includes.map((item) => (
          <li key={item}>{item}</li>
        ))}
        {plan.comingSoon.map((item) => (
          <li key={item}>
            {item} <span className="text-xs text-accent">Coming soon</span>
          </li>
        ))}
      </ul>
      <div className="flex flex-col gap-1">
        {cta.state === 'available' && cta.href ? (
          <Button asChild variant={plan.recommended ? 'default' : 'secondary'}>
            <Link to={cta.href}>{cta.label}</Link>
          </Button>
        ) : (
          <Button variant={plan.recommended ? 'default' : 'secondary'} disabled>
            {cta.label}
          </Button>
        )}
        {cta.note && <p className="text-center text-xs text-muted-foreground">{cta.note}</p>}
      </div>
    </article>
  );
}
