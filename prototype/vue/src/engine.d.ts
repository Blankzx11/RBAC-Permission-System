import type { ContextSummary, PrototypeController } from './types'
/** Shared JavaScript interaction engine. This declaration checks the Vue/TS boundary, not all JS internals. */
export function mountPrototype(onUpdate: (summary: ContextSummary) => void): PrototypeController
