import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { POST_CATEGORIES, PostCategory } from '../../core/models/community';

export interface PostEditData {
  category: PostCategory;
  title: string;
  content: string;
}

/** 라운지 글 작성·수정. 결과를 닫을 때 돌려줍니다. */
@Component({
  selector: 'sf-post-edit-dialog',
  imports: [FormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>{{ data ? '글 수정' : '글쓰기' }}</h2>
    <mat-dialog-content class="sf-form body">
      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>분류</mat-label>
        <mat-select [(ngModel)]="category" name="category">
          @for (c of categories; track c.value) {
            <mat-option [value]="c.value">{{ c.label }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>제목</mat-label>
        <input matInput [(ngModel)]="title" name="title" maxlength="200" required />
      </mat-form-field>
      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>내용</mat-label>
        <textarea matInput [(ngModel)]="content" name="content" rows="10" maxlength="20000" required></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>취소</button>
      <button mat-flat-button [disabled]="!title.trim() || !content.trim()" (click)="submit()">저장</button>
    </mat-dialog-actions>
  `,
  styles: `
    .body {
      gap: 12px;
      padding-top: 8px;
    }
  `,
})
export class PostEditDialog {
  protected readonly data = inject<PostEditData | null>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<PostEditDialog>);
  protected readonly categories = POST_CATEGORIES;
  protected category: PostCategory = this.data?.category ?? 'FREE';
  protected title = this.data?.title ?? '';
  protected content = this.data?.content ?? '';

  protected submit(): void {
    this.ref.close({ category: this.category, title: this.title.trim(), content: this.content.trim() });
  }
}
