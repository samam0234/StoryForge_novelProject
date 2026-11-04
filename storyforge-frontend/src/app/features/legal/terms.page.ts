import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommunityApi } from '../../core/api/community-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { AuthService } from '../../core/auth/auth.service';

/**
 * 이용약관. 문구는 법률 검토 전 초안입니다. 문구를 고치면 백엔드 TERMS_VERSION과 아래 시행일을 함께 올립니다.
 * 설계 배경은 docs/plan/moderation.md
 */
@Component({
  selector: 'sf-terms-page',
  imports: [MatButtonModule, MatIconModule],
  template: `
    <div class="sf-page narrow">
      <h1>이용약관</h1>
      <p class="draft" role="note"><mat-icon inline>info</mat-icon> 법률 검토 전 초안입니다. 시행일 2026-11-04.</p>

      <h2>1. 내 글의 권리</h2>
      <p>작품과 게시글의 권리는 작성자에게 있습니다. StoryForge는 작품의 소유권을 가져가지 않습니다.</p>

      <h2>2. 서비스에 주는 허락</h2>
      <p>작품을 게시하면, 선택한 공개 범위 안에서 서비스가 작품을 저장하고 보여 주고 전달하는 것을 허락합니다. 게시를 삭제하면 이 허락은 끝납니다.</p>

      <h2>3. Clone과 내려받기</h2>
      <p>작가가 고른 Clone 정책과 내려받기 설정이 곧 다른 사용자에게 주는 허락입니다.</p>
      <ul>
        <li>Clone을 허용한 범위 안에서 다른 사용자는 작품의 설정과 원고를 자기 프로젝트로 가져와 파생작을 쓰고 게시할 수 있습니다.</li>
        <li>파생작에는 원작 표시가 자동으로 붙습니다. 지우지 않습니다.</li>
        <li>정책을 나중에 바꿔도 이미 Clone된 프로젝트에는 소급되지 않습니다.</li>
        <li>내려받은 파일을 작가의 허락 없이 서비스 밖에 다시 배포하거나 판매할 수 없습니다.</li>
      </ul>

      <h2>4. AI 생성물</h2>
      <p>AI가 만든 글은 틀리거나 다른 저작물과 비슷할 수 있습니다. 게시하기 전에 내용을 확인할 책임은 게시하는 사람에게 있습니다. 작업 그래프에는 수동과 에이전트 작업의 비율이 공개됩니다.</p>
      <p>실제 AI 공급자를 쓰는 환경에서는 생성에 필요한 설정과 요약이 그 공급자에게 전송됩니다.</p>

      <h2>5. 금지하는 일</h2>
      <ul>
        <li>다른 사람의 저작권, 초상권, 개인정보를 침해하는 글</li>
        <li>미성년자를 성적으로 묘사하는 글, 불법 정보, 특정인에 대한 괴롭힘과 혐오</li>
        <li>도배, 광고, 여러 계정으로 추천과 달성 기록을 부풀리는 행위</li>
        <li>서비스를 방해하거나 다른 사용자의 계정과 비공개 자료에 접근하려는 시도</li>
      </ul>

      <h2>6. 신고와 조치</h2>
      <p>누구나 작품, 글, 댓글, 사용자를 신고할 수 있습니다. 관리자는 글을 숨기거나 계정의 이용을 정지할 수 있으며, 조치와 사유를 당사자에게 알립니다. 숨긴 글은 복원할 수 있습니다.</p>

      <h2>7. 공개되는 정보</h2>
      <p>이름, 소개, 공개 작품, 공개 활동, 공유 기록에 적은 위치는 다른 사람에게 보입니다. 이메일은 공개하지 않습니다.</p>

      <h2>8. 계정 삭제</h2>
      <p>계정을 삭제하면 프로젝트, 작품, 댓글, 활동 기록이 지워집니다. 다른 사람이 이미 Clone한 프로젝트는 그 사람의 것으로 남습니다.</p>

      <h2>9. 약관 변경</h2>
      <p>약관을 바꾸면 버전을 올리며, 바뀐 약관에 동의해야 새 글을 게시할 수 있습니다.</p>

      @if (auth.user(); as u) {
        <div class="accept">
          @if (u.termsAccepted) {
            <span class="sf-muted"><mat-icon inline>check_circle</mat-icon> 현재 약관에 동의했습니다.</span>
          } @else {
            <button mat-flat-button [disabled]="busy()" (click)="accept()">약관에 동의합니다</button>
          }
        </div>
      }
    </div>
  `,
  styles: `
    .narrow {
      max-width: 760px;
    }
    h1 {
      margin: 0 0 8px;
      font-size: 24px;
    }
    h2 {
      margin: 20px 0 6px;
      font-size: 16px;
    }
    p,
    li {
      line-height: 1.75;
      font-size: 14px;
    }
    p {
      margin: 0 0 8px;
    }
    .draft {
      padding: 8px 12px;
      border-left: 3px solid var(--sf-severity-medium);
      background: var(--sf-surface-2);
      border-radius: 6px;
    }
    .accept {
      margin-top: 24px;
    }
  `,
})
export class TermsPage {
  protected readonly auth = inject(AuthService);
  private readonly api = inject(CommunityApi);
  private readonly snack = inject(MatSnackBar);
  protected readonly busy = signal(false);

  protected accept(): void {
    this.busy.set(true);
    this.api.acceptTerms().subscribe({
      next: (u) => {
        this.busy.set(false);
        this.auth.setUser(u);
        this.snack.open('동의했습니다.', undefined, { duration: 2000 });
      },
      error: (e) => {
        this.busy.set(false);
        this.snack.open(errorMessage(e), '닫기', { duration: 4000 });
      },
    });
  }
}
