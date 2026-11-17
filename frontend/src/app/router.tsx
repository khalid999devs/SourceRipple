import { createBrowserRouter } from 'react-router';

import { RequireAuth } from '@/app/guards/RequireAuth';
import { RequireRole } from '@/app/guards/RequireRole';
import { NotFoundPage } from '@/components/shared/NotFoundPage';
import { RouteFallback } from '@/components/shared/RouteFallback';

export const router = createBrowserRouter([
  {
    HydrateFallback: RouteFallback,
    children: [
      {
        lazy: () =>
          import('@/components/layout/MarketingShell').then((m) => ({
            Component: m.MarketingShell,
          })),
        children: [
          {
            index: true,
            lazy: () =>
              import('@/marketing/pages/LandingPage').then((m) => ({ Component: m.LandingPage })),
          },
          {
            path: 'pricing',
            lazy: () =>
              import('@/marketing/pages/PricingPage').then((m) => ({ Component: m.PricingPage })),
          },
          {
            path: 'privacy',
            lazy: () =>
              import('@/marketing/pages/PrivacyPage').then((m) => ({ Component: m.PrivacyPage })),
          },
          {
            path: 'terms',
            lazy: () =>
              import('@/marketing/pages/TermsPage').then((m) => ({ Component: m.TermsPage })),
          },
        ],
      },
      {
        path: 'login',
        lazy: () =>
          import('@/features/auth/pages/LoginPage').then((m) => ({ Component: m.LoginPage })),
      },
      {
        path: 'register',
        lazy: () =>
          import('@/features/auth/pages/RegisterPage').then((m) => ({ Component: m.RegisterPage })),
      },
      {
        path: 'app',
        Component: RequireAuth,
        children: [
          {
            lazy: () =>
              import('@/components/layout/AppShell').then((m) => ({ Component: m.AppShell })),
            children: [
              {
                index: true,
                lazy: () =>
                  import('@/features/overview/pages/OverviewPage').then((m) => ({
                    Component: m.OverviewPage,
                  })),
              },
              {
                path: 'onboarding',
                lazy: () =>
                  import('@/features/onboarding/pages/OnboardingPage').then((m) => ({
                    Component: m.OnboardingPage,
                  })),
              },
              {
                path: 'projects',
                lazy: () =>
                  import('@/features/projects/pages/ProjectsPage').then((m) => ({
                    Component: m.ProjectsPage,
                  })),
              },
              {
                path: 'projects/:projectId',
                lazy: () =>
                  import('@/features/projects/pages/ProjectDetailPage').then((m) => ({
                    Component: m.ProjectDetailPage,
                  })),
              },
              {
                path: 'watches',
                lazy: () =>
                  import('@/features/watches/pages/WatchesPage').then((m) => ({
                    Component: m.WatchesPage,
                  })),
              },
              {
                path: 'watches/new',
                lazy: () =>
                  import('@/features/watches/pages/NewWatchPage').then((m) => ({
                    Component: m.NewWatchPage,
                  })),
              },
              {
                path: 'watches/:watchId',
                lazy: () =>
                  import('@/features/watches/pages/WatchDetailPage').then((m) => ({
                    Component: m.WatchDetailPage,
                  })),
              },
              {
                path: 'changes',
                lazy: () =>
                  import('@/features/changes/pages/ChangesPage').then((m) => ({
                    Component: m.ChangesPage,
                  })),
              },
              {
                path: 'changes/:changeId',
                lazy: () =>
                  import('@/features/changes/pages/ChangeDetailPage').then((m) => ({
                    Component: m.ChangeDetailPage,
                  })),
              },
              {
                path: 'dependencies',
                lazy: () =>
                  import('@/features/dependencies/pages/DependencyMapPage').then((m) => ({
                    Component: m.DependencyMapPage,
                  })),
              },
              {
                path: 'impacts',
                lazy: () =>
                  import('@/features/impacts/pages/ImpactInboxPage').then((m) => ({
                    Component: m.ImpactInboxPage,
                  })),
              },
              {
                path: 'integrations',
                lazy: () =>
                  import('@/features/integrations/pages/IntegrationsPage').then((m) => ({
                    Component: m.IntegrationsPage,
                  })),
              },
              {
                path: 'notifications',
                lazy: () =>
                  import('@/features/notifications/pages/NotificationsPage').then((m) => ({
                    Component: m.NotificationsPage,
                  })),
              },
              {
                path: 'settings',
                lazy: () =>
                  import('@/features/settings/pages/SettingsPage').then((m) => ({
                    Component: m.SettingsPage,
                  })),
              },
              {
                path: 'admin',
                element: <RequireRole role="ADMIN" />,
                children: [
                  {
                    index: true,
                    lazy: () =>
                      import('@/features/admin/pages/AdminPage').then((m) => ({
                        Component: m.AdminPage,
                      })),
                  },
                ],
              },
            ],
          },
        ],
      },
      { path: '*', Component: NotFoundPage },
    ],
  },
]);
