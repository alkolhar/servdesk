import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { PersonModel, PriorityModel } from '../api/models';
import { SubtypePath } from './subtypes';
import { CommentDialog, CommentDialogData } from './comment-dialog';
import { COMMENT_REQUIRED, Ticket, TicketAction, TicketService, actionsOf } from './ticket.service';

type LoadState = 'loading' | 'loaded' | 'notFound' | 'unavailable';

/**
 * One ticket of any subtype (the route's `path` data says which resource): its fields, what it's
 * waiting on, and one button per action the server offers this caller now (ADR-0003: no link, no
 * button — the UI never decides what's allowed). The task panel only shows for a subtype whose
 * model carries `tasks`, i.e. one that runs on its lifecycle process.
 */
@Component({
  selector: 'app-ticket-detail',
  imports: [MatButtonModule, MatCardModule, TranslocoDirective],
  templateUrl: './ticket-detail.html',
  styleUrl: './ticket-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TicketDetail {
  private readonly tickets = inject(TicketService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly transloco = inject(TranslocoService);

  protected readonly state = signal<LoadState>('loading');
  protected readonly ticket = signal<Ticket | null>(null);
  protected readonly requester = signal<string | null>(null);
  protected readonly priority = signal<string | null>(null);
  protected readonly busy = signal(false);

  protected readonly actions = computed(() => actionsOf(this.ticket()?._links));
  /** Whether this subtype runs on a lifecycle process at all (its model has `tasks`). */
  protected readonly hasLifecycle = computed(() => Array.isArray(this.ticket()?.tasks));
  protected readonly task = computed(() => this.ticket()?.tasks?.[0] ?? null);

  private id = 0;
  private readonly path: SubtypePath;

  constructor() {
    const route = inject(ActivatedRoute);
    this.path = (route.snapshot?.data?.['path'] as SubtypePath | undefined) ?? 'incidents';
    route.paramMap.subscribe((params) => {
      this.id = Number(params.get('id'));
      void this.load();
    });
  }

  protected async load(): Promise<void> {
    try {
      const ticket = await this.tickets.ticket(this.path, this.id);
      this.ticket.set(ticket);
      this.state.set('loaded');
      await this.loadNames(ticket);
    } catch (error) {
      this.state.set(
        error instanceof HttpErrorResponse && error.status === 404 ? 'notFound' : 'unavailable',
      );
    }
  }

  /** Names for the ids the model carries; a failure here only leaves the id showing. */
  private async loadNames(ticket: Ticket): Promise<void> {
    const requesterLink = ticket._links?.['requester'];
    const [requester, priority] = await Promise.allSettled([
      requesterLink ? this.tickets.follow<PersonModel>(requesterLink) : Promise.reject(),
      ticket.priorityId != null
        ? this.tickets.follow<PriorityModel>({ href: `/api/priorities/${ticket.priorityId}` })
        : Promise.reject(),
    ]);
    this.requester.set(requester.status === 'fulfilled' ? (requester.value.name ?? null) : null);
    this.priority.set(priority.status === 'fulfilled' ? (priority.value.name ?? null) : null);
  }

  protected async perform(action: TicketAction): Promise<void> {
    let comment: string | undefined;
    if (COMMENT_REQUIRED.has(action.name)) {
      comment = await firstValueFrom(
        this.dialog
          .open<CommentDialog, CommentDialogData, string>(CommentDialog, {
            data: { action: action.name },
            width: 'min(520px, 90vw)',
          })
          .afterClosed(),
      );
      if (!comment) {
        return; // backed out
      }
    }
    this.busy.set(true);
    try {
      await this.tickets.perform(action, comment);
      this.notify('ticket.done.' + action.name);
    } catch (error) {
      const status = error instanceof HttpErrorResponse ? error.status : 0;
      // 409: someone moved the ticket on meanwhile; the reload below shows where it is now
      this.notify(
        status === 409
          ? 'ticket.error.movedOn'
          : status === 400
            ? 'ticket.error.commentRequired'
            : 'ticket.error.unavailable',
      );
    } finally {
      await this.load();
      this.busy.set(false);
    }
  }

  protected formatDateTime(iso: string | null | undefined): string {
    if (!iso) {
      return '—';
    }
    return new Intl.DateTimeFormat(this.transloco.getActiveLang(), {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(iso));
  }

  private notify(key: string): void {
    this.snackBar.open(this.transloco.translate(key), undefined, { duration: 4000 });
  }
}
