'use client'

import Link from 'next/link'
import type { ReactNode } from 'react'

import { Button } from '@/components/ui/button'
import { buttonVariantClass } from '@/components/ui/button-styles'

export type AdminRow = {
  id: string
  cells: readonly string[]
}

type AdminListProps = {
  addHref: string
  onSearch: () => void
  searchFields: ReactNode
  pending: boolean
  searched: boolean
  error?: string
  rows: readonly AdminRow[]
  columns: readonly string[]
  editHref: (id: string) => string
}

export function AdminList({
  addHref,
  onSearch,
  searchFields,
  pending,
  searched,
  error,
  rows,
  columns,
  editHref,
}: AdminListProps) {
  return (
    <div className='flex flex-col gap-6'>
      <form
        className='surface-card'
        onSubmit={(event) => {
          event.preventDefault()
          onSearch()
        }}
      >
        {searchFields}
        <div className='flex justify-end'>
          <Button type='submit'>Search</Button>
        </div>
      </form>
      {error ? (
        <p role='alert' className='alert-banner'>
          {error}
        </p>
      ) : null}
      {pending ? <p>Loading results.</p> : null}
      {!pending && searched && !error && rows.length === 0 ? (
        <p>No results found</p>
      ) : null}
      {!pending && rows.length > 0 ? (
        <div className='table-shell'>
          <table className='table-grid'>
            <thead>
              <tr>
                {columns.map((column) => (
                  <th key={column} className='table-head'>
                    {column}
                  </th>
                ))}
                <th className='table-head'>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  {row.cells.map((cell, index) => (
                    <td key={`${row.id}-${index}`} className='data-cell'>
                      {cell}
                    </td>
                  ))}
                  <td className='data-cell'>
                    <Link
                      href={editHref(row.id)}
                      className='font-semibold text-highlight'
                    >
                      Edit
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
      <Link
        href={addHref}
        className={`${buttonVariantClass.compact} w-fit cursor-pointer`}
      >
        Add
      </Link>
    </div>
  )
}
