"use client";

import { Button as BaseButton } from "@base-ui/react/button";
import type { ComponentProps } from "react";

type ButtonProps = Omit<ComponentProps<typeof BaseButton>, "className"> & {
  variant?: "primary" | "nav";
  className?: string;
};

const variantClass = {
  primary:
    "inline-flex rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-fg focus-visible:outline-2 focus-visible:outline-offset-2 data-disabled:opacity-60 disabled:opacity-60",
  nav: "rounded-md px-3 py-2 text-sm font-medium text-primary-fg focus-visible:outline-2 focus-visible:outline-offset-2 data-disabled:opacity-60 disabled:opacity-60",
} as const;

export function Button({
  variant = "primary",
  className,
  ...props
}: ButtonProps) {
  const classes = [variantClass[variant], className].filter(Boolean).join(" ");
  return <BaseButton {...props} className={classes} />;
}
