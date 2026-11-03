import {
  Component,
  DestroyRef,
  ElementRef,
  ViewEncapsulation,
  afterNextRender,
  effect,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Editor } from '@tiptap/core';
import { Markdown } from '@tiptap/markdown';
import StarterKit from '@tiptap/starter-kit';

interface ToolbarItem {
  icon: string;
  label: string;
  active?: (e: Editor) => boolean;
  run: (e: Editor) => void;
}

/**
 * TipTap 리치 텍스트 에디터 (기획서 §9.2). 입출력 형식은 Markdown입니다.
 * 저장 형식을 Markdown으로 정한 이유는 docs/trainings/2026-11-02_chapter-content-markdown.md를 보세요.
 */
@Component({
  selector: 'sf-rich-text-editor',
  imports: [MatButtonModule, MatIconModule, MatTooltipModule],
  encapsulation: ViewEncapsulation.None,
  template: `
    @if (editable()) {
      <div class="sf-rte-toolbar" role="toolbar" aria-label="서식">
        @for (item of items; track item.label) {
          <button
            mat-icon-button
            type="button"
            [class.is-active]="revision() >= 0 && isActive(item)"
            [matTooltip]="item.label"
            [attr.aria-label]="item.label"
            (click)="run(item)"
          >
            <mat-icon>{{ item.icon }}</mat-icon>
          </button>
        }
      </div>
    }
    <div #host class="sf-rte-content"></div>
  `,
  styles: `
    sf-rich-text-editor {
      display: flex;
      flex-direction: column;
      flex: 1;
      min-height: 0;
      border: 1px solid var(--sf-border);
      border-radius: var(--sf-radius);
      background: var(--sf-surface);
      overflow: hidden;
    }
    sf-rich-text-editor:focus-within {
      border-color: var(--sf-secondary);
    }
    .sf-rte-toolbar {
      display: flex;
      flex-wrap: wrap;
      gap: 2px;
      padding: 4px;
      border-bottom: 1px solid var(--sf-border);
    }
    .sf-rte-toolbar .is-active {
      background: color-mix(in srgb, var(--sf-secondary) 25%, transparent);
    }
    .sf-rte-content {
      flex: 1;
      min-height: 0;
      overflow-y: auto;
    }
    .sf-rte-content .ProseMirror {
      min-height: 100%;
      box-sizing: border-box;
      padding: 20px 24px;
      outline: none;
      line-height: 1.85;
      font-size: 16px;
      color: var(--sf-text);
    }
    .sf-rte-content .ProseMirror p {
      margin: 0 0 1em;
    }
    .sf-rte-content .ProseMirror blockquote {
      margin: 0 0 1em;
      padding-left: 12px;
      border-left: 3px solid var(--sf-border);
      color: var(--sf-text-muted);
    }
  `,
})
export class RichTextEditorComponent {
  /** Markdown 본문. 입력이 바뀌면 에디터 내용을 교체합니다. */
  readonly value = input('');
  readonly editable = input(true);
  /** 사용자가 편집하면 Markdown을 내보냅니다. */
  readonly valueChange = output<string>();

  private readonly host = viewChild.required<ElementRef<HTMLElement>>('host');
  private editor?: Editor;
  private lastMarkdown = '';
  /** 선택 위치가 바뀔 때 툴바 활성 표시를 다시 계산하기 위한 값 */
  protected readonly revision = signal(0);

  protected readonly items: ToolbarItem[] = [
    { icon: 'format_bold', label: '굵게', active: (e) => e.isActive('bold'), run: (e) => e.chain().focus().toggleBold().run() },
    { icon: 'format_italic', label: '기울임', active: (e) => e.isActive('italic'), run: (e) => e.chain().focus().toggleItalic().run() },
    {
      icon: 'title',
      label: '소제목',
      active: (e) => e.isActive('heading', { level: 2 }),
      run: (e) => e.chain().focus().toggleHeading({ level: 2 }).run(),
    },
    {
      icon: 'format_quote',
      label: '인용',
      active: (e) => e.isActive('blockquote'),
      run: (e) => e.chain().focus().toggleBlockquote().run(),
    },
    {
      icon: 'format_list_bulleted',
      label: '목록',
      active: (e) => e.isActive('bulletList'),
      run: (e) => e.chain().focus().toggleBulletList().run(),
    },
    { icon: 'undo', label: '실행 취소', run: (e) => e.chain().focus().undo().run() },
    { icon: 'redo', label: '다시 실행', run: (e) => e.chain().focus().redo().run() },
  ];

  constructor() {
    afterNextRender(() => this.create());
    inject(DestroyRef).onDestroy(() => this.editor?.destroy());

    effect(() => {
      const value = this.value();
      if (this.editor && value !== this.lastMarkdown) {
        this.lastMarkdown = value;
        this.editor.commands.setContent(value, { contentType: 'markdown', emitUpdate: false });
      }
    });
    effect(() => this.editor?.setEditable(this.editable(), false));
  }

  protected isActive(item: ToolbarItem): boolean {
    return !!this.editor && !!item.active?.(this.editor);
  }

  protected run(item: ToolbarItem): void {
    if (this.editor) item.run(this.editor);
  }

  private create(): void {
    this.lastMarkdown = this.value();
    this.editor = new Editor({
      element: this.host().nativeElement,
      extensions: [StarterKit, Markdown],
      content: this.lastMarkdown,
      contentType: 'markdown',
      editable: this.editable(),
      onUpdate: ({ editor }) => {
        this.lastMarkdown = editor.getMarkdown();
        this.valueChange.emit(this.lastMarkdown);
      },
      onTransaction: () => this.revision.update((r) => r + 1),
    });
  }
}
