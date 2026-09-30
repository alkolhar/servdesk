import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import {
  AttributeDefinitionModel,
  CategoryModel,
  FieldProblem,
  ImpactModel,
  PersonModel,
  ProblemModel,
  UrgencyModel,
} from '../api/models';
import { ReferenceDataService } from './reference-data.service';
import { SUBTYPES, Subtype } from './subtypes';
import { TicketService } from './ticket.service';

/** A person picked from the autocomplete; typing free text leaves the control a string. */
function isPerson(value: unknown): value is PersonModel {
  return typeof value === 'object' && value !== null && 'id' in value;
}

function personPicked(control: AbstractControl): ValidationErrors | null {
  return isPerson(control.value) ? null : { required: true };
}

/** Maps a server error code (a constraint name or a FieldRejectedException code) to a message key. */
export function messageKeyFor(code: string): string {
  switch (code) {
    case 'NotBlank':
    case 'NotNull':
    case 'required':
      return 'form.required';
    case 'Email':
      return 'form.email';
    default:
      return 'form.invalid';
  }
}

/**
 * Create a ticket of any subtype (#97, #118): pick the subtype first, then fill its form. Custom
 * fields come from the TICKET attribute definitions and are rendered by type; the server remains
 * the judge, and a 400's field errors are shown next to the fields they name.
 */
