import { Sheet, SheetContent, SheetTitle } from '@/components/ui/overlay/sheet';
import { useUiStore } from '@/stores/ui-store';

import { SidebarContent } from './SidebarContent';

export function MobileSidebar() {
  const open = useUiStore((state) => state.mobileNavOpen);
  const setOpen = useUiStore((state) => state.setMobileNavOpen);

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetContent side="left" aria-describedby={undefined} className="w-72 gap-0 p-0">
        <SheetTitle className="sr-only">Navigation</SheetTitle>
        <SidebarContent collapsed={false} onNavigate={() => setOpen(false)} />
      </SheetContent>
    </Sheet>
  );
}
