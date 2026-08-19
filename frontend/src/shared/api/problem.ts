export interface FieldError { field: string; message: string }
export interface ApiProblem { type: string; title: string; status: number; code: string; detail: string; correlationId: string; fieldErrors?: FieldError[] }
export class ApiError extends Error { constructor(public readonly problem: ApiProblem) { super(problem.detail) } }

