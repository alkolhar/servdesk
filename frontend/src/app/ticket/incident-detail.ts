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
import { CommentDialog, CommentDialogData } from './comment-dialog';
import {
  COMMENT_REQUIRED,
  Incident,
  TicketAction,
  TicketService,
  actionsOf,
} from './ticket.service';

type LoadState = 'loading' | 'loaded' | 'notFound' | 'unavailable';

/**
 * One Incident: its fields, what it's waiting on, and one button per action the server offers
 * this caller now (ADR-0003: no link, no button — the UI never decides what's allowed).
 */
@Component({
  selector: 'app-incident-detail',
  imports: [MatButtonModule, MatCardModule, TranslocoDirective],
  templateUrl: './incident-detail.html',
  styleUrl: './incident-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IncidentDetail {
  private readonly tickets = inject(TicketService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly transloco = inject(TranslocoService);

  protected readonly state = signal<LoadState>('loading');
  protected readonly incident = signal<Incident | null>(null);
  protected readonly requester = signal<string | null>(null);
  protected readonly priority = signal<string | null>(null);
  protected readonly busy = signal(false);

  protected readonly actions = computed(() => actionsOf(this.incident()?._links));
  protected readonly task = computed(() => this.incident()?.tasks?.[0] ?? null);

  private id = 0;

  constructor() {
    inject(ActivatedRoute).paramMap.subscribe((params) => {
      this.id = Number(params.get('id'));
      void this.load();
    });
  }

  protected async load(): Promise<void> {
    try {
      const incident = await this.tickets.incident(this.id);
      this.incident.set(incident);
      this.state.set('loaded');
      await this.loadNames(incident);
    } catch (error) {
      this.state.set(
        error instanceof HttpErrorResponse && error.status === 404 ? 'notFound' : 'unavailable',
      );
    }
  }

  /** Names for the ids the model carries; a failure here only leaves the id showing. */
  private async loadNames(incident: Incident): Promise<void> {
    const requesterLink = incident._links?.['requester'];
    const [requester, priority] = await Promise.allSettled([
      requesterLink ? this.tickets.follow<PersonModel>(requesterLink) : Promise.reject(),
      incident.priorityId != null
        ? this.tickets.follow<PriorityModel>({ href: `/api/priorities/${incident.priorityId}` })
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
