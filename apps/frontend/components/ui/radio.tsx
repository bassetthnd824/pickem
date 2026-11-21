'use client'

import { Radio as BaseRadio } from '@base-ui/react/radio'
import { RadioGroup as BaseRadioGroup } from '@base-ui/react/radio-group'
import type { ComponentProps } from 'react'

const rootClass = 'choice-control'

const indicatorClass = 'choice-dot'

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
