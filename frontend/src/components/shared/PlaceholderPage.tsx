import { useDocumentTitle } from '@/hooks/use-document-title';

interface PlaceholderPageProps {
  title: string;
}

export function PlaceholderPage({ title }: PlaceholderPageProps) {
  useDocumentTitle(title);

  return (
    <section className="mx-auto flex max-w-3xl flex-col gap-3 px-6 py-16">
      <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
      <p className="text-muted-foreground">Coming soon.</p>
    </section>
  );
}
