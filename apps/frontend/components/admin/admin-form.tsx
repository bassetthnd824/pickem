'use client'

import Link from 'next/link'
import type { ReactNode } from 'react'

import { Button } from '@/components/ui/button'
import { buttonVariantClass } from '@/components/ui/button-styles'
import { formatAudit } from '@/lib/admin/audit'

type AdminFormProps = {
  title: string
  mode: 'add' | 'edit'
  saving: boolean
  deleting: boolean
  formError?: string
  cancelHref: string
  onSubmit: () => void
  onDelete?: () => void
  lastUpdateDate?: string | null
  lastUpdateUser?: string | null
  children: ReactNode
}

export function AdminForm({
  title,
  mode,
  saving,
  deleting,
  formError,
  cancelHref,
  onSubmit,
  onDelete,
  lastUpdateDate,
  lastUpdateUser,
  children,
}: AdminFormProps) {
  const busy = saving || deleting

  return (
    <form
      className='surface-card max-w-md'
      onSubmit={(event) => {
        event.preventDefault()
        onSubmit()
      }}
    >
      <div className='flex items-center justify-between gap-3'>
        <h2 className='type-title'>{title}</h2>
        {mode === 'edit' && onDelete ? (
          <Button
            type='button'
            variant='secondary'
            disabled={busy}
            onClick={onDelete}
          >
            Delete
          </Button>
        ) : null}
      </div>
      {formError ? (
        <p role='alert' className='alert-banner'>
          {formError}
        </p>
      ) : null}
      {children}
      {mode === 'edit' ? (
        <div className='flex flex-col gap-2'>
          <p>
            <span className='field-label'>Last Updated On </span>
            {formatAudit(lastUpdateDate ?? null)}
          </p>
          <p>
            <span className='field-label'>Last Updated By </span>
            {lastUpdateUser ?? ''}
          </p>
        </div>
      ) : null}
      <div className='flex justify-end gap-3'>
        <Link
          href={cancelHref}
          className={`${buttonVariantClass.secondary} cursor-pointer`}
        >
          Cancel
        </Link>
        <Button type='submit' disabled={busy}>
          Save
        </Button>
      </div>
    </form>
  )
}
