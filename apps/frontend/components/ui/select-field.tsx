'use client'

import { Field } from '@base-ui/react/field'
import { Select } from '@base-ui/react/select'

export type SelectOption = {
  value: string
  label: string
}

const itemClass = 'menu-item outline-hidden'

type SelectFieldProps = {
  name: string
  label: string
  value: string
  onChange: (value: string) => void
  options: readonly SelectOption[]
  emptyLabel: string
  error?: string
  disabled?: boolean
}

export function SelectField({
  name,
  label,
  value,
  onChange,
  options,
  emptyLabel,
  error,
  disabled = false,
}: SelectFieldProps) {
  const items = [
    { value: null as string | null, label: emptyLabel },
    ...options.map((option) => ({
      value: option.value,
      label: option.label,
    })),
  ]

  return (
    <Field.Root
      name={name}
      disabled={disabled}
      invalid={Boolean(error)}
      className='field-stack'
    >
      <Select.Root
        items={items}
        value={value === '' ? null : value}
        disabled={disabled}
        onValueChange={(nextValue) => {
          onChange(nextValue ?? '')
        }}
      >
        <Field.Label className='field-label'>{label}</Field.Label>
        <Select.Trigger className='field-control flex cursor-pointer items-center justify-between text-left'>
          <Select.Value placeholder={emptyLabel} />
          <Select.Icon className='ml-2 text-muted' aria-hidden>
            ▾
          </Select.Icon>
        </Select.Trigger>
        <Select.Portal>
          <Select.Positioner
            className='z-10 outline-none'
            side='bottom'
            sideOffset={4}
            align='start'
            alignItemWithTrigger={false}
          >
            <Select.Popup className='menu-popup max-h-72 w-(--anchor-width) origin-[var(--transform-origin)] outline-none'>
              <Select.List>
                {items.map((item) => (
                  <Select.Item
                    key={item.value ?? 'empty'}
                    value={item.value}
                    className={itemClass}
                  >
                    <Select.ItemText>{item.label}</Select.ItemText>
                  </Select.Item>
                ))}
              </Select.List>
            </Select.Popup>
          </Select.Positioner>
        </Select.Portal>
      </Select.Root>
      {error ? (
        <Field.Error className='field-error' match>
          {error}
        </Field.Error>
      ) : null}
    </Field.Root>
  )
}
