/** Standings stay in response order. This module does not rank them again. */

export type Standing = {
  rank: number;
  uid: string;
  nickName: string;
  score: number;
};

export type SeasonLeaderboard = {
  seasonId: string;
  season: string;
  standings: Standing[];
};

export function parseLeaderboard(value: unknown): SeasonLeaderboard {
  if (!value || typeof value !== "object") {
    throw new Error("Leaderboard response was invalid");
  }
  const record = value as Record<string, unknown>;
  if (typeof record.seasonId !== "string" || record.seasonId.trim() === "") {
    throw new Error("Leaderboard response was invalid");
  }
  if (typeof record.season !== "string") {
    throw new Error("Leaderboard response was invalid");
  }
  if (!Array.isArray(record.standings)) {
    throw new Error("Leaderboard response was invalid");
  }
  return {
    seasonId: record.seasonId,
    season: record.season,
    standings: record.standings.map(parseStanding),
  };
}

function parseStanding(value: unknown): Standing {
  if (!value || typeof value !== "object") {
    throw new Error("Leaderboard response was invalid");
  }
  const record = value as Record<string, unknown>;
  const rank = integer(record.rank);
  const score = integer(record.score);
  if (rank == null || rank < 1 || score == null || score < 0) {
    throw new Error("Leaderboard response was invalid");
  }
  if (typeof record.uid !== "string" || record.uid.trim() === "") {
    throw new Error("Leaderboard response was invalid");
  }
  if (record.nickName != null && typeof record.nickName !== "string") {
    throw new Error("Leaderboard response was invalid");
  }
  return {
    rank,
    uid: record.uid,
    nickName: typeof record.nickName === "string" ? record.nickName : "",
    score,
  };
}

function integer(value: unknown): number | null {
  if (typeof value !== "number" || !Number.isInteger(value)) {
    return null;
  }
  return value;
}
