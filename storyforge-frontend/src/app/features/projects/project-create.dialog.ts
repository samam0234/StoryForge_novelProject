import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { GENRES } from '../../core/models/community';
import { ProjectForm } from '../../core/models/models';

export interface ProjectCreateData {
  form?: ProjectForm;
  genre?: string;
}

/** 프로젝트 생성 폼. 결과({title, genre, form})를 닫을 때 돌려줍니다. */
@Component({
  selector: 'sf-project-create-dialog',
  imports: [FormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatButtonToggleModule],
  template: `
    <h2 mat-dialog-title>새 프로젝트</h2>
    <mat-dialog-content class="sf-form">
      <mat-button-toggle-group class="form-toggle" hideSingleSelectionIndicator [(ngModel)]="form" aria-label="분량">
        <mat-button-toggle value="LONG">장편</mat-button-toggle>
        <mat-button-toggle value="SHORT">단편</mat-button-toggle>
      </mat-button-toggle-group>
      <mat-form-field appearance="outline">
        <mat-label>제목</mat-label>
        <input matInput [(ngModel)]="title" maxlength="200" required cdkFocusInitial />
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>장르</mat-label>
        <input matInput [(ngModel)]="genre" maxlength="100" placeholder="예: 판타지, 미스터리" list="sf-create-genres" />
        <datalist id="sf-create-genres">
          @for (g of genres; track g) {
            <option [value]="g"></option>
          }
        </datalist>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>취소</button>
      <button mat-flat-button [disabled]="!title.trim()" (click)="submit()">만들기</button>
    </mat-dialog-actions>
  `,
  styles: `
    .form-toggle {
      align-self: flex-start;
      margin: 4px 0 12px;
    }
  `,
})
export class ProjectCreateDialog {
  private readonly ref = inject(MatDialogRef<ProjectCreateDialog>);
  private readonly data = inject<ProjectCreateData | null>(MAT_DIALOG_DATA, { optional: true });
  protected readonly genres = GENRES;
  protected title = '';
  protected genre = this.data?.genre ?? '';
  protected form: ProjectForm = this.data?.form ?? 'LONG';

  protected submit(): void {
    this.ref.close({ title: this.title.trim(), genre: this.genre.trim() || null, form: this.form });
  }
}
