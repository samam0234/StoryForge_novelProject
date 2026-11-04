import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** 게시·Clone의 권리 관계. 가입할 때 동의합니다. */
@Component({
  selector: 'sf-terms-page',
  imports: [RouterLink],
  template: `
    <article class="sf-page terms">
      <h1>이용약관</h1>
      <p>StoryForge에 가입하면 아래 내용에 동의하는 것입니다. 공개 운영 전에 게시물 권리와 운영 기준을 정한 문서입니다.</p>

      <h2>계정</h2>
      <p>가입은 본인이 쓰는 이메일로 합니다. 비밀번호는 해시로만 저장합니다. 다른 사람의 계정이나 데이터에 접근하지 않습니다.</p>

      <h2>원고</h2>
      <p>프로젝트 안의 Story Bible, Outline, 챕터는 작성한 사람의 글입니다. 공개하지 않은 프로젝트는 주인과 초대를 수락한 참여자만 봅니다.</p>

      <h2>게시와 Clone</h2>
      <ul>
        <li>작품을 게시하면 그 공개 범위 안에서 다른 사람이 읽고, 작가가 허용한 경우 내려받을 수 있습니다.</li>
        <li>Clone은 원작 제목과 작성자 이름을 남긴 파생 원고입니다. 원작을 지워도 Clone에 남은 출처 이름은 유지됩니다.</li>
        <li>게시하거나 Clone된 글을 다른 곳에 옮길 때의 추가 허락은 이 약관이 대신하지 않습니다. 작성자에게 권리가 있습니다.</li>
      </ul>

      <h2>해서는 안 되는 것</h2>
      <ul>
        <li>타인의 권리를 침해하는 글, 불법 정보, 괴롭힘, 도배</li>
        <li>다른 사용자인 것처럼 추천·달성 기록을 부풀리는 행위</li>
      </ul>
      <p>이런 글은 신고할 수 있습니다. 관리자는 신고를 보고 공개 목록에서 숨길 수 있습니다. 숨겨도 작성자 본인의 화면에서는 자기 글을 확인할 수 있습니다.</p>

      <h2>외부 언어 모델</h2>
      <p>기본 생성은 서버 밖으로 원고를 보내지 않습니다. 운영자가 외부 언어 모델을 켜면, 생성 화면에 그 사실을 표시합니다. 그때 Story Bible, Outline, 이전 챕터 요약과 가이드 질문이 해당 제공자에게 전송됩니다.</p>

      <p><a routerLink="/login">로그인으로 돌아가기</a></p>
    </article>
  `,
  styles: `
    .terms {
      max-width: 720px;
    }
    h1 {
      margin: 0 0 12px;
      font-size: 24px;
    }
    h2 {
      margin: 24px 0 8px;
      font-size: 18px;
    }
    p,
    li {
      line-height: 1.6;
    }
  `,
})
export class TermsPage {}
