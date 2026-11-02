import { TestBed } from '@angular/core/testing';
import { PIPELINE_STAGES, ProjectStatus, stageIndex, stageLabel } from '../../core/models/models';
import { PipelineStepperComponent } from './pipeline-stepper/pipeline-stepper.component';
import { RichTextEditorComponent } from './rich-text-editor/rich-text-editor.component';

describe('PIPELINE_STAGES', () => {
  it('기획서 §7.2의 상태 순서를 유지한다', () => {
    expect(PIPELINE_STAGES.map((s) => s.status)).toEqual([
      'BIBLE_DRAFT',
      'BIBLE_CONFIRMED',
      'OUTLINE_DRAFT',
      'OUTLINE_CONFIRMED',
      'CHAPTER_GENERATING',
      'CHAPTER_REVIEW',
      'COMPLETED',
    ]);
    expect(stageIndex('COMPLETED')).toBe(6);
    expect(stageLabel('BIBLE_DRAFT')).toBe('Bible 작성');
  });
});

describe('PipelineStepperComponent', () => {
  function render(status: ProjectStatus) {
    const fixture = TestBed.createComponent(PipelineStepperComponent);
    fixture.componentRef.setInput('status', status);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('현재 단계와 지난 단계를 표시한다', () => {
    const el = render('OUTLINE_DRAFT');
    expect(el.querySelectorAll('li.done').length).toBe(2);
    expect(el.querySelector('li.current')?.textContent).toContain('Outline 작성');
  });

  it('완료 단계에는 체크를 표시한다', () => {
    expect(render('COMPLETED').querySelector('li.current mat-icon')?.textContent).toContain('check');
  });
});

describe('RichTextEditorComponent', () => {
  async function render(value: string, editable = true) {
    const fixture = TestBed.createComponent(RichTextEditorComponent);
    fixture.componentRef.setInput('value', value);
    fixture.componentRef.setInput('editable', editable);
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture;
  }

  it('Markdown을 문단과 서식으로 렌더링한다', async () => {
    const fixture = await render('첫 문단\n\n**굵은** 문단');
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelectorAll('.ProseMirror p').length).toBe(2);
    expect(el.querySelector('.ProseMirror strong')?.textContent).toBe('굵은');
    fixture.destroy();
  });

  it('입력 값이 바뀌면 내용을 교체하되 valueChange를 내보내지 않는다', async () => {
    const fixture = await render('처음');
    const emitted: string[] = [];
    fixture.componentInstance.valueChange.subscribe((v) => emitted.push(v));
    fixture.componentRef.setInput('value', '바뀐 내용');
    fixture.detectChanges();
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).querySelector('.ProseMirror')?.textContent).toContain('바뀐 내용');
    expect(emitted).toEqual([]);
    fixture.destroy();
  });

  it('읽기 전용이면 툴바를 숨기고 편집할 수 없다', async () => {
    const fixture = await render('내용', false);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.sf-rte-toolbar')).toBeNull();
    expect(el.querySelector('.ProseMirror')?.getAttribute('contenteditable')).toBe('false');
    fixture.destroy();
  });
});
