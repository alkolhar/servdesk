import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  AttributeDefinitionModel,
  CategoryModel,
  ImpactModel,
  PersonModel,
  ProblemModel,
  UrgencyModel,
} from '../api/models';

/** A HAL collection or page: its items sit under one `_embedded` key (e.g. `personModelList`). */
interface HalCollection<T> {
  _embedded?: Record<string, T[]>;
}

function items<T>(collection: HalCollection<T>): T[] {
  return Object.values(collection._embedded ?? {})[0] ?? [];
}

/**
 * What the create-ticket form offers to choose from. Lists are loaded whole, in one page each:
 * fine for the MVP's reference data and directory sizes. A deployment with thousands of people
 * needs a server-side person search, which the API doesn't have yet.
 */
@Injectable({ providedIn: 'root' })
export class ReferenceDataService {
  private readonly http = inject(HttpClient);

  private list<T>(url: string): Promise<T[]> {
    return firstValueFrom(this.http.get<HalCollection<T>>(url)).then(items);
  }

  people(): Promise<PersonModel[]> {
    return this.list('/api/persons?size=1000&sort=name');
  }

  agents(): Promise<PersonModel[]> {
    return this.list('/api/persons?role=AGENT&size=1000&sort=name');
  }

  categories(): Promise<CategoryModel[]> {
    return this.list('/api/categories?size=1000&sort=name');
  }

  impacts(): Promise<ImpactModel[]> {
    return this.list('/api/impacts?size=100&sort=sortOrder');
  }

  urgencies(): Promise<UrgencyModel[]> {
    return this.list('/api/urgencies?size=100&sort=sortOrder');
  }

  /** The most recent Problems, for an Incident's optional link to its root cause. */
  problems(): Promise<ProblemModel[]> {
    return this.list('/api/problems?size=100&sort=id,desc');
  }

  ticketAttributes(): Promise<AttributeDefinitionModel[]> {
    return this.list('/api/attribute-definitions?target=TICKET');
  }
}
