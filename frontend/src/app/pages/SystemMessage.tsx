import type { ReactNode } from 'react';

interface SystemMessageProps {
  code: string;
  title: string;
  children: ReactNode;
  actions?: ReactNode;
}

export function SystemMessage({ code, title, children, actions }: SystemMessageProps) {
  return (
    <section className="flex flex-1 items-center justify-center px-6 py-24">
      <div className="flex max-w-md flex-col items-center gap-4 text-center">
        <p className="font-mono text-sm text-primary">{code}</p>
        <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        <div className="flex flex-col gap-2 text-muted-foreground">{children}</div>
        {actions && <div className="mt-2 flex flex-wrap justify-center gap-2">{actions}</div>}
      </div>
    </section>
  );
}
