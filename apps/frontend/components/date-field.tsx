'use client'

import { Field } from '@base-ui/react/field'
import { Input } from '@base-ui/react/input'

type DateFieldProps = {
  id: string
  name: string
  label: string
  value: string
  onChange: (value: string) => void
  onBlur?: () => void
  error?: string
  disabled?: boolean
  required?: boolean
  min?: string
  max?: string
}

export function DateField({
  id,
  name,
  label,
  value,
  onChange,
  onBlur,
  error,
  disabled = false,
  required = false,
  min,
  max,
}: DateFieldProps) {
  return (
    <Field.Root
      name={name}
      disabled={disabled}
      invalid={Boolean(error)}
      className='field-stack'
    >
      <Field.Label className='field-label'>{label}</Field.Label>
      <Input
        id={id}
        type='date'
        value={value}
        min={min}
        max={max}
        required={required}
        aria-invalid={error ? true : undefined}
        onBlur={onBlur}
        onValueChange={(nextValue) => {
          onChange(nextValue)
        }}
        className='field-control'
      />
      {error ? (
        <Field.Error className='field-error' match>
          {error}
        </Field.Error>
      ) : null}
    </Field.Root>
  )
}
