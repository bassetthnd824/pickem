'use client'

import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'

import { AdminForm } from '@/components/admin/admin-form'
import { DateField } from '@/components/date-field'
import { SelectField } from '@/components/ui/select-field'
import { TextField } from '@/components/ui/text-field'
import { deleteAdmin, saveAdmin } from '@/lib/admin/admin-actions'
import type { FieldErrors } from '@/lib/admin/problem-detail'
import { managerCollection, managerItem } from '@/lib/admin/query-string'
import {
  parseSeasonWeek,
  parseSeasons,
  type SeasonRecord,
  type SeasonWeekRecord,
} from '@/lib/admin/records'
import { resumeHref } from '@/lib/admin/search-memory'
import { defaultWeekDates, parseWeekNumber } from '@/lib/admin/week-dates'
import { api } from '@/lib/api-client'

const RESOURCE = 'season-weeks'
const FIELDS = ['seasonId', 'weekNumber', 'beginDate', 'endDate']
const LIST = '/season-weeks'

type SeasonWeekFormProps = {
  mode: 'add' | 'edit'
  id?: string
}

type WeekDraft = {
  seasonId: string
  weekNumber: string
  beginDate: string
  endDate: string
}

const EMPTY: WeekDraft = {
  seasonId: '',
  weekNumber: '',
  beginDate: '',
  endDate: '',
}

function fromRecord(record: SeasonWeekRecord): WeekDraft {
  return {
    seasonId: record.seasonId,
    weekNumber: String(record.weekNumber),
    beginDate: record.beginDate,
    endDate: record.endDate,
  }
}

function withDefaults(
  draft: WeekDraft,
  seasons: readonly SeasonRecord[],
): WeekDraft {
  const season = seasons.find((item) => item.id === draft.seasonId)
  const weekNumber = parseWeekNumber(draft.weekNumber)
  if (!season || weekNumber == null) {
    return draft
  }
  const dates = defaultWeekDates(season.beginDate, weekNumber)
  if (!dates) {
    return draft
  }
  return { ...draft, beginDate: dates.beginDate, endDate: dates.endDate }
}

export function SeasonWeekForm({ mode, id }: SeasonWeekFormProps) {
  const router = useRouter()
  const seasonsQuery = useQuery({
    queryKey: ['manager', 'seasons', 'choices'],
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseSeasons(
        await api.get<unknown>(managerCollection('seasons'), { signal }),
      ),
  })
  const recordQuery = useQuery({
    queryKey: ['manager', RESOURCE, id],
    enabled: mode === 'edit' && Boolean(id),
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseSeasonWeek(
        await api.get<unknown>(managerItem(RESOURCE, id ?? ''), { signal }),
      ),
  })
  const [draft, setDraft] = useState<WeekDraft | null>(null)
  const [version, setVersion] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | undefined>()

  if (seasonsQuery.isPending || (mode === 'edit' && recordQuery.isPending)) {
    return <p>Loading season week.</p>
  }

  if (!seasonsQuery.data || (mode === 'edit' && !recordQuery.data)) {
    return <p role='alert'>Could not load season week.</p>
  }

  const seasons = seasonsQuery.data
  const record = recordQuery.data
  if (
    mode === 'edit' &&
    record &&
    draft === null &&
    version !== record.version
  ) {
    setVersion(record.version)
  }
  const values = draft ?? (record ? fromRecord(record) : EMPTY)

  function update(patch: Partial<WeekDraft>) {
    setDraft((current) => {
      const base = current ?? values
      const next = { ...base, ...patch }
      const seasonChanged = next.seasonId !== base.seasonId
      const weekChanged =
        parseWeekNumber(next.weekNumber) !== parseWeekNumber(base.weekNumber)
      if (!seasonChanged && !weekChanged) {
        return current
      }
      return withDefaults(next, seasons)
    })
  }

  async function save() {
    setSaving(true)
    setFieldErrors({})
    setFormError(undefined)
    const saved = await saveAdmin(
      RESOURCE,
      mode === 'edit' ? id : undefined,
      {
        seasonId: values.seasonId,
        weekNumber: parseWeekNumber(values.weekNumber),
        beginDate: values.beginDate.trim(),
        endDate: values.endDate.trim(),
        ...(mode === 'edit' ? { version } : {}),
      },
      FIELDS,
    )
    setSaving(false)
    if (!saved.ok) {
      setFieldErrors(saved.fieldErrors)
      setFormError(saved.formError)
      return
    }
    router.push(resumeHref(LIST))
  }

  async function remove() {
    if (!id) {
      return
    }
    setDeleting(true)
    setFormError(undefined)
    const removed = await deleteAdmin(RESOURCE, id)
    setDeleting(false)
    if (!removed.ok) {
      setFormError(removed.formError)
      return
    }
    router.push(resumeHref(LIST))
  }

  return (
    <AdminForm
      title='Season Week'
      mode={mode}
      saving={saving}
      deleting={deleting}
      formError={formError}
      cancelHref={resumeHref(LIST)}
      onSubmit={() => {
        void save()
      }}
      onDelete={() => {
        void remove()
      }}
      lastUpdateDate={record?.lastUpdateDate}
      lastUpdateUser={record?.lastUpdateUser}
    >
      <SelectField
        name='seasonId'
        label='Season'
        emptyLabel='Please select a Season'
        value={values.seasonId}
        error={fieldErrors.seasonId}
        options={seasons.map((season) => ({
          value: season.id,
          label: season.season,
        }))}
        onChange={(seasonId) => {
          update({ seasonId })
        }}
      />
      <TextField
        id='week-number'
        name='weekNumber'
        label='Week Number'
        value={values.weekNumber}
        error={fieldErrors.weekNumber}
        onChange={(weekNumber) => {
          update({ weekNumber })
        }}
      />
      <DateField
        id='week-begin'
        name='beginDate'
        label='Begin Date'
        value={values.beginDate}
        error={fieldErrors.beginDate}
        onChange={(beginDate) => {
          setDraft((current) => ({
            ...(current ?? values),
            beginDate,
          }))
        }}
      />
      <DateField
        id='week-end'
        name='endDate'
        label='End Date'
        value={values.endDate}
        error={fieldErrors.endDate}
        onChange={(endDate) => {
          setDraft((current) => ({
            ...(current ?? values),
            endDate,
          }))
        }}
      />
    </AdminForm>
  )
}
