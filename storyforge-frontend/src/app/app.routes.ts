import { Routes } from '@angular/router';
import { adminGuard, authGuard, guestGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'terms',
    loadComponent: () => import('./features/legal/terms.page').then((m) => m.TermsPage),
  },
  {
    path: '',
    pathMatch: 'full',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.page').then((m) => m.DashboardPage),
  },
  // 게시판과 작품 읽기는 로그인 없이 볼 수 있습니다 (공유 링크).
  {
    path: 'board/:kind',
    loadComponent: () => import('./features/board/board.page').then((m) => m.BoardPage),
  },
  {
    path: 'works/:workId',
    loadComponent: () => import('./features/board/work-detail.page').then((m) => m.WorkDetailPage),
  },
  {
    path: 'create',
    loadComponent: () => import('./features/lounge/create.page').then((m) => m.CreatePage),
  },
  {
    path: 'posts/:postId',
    loadComponent: () => import('./features/lounge/post-detail.page').then((m) => m.PostDetailPage),
  },
  {
    path: 'issues',
    loadComponent: () => import('./features/issues/issues.page').then((m) => m.IssuesPage),
  },
  {
    path: 'users/:userId',
    loadComponent: () => import('./features/profile/profile.page').then((m) => m.ProfilePage),
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/moderation/admin-reports.page').then((m) => m.AdminReportsPage),
  },
  {
    path: 'package-forge',
    canActivate: [authGuard],
    loadComponent: () => import('./features/packages/package-forge.page').then((m) => m.PackageForgePage),
  },
  {
    path: 'package-forge/:workId',
    canActivate: [authGuard],
    loadComponent: () => import('./features/packages/package-forge.page').then((m) => m.PackageForgePage),
  },
  {
    path: 'me/works',
    canActivate: [authGuard],
    loadComponent: () => import('./features/profile/my-works.page').then((m) => m.MyWorksPage),
  },
  {
    path: 'me/works/:workId/edit',
    canActivate: [authGuard],
    loadComponent: () => import('./features/profile/work-edit.page').then((m) => m.WorkEditPage),
  },
  {
    path: 'me/settings',
    canActivate: [authGuard],
    loadComponent: () => import('./features/profile/account-settings.page').then((m) => m.AccountSettingsPage),
  },
  {
    path: 'projects/:projectId',
    canActivate: [authGuard],
    loadComponent: () => import('./features/projects/project-shell.page').then((m) => m.ProjectShellPage),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'bible' },
      {
        path: 'bible',
        loadComponent: () => import('./features/story-bible/story-bible.page').then((m) => m.StoryBiblePage),
      },
      {
        path: 'outline',
        loadComponent: () => import('./features/outline/outline.page').then((m) => m.OutlinePage),
      },
      {
        path: 'settings',
        loadComponent: () => import('./features/projects/project-settings.page').then((m) => m.ProjectSettingsPage),
      },
      {
        path: 'chapters',
        loadComponent: () => import('./features/chapters/chapter-workspace.page').then((m) => m.ChapterWorkspacePage),
      },
      {
        path: 'activity',
        loadComponent: () => import('./features/projects/project-activity.page').then((m) => m.ProjectActivityPage),
      },
      {
        path: 'publish',
        loadComponent: () => import('./features/projects/project-publish.page').then((m) => m.ProjectPublishPage),
      },
      {
        path: 'upstream',
        loadComponent: () => import('./features/projects/project-upstream.page').then((m) => m.ProjectUpstreamPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
