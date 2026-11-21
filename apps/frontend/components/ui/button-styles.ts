const quietWhenDisabled = 'data-disabled:opacity-60 disabled:opacity-60'

export const buttonVariantClass = {
  primary: `btn-primary ${quietWhenDisabled}`,
  secondary: `btn-secondary ${quietWhenDisabled}`,
  nav: `btn-nav ${quietWhenDisabled}`,
  compact: `btn-compact ${quietWhenDisabled}`,
} as const
