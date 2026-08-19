import type { ReactNode } from 'react'
export function Loading({ label = 'Loading' }: { label?: string }) { return <p role="status">{label}…</p> }
export function Empty({ children = 'Nothing to show.' }: { children?: ReactNode }) { return <p className="empty">{children}</p> }
export function ErrorSummary({ message }: { message: string }) { return <div role="alert" className="error-summary">{message}</div> }
export function Card({ title, children }: { title: string; children: ReactNode }) { return <section className="card"><h2>{title}</h2>{children}</section> }
export function StatusBadge({ status }: { status: string }) { return <span className={`status status-${status.toLowerCase()}`}>{status}</span> }
export function DateDisplay({ value }: { value: string }) { return <time dateTime={value}>{new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(`${value}T00:00:00`))}</time> }
export function Retry({ onRetry }: { onRetry(): void }) { return <button type="button" onClick={onRetry}>Try again</button> }
export function DataTable({ label, children }: { label: string; children: ReactNode }) { return <div className="table-wrap"><table aria-label={label}>{children}</table></div> }
