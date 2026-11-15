import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Route, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { routes } from './app.routes';
import { ProjectContext } from './features/projects/project-context';

/**
 * 라우트에 연결된 모든 화면을 실제로 만들어 봅니다.
 *
 * 단위 테스트는 spec이 가져오는 파일만 컴파일하므로, 어떤 spec도 가져오지 않는 화면은
 * 템플릿이 깨져도 통과합니다 (docs/find_debug/2026-11-04.md). 이 스모크 테스트는 모든 화면을
 * 가져와 렌더링하므로 같은 종류의 오류를 `ng test`에서도 잡습니다.
 * 동작을 검증하지는 않습니다. 화면이 "그려지는지"만 확인합니다.
 */
async function collect(list: Route[], prefix = ''): Promise<{ path: string; component: Type<unknown> }[]> {
  const found: { path: string; component: Type<unknown> }[] = [];
  for (const route of list) {
    const path = `${prefix}/${route.path ?? ''}`.replace(/\/+/g, '/');
    if (route.loadComponent) {
      found.push({ path, component: (await route.loadComponent()) as Type<unknown> });
    }
    if (route.children) {
      found.push(...(await collect(route.children, path)));
    }
  }
  return found;
}

function activatedRoute(params: Record<string, string>) {
  const paramMap = convertToParamMap(params);
  const queryParamMap = convertToParamMap({});
  const snapshot = { params, paramMap, queryParams: {}, queryParamMap, fragment: null, data: {}, url: [] };
  const route: Record<string, unknown> = {
    snapshot,
    params: of(params),
    paramMap: of(paramMap),
    queryParams: of({}),
    queryParamMap: of(queryParamMap),
    fragment: of(null),
    data: of({}),
    url: of([]),
    parent: null,
  };
  route['parent'] = route;
  route['root'] = route;
  return route;
}

describe('화면 스모크 (모든 라우트)', () => {
  let screens: { path: string; component: Type<unknown> }[] = [];

  beforeAll(async () => {
    screens = await collect(routes);
  });

  afterEach(() => localStorage.clear());

  it('라우트에 연결된 화면이 있다', () => {
    expect(screens.length).toBeGreaterThanOrEqual(20);
  });

  it('모든 화면이 템플릿 오류 없이 만들어진다', () => {
    const failures: string[] = [];
    for (const { path, component } of screens) {
      const params = Object.fromEntries(
        [...path.matchAll(/:(\w+)/g)].map((m) => [m[1], m[1] === 'kind' ? 'long' : '1']),
      );
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [component],
        providers: [
          provideRouter([]),
          // 프로젝트 하위 화면은 ProjectShellPage가 제공하는 상태를 주입받습니다.
          ...(path.startsWith('/projects/:projectId/') ? [ProjectContext] : []),
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: ActivatedRoute, useValue: activatedRoute(params) },
        ],
      });
      try {
        const fixture = TestBed.createComponent(component);
        // 라우터가 경로 파라미터를 입력으로 넘기는 것(withComponentInputBinding)을 흉내 냅니다.
        for (const [name, value] of Object.entries(params)) {
          try {
            fixture.componentRef.setInput(name, value);
          } catch {
            /* 이 화면은 해당 입력이 없습니다 */
          }
        }
        fixture.detectChanges();
      } catch (error) {
        failures.push(`${path}: ${error instanceof Error ? error.message.split('\n')[0] : String(error)}`);
      }
    }
    expect(failures).toEqual([]);
  });
});
