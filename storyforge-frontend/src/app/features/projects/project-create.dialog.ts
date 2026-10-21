import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

/** 프로젝트 생성 폼. 결과({title, genre})를 닫을 때 돌려줍니다. */
@Component({
  selector: 'sf-project-create-dialog',
  imports: [FormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>새 프로젝트</h2>
    <mat-dialog-content class="sf-form">
      <mat-form-field appearance="outline">
        <mat-label>제목</mat-label>
        <input matInput [(ngModel)]="title" maxlength="200" required cdkFocusInitial />
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>장르</mat-label>
        <input matInput [(ngModel)]="genre" maxlength="100" placeholder="예: 판타지, 미스터리" />
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>취소</button>
      <button mat-flat-button [disabled]="!title.trim()" (click)="submit()">만들기</button>
    </mat-dialog-actions>
  `,
})
export class ProjectCreateDialog {
  private readonly ref = inject(MatDialogRef<ProjectCreateDialog>);
  protected title = '';
  protected genre = '';

  protected submit(): void {
    this.ref.close({ title: this.title.trim(), genre: this.genre.trim() || null });
  }
}
