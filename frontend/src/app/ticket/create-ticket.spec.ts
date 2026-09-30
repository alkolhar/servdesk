import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { AttributeDefinitionModel, PersonModel } from '../api/models';
import { translocoTesting } from '../i18n/testing';
import { CreateTicket, messageKeyFor } from './create-ticket';
import { ReferenceDataService } from './reference-data.service';
import { TicketService } from './ticket.service';

const carla = { id: 2, name: 'Carla Customer', email: 'carla@example.com', role: 'CUSTOMER' };
const definitions: AttributeDefinitionModel[] = [
  {
    id: 1,
    target: 'TICKET',
    key: 'costCentre',
    label: 'Cost centre',
    type: 'STRING',
    required: true,
  },
  { id: 2, target: 'TICKET', key: 'seats', label: 'Seats', type: 'NUMBER', required: false },
  { id: 3, target: 'TICKET', key: 'vip', label: 'VIP', type: 'BOOLEAN', required: false },
  { id: 4, target: 'TICKET', key: 'due', label: 'Due', type: 'DATE', required: false },
  {
    id: 5,
    target: 'TICKET',
    key: 'site',
    label: 'Site',
    type: 'ENUM',
    required: false,
    enumValues: ['Zurich', 'Bern'],
  },
] as AttributeDefinitionModel[];

/** The component's form, reached past `protected` for set-up and assertions. */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const formOf = (fixture: { componentInstance: unknown }): any =>
  (fixture.componentInstance as { form: unknown }).form;

describe('CreateTicket', () => {
  const create = vi.fn<TicketService['create']>();

  beforeEach(async () => {
    create.mockReset();
    await TestBed.configureTestingModule({
      imports: [CreateTicket, translocoTesting()],
      providers: [
        provideRouter([]),
        { provide: TicketService, useValue: { create } },
        {
          provide: ReferenceDataService,
          useValue: {
            people: async () => [carla],
            agents: async () => [],
            categories: async () => [],
            impacts: async () => [],
            urgencies: async () => [],
            problems: async () => [],
            ticketAttributes: async () => definitions,
          },
        },
      ],
    }).compileComponents();
  });

  async function render() {
    const fixture = TestBed.createComponent(CreateTicket);
    const settle = async () => {
      await fixture.whenStable();
      fixture.detectChanges();
      await fixture.whenStable();
    };
    await settle();
    const element = fixture.nativeElement as HTMLElement;
    return { fixture, element, settle };
  }

  async function chooseAndFill(subtype: string) {
    const rendered = await render();
    rendered.element.querySelector<HTMLElement>(`[data-subtype="${subtype}"] button`)!.click();
    await rendered.settle();
    const form = formOf(rendered.fixture);
    form.controls.requester.setValue(carla as PersonModel);
    form.controls.subject.setValue('Printer on fire');
    form.controls.attributes.controls.costCentre.setValue('CC-42');
    form.controls.attributes.controls.seats.setValue('3');
    form.controls.attributes.controls.due.setValue('2026-10-31');
    form.controls.attributes.controls.site.setValue('Bern');
    await rendered.settle();
    return rendered;
  }

  it('shows the form only once a subtype is chosen', async () => {
    const { element, settle } = await render();
    expect(element.querySelector('form')).toBeNull();

    element.querySelector<HTMLElement>('[data-subtype="change"] button')!.click();
    await settle();

    expect(element.querySelector('form')).not.toBeNull();
    expect(element.querySelector('[data-attribute="costCentre"]')).not.toBeNull();
    expect(element.textContent).toContain('Create Change');
  });

  it('creates the ticket with typed custom fields and opens it', async () => {
    create.mockResolvedValue({ id: 41 } as never);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const { element, settle } = await chooseAndFill('incident');

    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await settle();

    expect(create).toHaveBeenCalledWith(
      'incidents',
      expect.objectContaining({
        subject: 'Printer on fire',
        requesterId: 2,
        relatedProblemId: null,
        attributes: { costCentre: 'CC-42', seats: 3, vip: false, due: '2026-10-31', site: 'Bern' },
      }),
    );
    expect(navigate).toHaveBeenCalledWith(['/', 'incidents', 41]);
  });

  it('only sends relatedProblemId for an Incident', async () => {
    create.mockResolvedValue({ id: 5 } as never);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const { element, settle } = await chooseAndFill('service-request');

    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await settle();

    expect(create.mock.calls[0][0]).toBe('service-requests');
    expect(create.mock.calls[0][1]).not.toHaveProperty('relatedProblemId');
  });

  it('refuses to send without a picked requester or a required custom field', async () => {
    const { element, settle, fixture } = await chooseAndFill('problem');
    const form = formOf(fixture);
    form.controls.requester.setValue('Car'); // typed, not picked
    form.controls.attributes.controls.costCentre.setValue(null);

    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await settle();

    expect(create).not.toHaveBeenCalled();
  });

  it('shows the server’s field errors next to their fields', async () => {
    create.mockRejectedValue(
      new HttpErrorResponse({
        status: 400,
        error: {
          errors: [
            { field: 'subject', code: 'NotBlank' },
            { field: 'attributes.site', code: 'not_allowed' },
          ],
        },
      }),
    );
    const { element, settle, fixture } = await chooseAndFill('incident');

    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await settle();

    const form = formOf(fixture);
    expect(form.controls.subject.errors).toEqual({ server: 'form.required' });
    expect(form.controls.attributes.controls.site.errors).toEqual({ server: 'form.invalid' });
    const fieldErrors = Array.from(element.querySelectorAll('mat-error')).map((e) => e.textContent);
    expect(fieldErrors.join('|')).toContain('Required');
    expect(fieldErrors.join('|')).toContain('Not a valid value');
  });

  it('falls back to a message above the button when the server names no field', async () => {
    create.mockRejectedValue(new HttpErrorResponse({ status: 400, error: { detail: 'Bad' } }));
    const { element, settle } = await chooseAndFill('incident');

    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await settle();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Please check the highlighted fields.',
    );
  });

  it('maps server codes to messages', () => {
    expect(messageKeyFor('NotBlank')).toBe('form.required');
    expect(messageKeyFor('required')).toBe('form.required');
    expect(messageKeyFor('Email')).toBe('form.email');
    expect(messageKeyFor('type')).toBe('form.invalid');
  });
});
