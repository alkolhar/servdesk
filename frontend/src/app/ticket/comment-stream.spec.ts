import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { CommentModel, PersonModel } from '../api/models';
import { translocoTesting } from '../i18n/testing';
import { CommentStream } from './comment-stream';
import { TicketService } from './ticket.service';

@Component({
  imports: [CommentStream],
  template: `<app-comment-stream [ticketId]="7" [reloadKey]="version()" />`,
})
class Host {
  readonly version = signal(0);
}

const comment = (id: number, authorId: number, body: string, internal = false): CommentModel =>
  ({
    id,
    ticketId: 7,
    authorId,
    body,
    internal,
    createdAt: `2026-09-30T10:0${id}:00Z`,
  }) as CommentModel;

describe('CommentStream', () => {
  const comments = vi.fn<TicketService['comments']>();
  const addComment = vi.fn<TicketService['addComment']>();
  const person = vi.fn<TicketService['person']>();

  beforeEach(async () => {
    comments.mockReset();
    addComment.mockReset();
    person
      .mockReset()
      .mockImplementation(async (id: number) =>
        id === 1
          ? ({ id: 1, name: 'Ada Admin' } as PersonModel)
          : ({ id: 2, name: 'Carla Customer' } as PersonModel),
      );
    await TestBed.configureTestingModule({
      imports: [Host, translocoTesting()],
      providers: [{ provide: TicketService, useValue: { comments, addComment, person } }],
    }).compileComponents();
  });

  async function render() {
    const fixture = TestBed.createComponent(Host);
    const settle = async () => {
      await fixture.whenStable();
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
    };
    await settle();
    const element = fixture.nativeElement as HTMLElement;
    const shown = () =>
      Array.from(element.querySelectorAll('[data-testid="comment"]')).map((li) => ({
        text: li.querySelector('.body')?.textContent?.trim(),
        internal: li.getAttribute('data-internal') === 'true',
        author: li.querySelector('.author')?.textContent?.trim(),
      }));
    return { fixture, element, settle, shown };
  }

  it('shows the stream in order, with internal notes marked and authors named', async () => {
    comments.mockResolvedValue([
      comment(1, 2, 'The printer is on fire'),
      comment(2, 1, 'Called facilities', true),
      comment(3, 1, 'Toner replaced'),
    ]);

    const { element, shown } = await render();

    expect(shown()).toEqual([
      { text: 'The printer is on fire', internal: false, author: 'Carla Customer' },
      { text: 'Called facilities', internal: true, author: 'Ada Admin' },
      { text: 'Toner replaced', internal: false, author: 'Ada Admin' },
    ]);
    expect(element.querySelectorAll('.badge')).toHaveLength(1);
    expect(element.querySelector('.badge')?.textContent).toContain('Internal');
    // each author fetched once, however many comments they wrote
    expect(person).toHaveBeenCalledTimes(2);
  });

  it('says so when there are no comments yet', async () => {
    comments.mockResolvedValue([]);

    const { element } = await render();

    expect(element.querySelector('[data-testid="no-comments"]')?.textContent).toContain(
      'No comments yet.',
    );
  });

  it('adds an internal note and shows it straight away', async () => {
    comments
      .mockResolvedValueOnce([comment(1, 2, 'The printer is on fire')])
      .mockResolvedValue([
        comment(1, 2, 'The printer is on fire'),
        comment(2, 1, 'Ordered toner', true),
      ]);
    addComment.mockResolvedValue(comment(2, 1, 'Ordered toner', true));
    const { element, settle, shown } = await render();

    const textarea = element.querySelector<HTMLTextAreaElement>('[data-field="comment"]')!;
    textarea.value = '  Ordered toner  ';
    textarea.dispatchEvent(new Event('input'));
    element.querySelector<HTMLElement>('[data-field="internal"] button')!.click();
    await settle();
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await settle();

    expect(addComment).toHaveBeenCalledWith(7, 'Ordered toner', true);
    expect(shown().map((c) => c.text)).toEqual(['The printer is on fire', 'Ordered toner']);
    expect(textarea.value).toBe('');
    // the emptied box isn't flagged as a missing field, and the toggle kept its value
    expect(element.querySelector('mat-error')).toBeNull();
    expect(
      element.querySelector('[data-field="internal"] button')!.getAttribute('aria-checked'),
    ).toBe('true');
  });

  it('does not send an empty comment', async () => {
    comments.mockResolvedValue([]);
    const { element, settle } = await render();

    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await settle();

    expect(addComment).not.toHaveBeenCalled();
  });

  it('reloads when the ticket screen says an action happened', async () => {
    comments
      .mockResolvedValueOnce([])
      .mockResolvedValue([comment(1, 1, 'Resolution: toner replaced')]);
    const { fixture, settle, shown } = await render();

    fixture.componentInstance.version.set(1);
    await settle();

    expect(shown().map((c) => c.text)).toEqual(['Resolution: toner replaced']);
  });
});
