import { Routes } from "@angular/router";
import { adminGuard } from "./core/auth";

export const routes: Routes = [
  {
    path: "login",
    loadComponent: () => import("./features/login").then((m) => m.LoginPage),
  },
  {
    path: "",
    canActivate: [adminGuard],
    canActivateChild: [adminGuard],
    loadComponent: () => import("./layout/shell").then((m) => m.Shell),
    children: [
      {
        path: "",
        pathMatch: "full",
        loadComponent: () =>
          import("./features/overview").then((m) => m.OverviewPage),
      },
      {
        path: "reports",
        loadComponent: () =>
          import("./features/reports").then((m) => m.ReportsPage),
      },
      {
        path: "users",
        loadComponent: () =>
          import("./features/users").then((m) => m.UsersPage),
      },
      {
        path: "audit",
        loadComponent: () =>
          import("./features/audit").then((m) => m.AuditPage),
      },
    ],
  },
  { path: "**", redirectTo: "" },
];
