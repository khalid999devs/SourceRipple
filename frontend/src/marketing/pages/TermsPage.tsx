import { useDocumentTitle } from '@/hooks/use-document-title';

export function TermsPage() {
  useDocumentTitle('Terms');

  return (
    <section className="mx-auto flex max-w-3xl flex-col gap-4 px-6 py-24">
      <h1 className="text-3xl font-semibold tracking-tight">Terms</h1>
      <p className="text-muted-foreground">
        Draft, to be reviewed before any commercial launch. The public instance is a free preview
        with no service-level agreement.
      </p>
    </section>
  );
}
