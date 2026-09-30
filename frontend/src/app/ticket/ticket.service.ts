import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ActionRequest, HalLink, HalLinks, IncidentModel, TicketTaskModel } from '../api/models';
import { SubtypePath } from './subtypes';

/** The link relation prefix of a ticket action (ADR-0008): `action:resolve`, ... */
export const ACTION_REL_PREFIX = 'action:';

/**
 * Actions whose request must carry a comment (ADR-0008). The server enforces it and answers 400
 * without one; knowing it here only means the UI asks before sending.
 */
export const COMMENT_REQUIRED = new Set(['resolve', 'cancel', 'reject']);

export interface TicketAction {
  name: string;
  link: HalLink;
}

/** The actions a ticket model offers this caller now, in the server's order. */
export function actionsOf(links: HalLinks | undefined): TicketAction[] {
  return Object.entries(links ?? {})
    .filter(([rel]) => rel.startsWith(ACTION_REL_PREFIX))
    .map(([rel, link]) => ({ name: rel.slice(ACTION_REL_PREFIX.length), link }));
}

/**
 * HAL hrefs are absolute, but HttpClient only sends the X-XSRF-TOKEN header on *relative* URLs —
 * followed as-is, a session-authenticated action would be refused with 403. Links always point at
 * this same origin, so their path is all that's needed.
 */
export function pathOf(href: string): string {
  const url = new URL(href, window.location.origin);
  return url.pathname + url.search;
}

/**
 * Any subtype's model: the shared fields every subtype has (IncidentModel's, minus its own
 * `relatedProblemId`), plus `tasks` for a subtype on its lifecycle process.
 */
export type Ticket = Omit<IncidentModel, 'tasks' | 'relatedProblemId'> & {
  tasks?: TicketTaskModel[];
  relatedProblemId?: number | null;
  _links?: HalLinks;
};

@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly http = inject(HttpClient);

  ticket(path: SubtypePath, id: number): Promise<Ticket> {
    return firstValueFrom(this.http.get<Ticket>(`/api/${path}/${id}`));
  }

  /** Creates a ticket of the subtype at `path`. Rejects with the HttpErrorResponse (400: see `errors`). */
  create(path: SubtypePath, request: object): Promise<Ticket> {
    return firstValueFrom(this.http.post<Ticket>(`/api/${path}`, request));
  }

  /** GETs whatever a link points at (a requester, a priority, ...). */
  follow<T>(link: HalLink): Promise<T> {
    return firstValueFrom(this.http.get<T>(pathOf(link.href)));
  }

  /** Rejects with the HttpErrorResponse: 400 missing comment, 409 not available (any more). */
  async perform(action: TicketAction, comment?: string): Promise<void> {
    const body: ActionRequest | null = comment ? { comment } : null;
    await firstValueFrom(this.http.post<void>(pathOf(action.link.href), body));
  }
}
