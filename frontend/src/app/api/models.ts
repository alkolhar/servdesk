// Types generated from the hand-authored contract (servdesk-api.yaml) by `npm run generate:api`,
// which every build, test and lint run first. A contract change that breaks a screen fails the
// build here rather than at runtime (ADR-0003).
import type { components } from './servdesk-api';

type Schemas = components['schemas'];

export type MeModel = Schemas['MeModel'];
export type LoginRequest = Schemas['LoginRequest'];
export type PersonRole = Schemas['PersonRole'];
export type SetupStatus = Schemas['SetupStatus'];
export type SetupRequest = Schemas['SetupRequest'];
export type ChangePasswordRequest = Schemas['ChangePasswordRequest'];
export type IncidentModel = Schemas['IncidentModel'];
export type TicketTaskModel = Schemas['TicketTaskModel'];
export type ActionRequest = Schemas['ActionRequest'];
export type TicketStatus = Schemas['TicketStatus'];
export type PersonModel = Schemas['PersonModel'];
export type PriorityModel = Schemas['PriorityModel'];

/** A HAL link. The generated `_links` types are opaque objects; this is their actual shape. */
export interface HalLink {
  href: string;
}
export type HalLinks = Record<string, HalLink>;
export type CategoryModel = Schemas['CategoryModel'];
export type ImpactModel = Schemas['ImpactModel'];
export type UrgencyModel = Schemas['UrgencyModel'];
export type ProblemModel = Schemas['ProblemModel'];
export type AttributeDefinitionModel = Schemas['AttributeDefinitionModel'];

/** One field-level problem in a 400 ProblemDetail's `errors` list (RestExceptionHandler). */
export interface FieldProblem {
  field: string;
  code: string;
  message?: string | null;
}
