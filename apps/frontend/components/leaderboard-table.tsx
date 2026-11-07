'use client'

import { useQuery } from '@tanstack/react-query'

import { api } from '@/lib/api-client'
import { parseLeaderboard } from '@/lib/leaderboard'

const LEADERBOARD_KEY = ['game', 'leaderboard'] as const

async function loadLeaderboard() {
  return parseLeaderboard(await api.get<unknown>('/api/game/leaderboard'))
}

export function LeaderboardTable() {
  const query = useQuery({
    queryKey: LEADERBOARD_KEY,
    queryFn: loadLeaderboard,
  })

  if (query.isPending) {
    return <p>Loading leaderboard.</p>
  }

  if (!query.data) {
    return <p role='alert'>Could not load the leaderboard.</p>
  }

  const board = query.data

  return (
    <div className='overflow-x-auto'>
      <table className='w-full border-collapse text-left text-sm text-text'>
        <thead>
          <tr className='border-b border-border'>
            <th scope='col' className='px-2 py-2 text-right font-medium'>
              Rank
            </th>
            <th scope='col' className='px-2 py-2 font-medium'>
              User
            </th>
            <th scope='col' className='px-2 py-2 text-right font-medium'>
              Score
            </th>
          </tr>
        </thead>
        <tbody>
          {board.standings.map((standing) => (
            <tr key={standing.uid} className='border-b border-border'>
              <td className='px-2 py-2 text-right tabular-nums'>
                {standing.rank}
              </td>
              <td className='px-2 py-2'>{standing.nickName}</td>
              <td className='px-2 py-2 text-right tabular-nums'>
                {standing.score}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
