export type ContextSummary = {
  displayName: string
  systemName: string
  loggedIn: boolean
  roleCount: number
  permissionCount: number
  annotations: boolean
}
export type PrototypeController = {
  setApiAnnotations: (enabled: boolean) => void
  dispose: () => void
}
