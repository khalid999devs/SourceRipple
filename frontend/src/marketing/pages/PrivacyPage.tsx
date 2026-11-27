import { useDocumentTitle } from '@/hooks/useDocumentTitle';

export function PrivacyPage() {
  useDocumentTitle('Privacy');

  return (
    <section className="mx-auto flex max-w-3xl flex-col gap-4 px-6 py-24">
      <h1 className="text-3xl font-semibold tracking-tight">Privacy</h1>
      <p className="text-muted-foreground">
        Draft, to be reviewed before any commercial launch. SourceRipple sets no tracking cookies
        and sends no analytics to third parties.
      </p>
    </section>
  );
}
