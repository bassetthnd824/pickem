'use client'

import { Button as BaseButton } from '@base-ui/react/button'
import type { ComponentProps } from 'react'

import { buttonVariantClass } from '@/components/ui/button-styles'

type ButtonProps = Omit<ComponentProps<typeof BaseButton>, 'className'> & {
  variant?: 'primary' | 'secondary' | 'nav'
  className?: string
}

export function Button({
  variant = 'primary',
  className,
  ...props
}: ButtonProps) {
  const classes = [
    buttonVariantClass[variant],
    'cursor-pointer disabled:cursor-not-allowed data-disabled:cursor-not-allowed',
    className,
  ]
    .filter(Boolean)
    .join(' ')
  return <BaseButton {...props} className={classes} />
}
