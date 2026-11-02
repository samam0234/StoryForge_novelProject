import { MatPaginatorIntl } from '@angular/material/paginator';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    { provide: MatPaginatorIntl, useFactory: koreanPaginator },
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
  ],
};

function koreanPaginator(): MatPaginatorIntl {
  const intl = new MatPaginatorIntl();
  intl.nextPageLabel = '다음 페이지';
  intl.previousPageLabel = '이전 페이지';
  intl.firstPageLabel = '첫 페이지';
  intl.lastPageLabel = '마지막 페이지';
  intl.getRangeLabel = (page, size, length) => {
    if (!length) return '0건';
    const end = Math.min((page + 1) * size, length);
    return `${page * size + 1}–${end} / ${length}건`;
  };
  return intl;
}
