import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/dashboard/dashboard.page').then((m) => m.DashboardPage),
  },
  {
    path: 'projects/:projectId',
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
        path: 'chapters',
        loadComponent: () => import('./features/chapters/chapter-workspace.page').then((m) => m.ChapterWorkspacePage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
