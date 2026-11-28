'use client'

import { Field } from '@base-ui/react/field'
import { Input } from '@base-ui/react/input'

type TextFieldProps = {
  id: string
  name: string
  label: string
  value: string
  onChange: (value: string) => void
  error?: string
  disabled?: boolean
}

export function TextField({
  id,
  name,
  label,
  value,
  onChange,
  error,
  disabled = false,
}: TextFieldProps) {
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
        value={value}
        disabled={disabled}
        aria-invalid={error ? true : undefined}
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
