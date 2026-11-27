import { createBrowserRouter } from 'react-router';

import { RequireAuth } from '@/app/guards/RequireAuth';
import { RequireRole } from '@/app/guards/RequireRole';
import { NotFoundPage } from '@/app/pages/NotFoundPage';
import { RouteErrorBoundary } from '@/app/pages/RouteErrorBoundary';
import type { CrumbHandle } from '@/components/layout/top-bar/Breadcrumbs';
import { RouteFallback } from '@/components/shared/RouteFallback';

const crumb = (label: string): CrumbHandle => ({ crumb: label });

export const router = createBrowserRouter([
  {
    HydrateFallback: RouteFallback,
    ErrorBoundary: RouteErrorBoundary,
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
            ErrorBoundary: RouteErrorBoundary,
            children: [
              {
                index: true,
                handle: crumb('Overview'),
                lazy: () =>
                  import('@/features/overview/pages/OverviewPage').then((m) => ({
                    Component: m.OverviewPage,
                  })),
              },
              {
                path: 'onboarding',
                handle: crumb('Get started'),
                lazy: () =>
                  import('@/features/onboarding/pages/OnboardingPage').then((m) => ({
                    Component: m.OnboardingPage,
                  })),
              },
              {
                path: 'projects',
                handle: crumb('Projects'),
                children: [
                  {
                    index: true,
                    lazy: () =>
                      import('@/features/projects/pages/ProjectsPage').then((m) => ({
                        Component: m.ProjectsPage,
                      })),
                  },
                  {
                    path: ':projectId',
                    handle: crumb('Project'),
                    lazy: () =>
                      import('@/features/projects/pages/ProjectDetailPage').then((m) => ({
                        Component: m.ProjectDetailPage,
                      })),
                  },
                ],
              },
              {
                path: 'watches',
                handle: crumb('Sources'),
                children: [
                  {
                    index: true,
                    lazy: () =>
                      import('@/features/watches/pages/WatchesPage').then((m) => ({
                        Component: m.WatchesPage,
                      })),
                  },
                  {
                    path: 'new',
                    handle: crumb('New watch'),
                    lazy: () =>
                      import('@/features/watches/pages/NewWatchPage').then((m) => ({
                        Component: m.NewWatchPage,
                      })),
                  },
                  {
                    path: ':watchId',
                    handle: crumb('Watch'),
                    lazy: () =>
                      import('@/features/watches/pages/WatchDetailPage').then((m) => ({
                        Component: m.WatchDetailPage,
                      })),
                  },
                ],
              },
              {
                path: 'changes',
                handle: crumb('Changes'),
                children: [
                  {
                    index: true,
                    lazy: () =>
                      import('@/features/changes/pages/ChangesPage').then((m) => ({
                        Component: m.ChangesPage,
                      })),
                  },
                  {
                    path: ':changeId',
                    handle: crumb('Change'),
                    lazy: () =>
                      import('@/features/changes/pages/ChangeDetailPage').then((m) => ({
                        Component: m.ChangeDetailPage,
                      })),
                  },
                ],
              },
              {
                path: 'dependencies',
                handle: crumb('Dependency Map'),
                lazy: () =>
                  import('@/features/dependencies/pages/DependencyMapPage').then((m) => ({
                    Component: m.DependencyMapPage,
                  })),
              },
              {
                path: 'impacts',
                handle: crumb('Impact Inbox'),
                lazy: () =>
                  import('@/features/impacts/pages/ImpactInboxPage').then((m) => ({
                    Component: m.ImpactInboxPage,
                  })),
              },
              {
                path: 'integrations',
                handle: crumb('Integrations'),
                lazy: () =>
                  import('@/features/integrations/pages/IntegrationsPage').then((m) => ({
                    Component: m.IntegrationsPage,
                  })),
              },
              {
                path: 'notifications',
                handle: crumb('Notifications'),
                lazy: () =>
                  import('@/features/notifications/pages/NotificationsPage').then((m) => ({
                    Component: m.NotificationsPage,
                  })),
              },
              {
                path: 'settings',
                handle: crumb('Settings'),
                lazy: () =>
                  import('@/features/settings/pages/SettingsPage').then((m) => ({
                    Component: m.SettingsPage,
                  })),
              },
              {
                path: 'admin',
                handle: crumb('Admin'),
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
              { path: '*', Component: NotFoundPage },
            ],
          },
        ],
      },
      { path: '*', Component: NotFoundPage },
    ],
  },
]);