@Component({
  selector: 'app-create-ticket',
  imports: [
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    TranslocoDirective,
  ],
  templateUrl: './create-ticket.html',
  styleUrl: './create-ticket.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateTicket {
  private readonly tickets = inject(TicketService);
  private readonly referenceData = inject(ReferenceDataService);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly subtypes = SUBTYPES;
  protected readonly subtype = signal<Subtype | null>(null);

  protected readonly people = signal<PersonModel[]>([]);
  protected readonly agents = signal<PersonModel[]>([]);
  protected readonly categories = signal<CategoryModel[]>([]);
  protected readonly impacts = signal<ImpactModel[]>([]);
  protected readonly urgencies = signal<UrgencyModel[]>([]);
  protected readonly problems = signal<ProblemModel[]>([]);
  protected readonly attributeDefinitions = signal<AttributeDefinitionModel[]>([]);

  /** Translation keys for problems no single field owns (or the server's reply had no field). */
  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);

  protected readonly form = this.fb.group({
    requester: new FormControl<PersonModel | string>('', { validators: personPicked }),
    subject: ['', [Validators.required, Validators.pattern(/\S/)]],
    description: [''],
    categoryId: new FormControl<number | null>(null),
    impactId: new FormControl<number | null>(null),
    urgencyId: new FormControl<number | null>(null),
    assigneeId: new FormControl<number | null>(null),
    relatedProblemId: new FormControl<number | null>(null),
    attributes: new FormGroup<Record<string, AbstractControl>>({}),
  });

  /** The requester autocomplete, filtered by what's typed. */
  private readonly requesterQuery = signal('');
  protected readonly requesterOptions = computed(() => {
    const query = this.requesterQuery().trim().toLowerCase();
    return this.people()
      .filter(
        (person) =>
          !query ||
          person.name?.toLowerCase().includes(query) ||
          person.email?.toLowerCase().includes(query),
      )
      .slice(0, 50);
  });

  /** Categories shown as their path ("Hardware › Printers"), since they form a tree. */
  protected readonly categoryOptions = computed(() => {
    const byId = new Map(this.categories().map((category) => [category.id, category]));
    const pathOf = (category: CategoryModel): string => {
      const parent = category.parentId != null ? byId.get(category.parentId) : undefined;
      return parent ? `${pathOf(parent)} › ${category.name}` : (category.name ?? '');
    };
    return this.categories()
      .map((category) => ({ id: category.id, label: pathOf(category) }))
      .sort((a, b) => a.label.localeCompare(b.label));
  });

  constructor() {
    this.form.controls.requester.valueChanges.subscribe((value) =>
      this.requesterQuery.set(typeof value === 'string' ? value : ''),
    );
    void this.loadReferenceData();
  }

  protected displayPerson(person: PersonModel | string | null): string {
    return isPerson(person) ? `${person.name} (${person.email})` : (person ?? '');
  }

  protected choose(subtype: Subtype): void {
    this.subtype.set(subtype);
    this.error.set(null);
  }

  protected attributeControl(key: string): AbstractControl {
    return this.form.controls.attributes.controls[key];
  }

  private async loadReferenceData(): Promise<void> {
    const [people, agents, categories, impacts, urgencies, problems, definitions] =
      await Promise.allSettled([
        this.referenceData.people(),
        this.referenceData.agents(),
        this.referenceData.categories(),
        this.referenceData.impacts(),
        this.referenceData.urgencies(),
        this.referenceData.problems(),
        this.referenceData.ticketAttributes(),
      ]);
    const value = <T>(result: PromiseSettledResult<T[]>): T[] =>
      result.status === 'fulfilled' ? result.value : [];
    this.people.set(value(people));
    this.agents.set(value(agents));
    this.categories.set(value(categories));
    this.impacts.set(value(impacts));
    this.urgencies.set(value(urgencies));
    this.problems.set(value(problems));
    const attributeDefinitions = value(definitions);
    for (const definition of attributeDefinitions) {
      const validators = definition.required ? [Validators.required] : [];
      this.form.controls.attributes.addControl(
        definition.key!,
        new FormControl(definition.type === 'BOOLEAN' ? false : null, validators),
      );
    }
    this.attributeDefinitions.set(attributeDefinitions);
    if ([people, definitions].some((result) => result.status === 'rejected')) {
      this.error.set('create.error.unavailable');
    }
  }

  /** Only values that were given: an omitted attribute is "not set", as the server reads it. */
  private attributesPayload(): Record<string, unknown> {
    const payload: Record<string, unknown> = {};
    for (const definition of this.attributeDefinitions()) {
      const raw = this.attributeControl(definition.key!).value;
      if (raw === null || raw === '' || raw === undefined) {
        continue;
      }
      payload[definition.key!] = definition.type === 'NUMBER' ? Number(raw) : raw;
    }
    return payload;
  }

  protected async submit(): Promise<void> {
    const subtype = this.subtype();
    if (!subtype) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const value = this.form.getRawValue();
    const request = {
      subject: value.subject,
      description: value.description || null,
      requesterId: (value.requester as PersonModel).id,
      categoryId: value.categoryId,
      impactId: value.impactId,
      urgencyId: value.urgencyId,
      assigneeId: value.assigneeId,
      ...(subtype.type === 'incident' ? { relatedProblemId: value.relatedProblemId } : {}),
      attributes: this.attributesPayload(),
    };
    try {
      const created = await this.tickets.create(subtype.path, request);
      await this.router.navigate(['/', subtype.path, created.id]);
    } catch (error) {
      this.showServerErrors(error);
    } finally {
      this.submitting.set(false);
    }
  }

  /** A 400's field errors go onto their controls; anything else is a message above the button. */
  private showServerErrors(error: unknown): void {
    if (!(error instanceof HttpErrorResponse) || error.status !== 400) {
      this.error.set(
        error instanceof HttpErrorResponse && error.status === 409
          ? 'create.error.conflict'
          : 'create.error.unavailable',
      );
      return;
    }
    const problems: FieldProblem[] = error.error?.errors ?? [];
    let unplaced = problems.length === 0;
    for (const problem of problems) {
      const control = this.controlFor(problem.field);
      if (control) {
        control.setErrors({ server: messageKeyFor(problem.code) });
        control.markAsTouched();
      } else {
        unplaced = true;
      }
    }
    if (unplaced) {
      this.error.set('create.error.invalid');
    }
  }

  private controlFor(field: string): AbstractControl | null {
    if (field.startsWith('attributes.')) {
      return this.form.controls.attributes.controls[field.slice('attributes.'.length)] ?? null;
    }
    const name = field === 'requesterId' ? 'requester' : field;
    return (this.form.controls as Record<string, AbstractControl>)[name] ?? null;
  }

  /** The message a control shows: the server's verdict if it has one, else the local one. */
  protected messageFor(control: AbstractControl): string {
    const server = control.errors?.['server'];
    if (typeof server === 'string') {
      return server;
    }
    return control.hasError('required') ? 'form.required' : 'form.invalid';
  }
}
