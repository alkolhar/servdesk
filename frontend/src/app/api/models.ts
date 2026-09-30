// Types generated from the hand-authored contract (servdesk-api.yaml) by `npm run generate:api`,
// which every build, test and lint run first. A contract change that breaks a screen fails the
// build here rather than at runtime (ADR-0003).
import type { components } from './servdesk-api';

type Schemas = components['schemas'];

export type MeModel = Schemas['MeModel'];
export type LoginRequest = Schemas['LoginRequest'];
export type PersonRole = Schemas['PersonRole'];
