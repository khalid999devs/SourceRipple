import { useEffect } from 'react';

const SUFFIX = 'SourceRipple';

export function useDocumentTitle(title: string): void {
  useEffect(() => {
    document.title = title ? `${title} · ${SUFFIX}` : SUFFIX;
  }, [title]);
}
