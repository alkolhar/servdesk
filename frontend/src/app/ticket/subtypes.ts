/** The four ticket subtypes (ADR-0001), each with its own resource under /api and in the UI. */
export const SUBTYPES = [
  { type: 'incident', path: 'incidents' },
  { type: 'problem', path: 'problems' },
  { type: 'change', path: 'changes' },
  { type: 'service-request', path: 'service-requests' },
] as const;

export type Subtype = (typeof SUBTYPES)[number];
export type SubtypeName = Subtype['type'];
export type SubtypePath = Subtype['path'];
