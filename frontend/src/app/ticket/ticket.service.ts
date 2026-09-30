import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ActionRequest, HalLink, HalLinks, IncidentModel } from '../api/models';

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

export type Incident = IncidentModel & { _links?: HalLinks };

@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly http = inject(HttpClient);

  incident(id: number): Promise<Incident> {
    return firstValueFrom(this.http.get<Incident>(`/api/incidents/${id}`));
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
