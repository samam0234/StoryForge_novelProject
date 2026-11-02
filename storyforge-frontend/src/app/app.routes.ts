import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login.page').then((m) => m.LoginPage),
  },
  {
    path: '',
    pathMatch: 'full',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.page').then((m) => m.DashboardPage),
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
    ],
  },
  { path: '**', redirectTo: '' },
];
