import type { InputHTMLAttributes, ReactNode } from 'react'
export function Field({ label, error, children, ...input }: InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string; children?: ReactNode }) { const id=input.id ?? input.name; return <div className="field"><label htmlFor={id}>{label}</label>{children ?? <input {...input} id={id} aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : undefined}/>} {error && <span id={`${id}-error`} className="field-error">{error}</span>}</div> }

