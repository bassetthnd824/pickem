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
    <div className='table-shell'>
      <table className='table-grid'>
        <thead>
          <tr>
            <th scope='col' className='table-head numeric'>
              Rank
            </th>
            <th scope='col' className='table-head'>
              User
            </th>
            <th scope='col' className='table-head numeric'>
              Score
            </th>
          </tr>
        </thead>
        <tbody>
          {board.standings.map((standing) => (
            <tr key={standing.uid}>
              <td className='data-cell numeric'>{standing.rank}</td>
              <td className='data-cell'>{standing.nickName}</td>
              <td className='data-cell numeric'>{standing.score}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
