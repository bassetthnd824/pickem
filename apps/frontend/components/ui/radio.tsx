'use client'

import { Radio as BaseRadio } from '@base-ui/react/radio'
import { RadioGroup as BaseRadioGroup } from '@base-ui/react/radio-group'
import type { ComponentProps } from 'react'

const rootClass =
  'flex size-4 shrink-0 items-center justify-center rounded-full border border-text bg-surface p-0 text-primary focus-visible:outline-2 focus-visible:outline-offset-2 data-checked:border-primary data-disabled:opacity-60 disabled:opacity-60'

const indicatorClass = 'size-2 rounded-full bg-current data-unchecked:hidden'

export function RadioGroup(props: ComponentProps<typeof BaseRadioGroup>) {
  return <BaseRadioGroup {...props} />
}

export function Radio(props: ComponentProps<typeof BaseRadio.Root>) {
  return (
    <BaseRadio.Root
      {...props}
      className={[rootClass, props.className].filter(Boolean).join(' ')}
    >
      <BaseRadio.Indicator className={indicatorClass} />
    </BaseRadio.Root>
  )
}
