import { Outlet } from 'react-router';

import { OfflineBanner } from '@/components/shared/OfflineBanner';

import { Sidebar } from './Sidebar';
import { MobileSidebar } from './sidebar/MobileSidebar';
import { TopBar } from './TopBar';

export function AppShell() {
  return (
    <div className="flex flex-1">
      <Sidebar />
      <MobileSidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <OfflineBanner />
        <TopBar />
        <main className="flex flex-1 flex-col">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
