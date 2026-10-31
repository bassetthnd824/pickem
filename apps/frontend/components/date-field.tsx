"use client";

import { Field } from "@base-ui/react/field";
import { Input } from "@base-ui/react/input";

type DateFieldProps = {
  id: string;
  name: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  onBlur?: () => void;
  error?: string;
  disabled?: boolean;
  required?: boolean;
};

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
}: DateFieldProps) {
  return (
    <Field.Root
      name={name}
      disabled={disabled}
      invalid={Boolean(error)}
      className="flex flex-col gap-1"
    >
      <Field.Label className="text-sm font-medium text-text">
        {label}
      </Field.Label>
      <Input
        id={id}
        type="date"
        value={value}
        required={required}
        aria-invalid={error ? true : undefined}
        onBlur={onBlur}
        onValueChange={(nextValue) => {
          onChange(nextValue);
        }}
        className="rounded border border-border bg-surface px-2 py-1 text-text"
      />
      {error ? (
        <Field.Error className="text-sm text-danger" match>
          {error}
        </Field.Error>
      ) : null}
    </Field.Root>
  );
}
