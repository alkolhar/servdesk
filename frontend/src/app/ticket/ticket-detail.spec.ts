import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { translocoTesting } from '../i18n/testing';
import { TicketDetail } from './ticket-detail';
import { Ticket, TicketService } from './ticket.service';

function incidentIn(status: string, taskKey: string | null, actions: string[]): Ticket {
  const links: Record<string, { href: string }> = {
    self: { href: 'http://localhost/api/incidents/7' },
  };
  for (const action of actions) {
    links['action:' + action] = { href: `http://localhost/api/tickets/7/actions/${action}` };
  }
  return {
    id: 7,
    displayNumber: 'INC-000007',
    status: status as Ticket['status'],
    subject: 'Printer on fire',
    requesterId: 2,
    createdAt: '2026-09-30T10:00:00Z',
    tasks: taskKey ? [{ key: taskKey, createdAt: '2026-09-30T10:00:00Z' }] : [],
    _links: links,
  } as Ticket;
}

describe('TicketDetail', () => {
  const incident = vi.fn<TicketService['ticket']>();
  const perform = vi.fn<TicketService['perform']>();
  const follow = vi.fn<TicketService['follow']>();
  let dialogResult: string | undefined;

  beforeEach(async () => {
    incident.mockReset();
    perform.mockReset();
    follow.mockReset().mockRejectedValue(new Error('no names in this test'));
    dialogResult = undefined;
    await TestBed.configureTestingModule({
      imports: [TicketDetail, translocoTesting()],
      providers: [
        { provide: TicketService, useValue: { ticket: incident, perform, follow } },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { data: { path: 'incidents' } },
            paramMap: of(convertToParamMap({ id: '7' })),
          },
        },
        {
          provide: MatDialog,
          useValue: { open: () => ({ afterClosed: () => of(dialogResult) }) },
        },
      ],
    }).compileComponents();
  });

  async function render(): Promise<{ element: HTMLElement; settle: () => Promise<void> }> {
    const fixture = TestBed.createComponent(TicketDetail);
    const settle = async () => {
      await fixture.whenStable();
      fixture.detectChanges();
      await fixture.whenStable();
    };
    await settle();
    return { element: fixture.nativeElement as HTMLElement, settle };
  }

  function buttons(element: HTMLElement): string[] {
    return Array.from(element.querySelectorAll<HTMLButtonElement>('[data-action]')).map(
      (button) => button.dataset['action']!,
    );
  }

  it('shows the Incident, what it waits on, and one button per offered action', async () => {
    incident.mockResolvedValue(incidentIn('OPEN', 'triage', ['start-work', 'cancel']));

    const { element } = await render();

    expect(element.querySelector('[data-testid="display-number"]')?.textContent).toContain(
      'INC-000007',
    );
    expect(element.querySelector('[data-testid="status"]')?.textContent).toContain('Open');
    expect(element.querySelector('[data-testid="task"]')?.textContent).toContain('Triage');
    expect(buttons(element)).toEqual(['start-work', 'cancel']);
  });

  it('shows no buttons when the server offers no actions', async () => {
    incident.mockResolvedValue(incidentIn('CLOSED', null, []));

    const { element } = await render();

    expect(buttons(element)).toEqual([]);
    expect(element.querySelector('[data-testid="task"]')?.textContent).toContain('closed');
  });

  it('performs an action that needs no comment straight away, then reloads', async () => {
    incident
      .mockResolvedValueOnce(incidentIn('OPEN', 'triage', ['start-work', 'cancel']))
      .mockResolvedValue(incidentIn('IN_PROGRESS', 'work', ['resolve', 'put-on-hold', 'cancel']));
    perform.mockResolvedValue();
    const { element, settle } = await render();

    element.querySelector<HTMLButtonElement>('[data-action="start-work"]')!.click();
    await settle();

    expect(perform).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'start-work' }),
      undefined,
    );
    expect(element.querySelector('[data-testid="status"]')?.textContent).toContain('In progress');
    expect(buttons(element)).toEqual(['resolve', 'put-on-hold', 'cancel']);
  });

  it('asks for the resolution note before resolving', async () => {
    incident.mockResolvedValue(incidentIn('IN_PROGRESS', 'work', ['resolve']));
    perform.mockResolvedValue();
    dialogResult = 'Toner replaced';
    const { element, settle } = await render();

    element.querySelector<HTMLButtonElement>('[data-action="resolve"]')!.click();
    await settle();

    expect(perform).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'resolve' }),
      'Toner replaced',
    );
  });

  it('does nothing when the comment prompt is backed out of', async () => {
    incident.mockResolvedValue(incidentIn('IN_PROGRESS', 'work', ['cancel']));
    dialogResult = undefined;
    const { element, settle } = await render();

    element.querySelector<HTMLButtonElement>('[data-action="cancel"]')!.click();
    await settle();

    expect(perform).not.toHaveBeenCalled();
  });

  it('reloads when someone else moved the ticket on meanwhile', async () => {
    incident
      .mockResolvedValueOnce(incidentIn('OPEN', 'triage', ['start-work']))
      .mockResolvedValue(incidentIn('IN_PROGRESS', 'work', ['resolve']));
    perform.mockRejectedValue(new HttpErrorResponse({ status: 409 }));
    const { element, settle } = await render();

    element.querySelector<HTMLButtonElement>('[data-action="start-work"]')!.click();
    await settle();

    expect(buttons(element)).toEqual(['resolve']);
  });

  it('says so when the Incident does not exist', async () => {
    incident.mockRejectedValue(new HttpErrorResponse({ status: 404 }));

    const { element } = await render();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('no such ticket');
  });

  it('asks for the resource its route names', async () => {
    incident.mockResolvedValue(incidentIn('OPEN', 'triage', []));

    await render();

    expect(incident).toHaveBeenCalledWith('incidents', 7);
  });

  it('shows no task panel for a subtype not on a lifecycle process yet', async () => {
    const problem = incidentIn('OPEN', null, []);
    delete problem.tasks;
    incident.mockResolvedValue(problem);

    const { element } = await render();

    expect(element.querySelector('[data-testid="task-panel"]')).toBeNull();
    expect(element.querySelector('[data-testid="status"]')?.textContent).toContain('Open');
  });
});
