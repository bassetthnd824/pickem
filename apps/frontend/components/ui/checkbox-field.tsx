'use client'

import { Checkbox } from '@base-ui/react/checkbox'
import { Field } from '@base-ui/react/field'

type CheckboxFieldProps = {
  name: string
  label: string
  checked: boolean
  onChange: (checked: boolean) => void
  error?: string
  disabled?: boolean
}

export function CheckboxField({
  name,
  label,
  checked,
  onChange,
  error,
  disabled = false,
}: CheckboxFieldProps) {
  return (
    <Field.Root
      name={name}
      disabled={disabled}
      invalid={Boolean(error)}
      className='field-stack'
    >
      <Field.Label className='choice-row'>
        <Checkbox.Root
          name={name}
          checked={checked}
          disabled={disabled}
          aria-invalid={error ? true : undefined}
          onCheckedChange={(nextChecked) => {
            onChange(nextChecked)
          }}
          className='choice-box'
        >
          <Checkbox.Indicator
            aria-hidden
            className='text-xs leading-none font-bold'
          >
            ✓
          </Checkbox.Indicator>
        </Checkbox.Root>
        {label}
      </Field.Label>
      {error ? (
        <Field.Error className='field-error' match>
          {error}
        </Field.Error>
      ) : null}
    </Field.Root>
  )
}
