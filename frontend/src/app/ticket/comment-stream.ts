import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import {
  FormGroupDirective,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';
import { CommentModel } from '../api/models';
import { TicketService } from './ticket.service';

/**
 * A ticket's comment stream (#119): oldest first, internal notes marked, and a box to add one.
 * Comments an action wrote (a resolution note, a cancel reason) are ordinary comments, so they
 * appear here like any other — the ticket screen bumps `reloadKey` after each action to fetch them.
 */
@Component({
  selector: 'app-comment-stream',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    TranslocoDirective,
  ],
  templateUrl: './comment-stream.html',
  styleUrl: './comment-stream.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CommentStream {
  private readonly tickets = inject(TicketService);
  private readonly transloco = inject(TranslocoService);

  readonly ticketId = input.required<number>();
  /** Any change reloads the stream: the ticket screen bumps it after an action. */
  readonly reloadKey = input(0);

  protected readonly comments = signal<CommentModel[]>([]);
  protected readonly authors = signal<ReadonlyMap<number, string>>(new Map());
  protected readonly loadFailed = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);

  protected readonly form = inject(NonNullableFormBuilder).group({
    body: ['', [Validators.required, Validators.pattern(/\S/)]],
    internal: [false],
  });

  constructor() {
    effect(() => {
      const ticketId = this.ticketId();
      this.reloadKey();
      untracked(() => void this.load(ticketId));
    });
  }

  private async load(ticketId: number): Promise<void> {
    try {
      const comments = await this.tickets.comments(ticketId);
      this.comments.set(comments);
      this.loadFailed.set(false);
      await this.loadAuthors(comments);
    } catch {
      this.loadFailed.set(true);
    }
  }

  /** Each author's name, fetched once; one that can't be loaded just isn't named. */
  private async loadAuthors(comments: CommentModel[]): Promise<void> {
    const known = this.authors();
    const missing = [...new Set(comments.map((comment) => comment.authorId))].filter(
      (id): id is number => id != null && !known.has(id),
    );
    if (!missing.length) {
      return;
    }
    const results = await Promise.allSettled(missing.map((id) => this.tickets.person(id)));
    const names = new Map(known);
    results.forEach((result, index) => {
      if (result.status === 'fulfilled' && result.value.name) {
        names.set(missing[index], result.value.name);
      }
    });
    this.authors.set(names);
  }

  protected authorOf(comment: CommentModel): string {
    return (
      (comment.authorId != null ? this.authors().get(comment.authorId) : undefined) ??
      this.transloco.translate('ticket.comments.unknownAuthor')
    );
  }

  protected formatDateTime(iso: string | undefined): string {
    return iso
      ? new Intl.DateTimeFormat(this.transloco.getActiveLang(), {
          dateStyle: 'medium',
          timeStyle: 'short',
        }).format(new Date(iso))
      : '';
  }

  protected async add(formDirective: FormGroupDirective): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const { body, internal } = this.form.getRawValue();
    try {
      await this.tickets.addComment(this.ticketId(), body.trim(), internal);
      // through the directive, so the emptied box isn't flagged as a submitted, missing field;
      // the internal toggle stays as it was — several notes in a row are usually all one kind
      formDirective.resetForm({ body: '', internal });
      await this.load(this.ticketId());
    } catch {
      this.error.set('ticket.comments.error.add');
    } finally {
      this.submitting.set(false);
    }
  }
}
