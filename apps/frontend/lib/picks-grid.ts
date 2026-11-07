export type GridTeam = {
  id: string;
  name: string;
};

export type GridMatchup = {
  matchupId: string;
  awayTeam: GridTeam;
  homeTeam: GridTeam;
  awayTeamScore: number | null;
  homeTeamScore: number | null;
  pickedTeamId: string | null;
  rank: number | null;
  winningTeamId: string | null;
};

export type GridWeek = {
  id: string;
  weekNumber: number;
  currentOrPast: boolean;
  matchups: GridMatchup[];
};

export type GridMain = {
  weeks: GridWeek[];
};

export type PickDraft = {
  pickedTeamId: string;
  rank: string;
};

export type PickResult = "correct" | "incorrect";

export function parseGameMain(value: unknown): GridMain {
  if (!value || typeof value !== "object") {
    throw new Error("Game main response was invalid");
  }
  const weeks = (value as { weeks?: unknown }).weeks;
  if (!Array.isArray(weeks)) {
    throw new Error("Game main response was invalid");
  }
  return { weeks: weeks.map(parseWeek) };
}

export function draftsFrom(week: GridWeek): Record<string, PickDraft> {
  const drafts: Record<string, PickDraft> = {};
  for (const matchup of week.matchups) {
    drafts[matchup.matchupId] = {
      pickedTeamId: matchup.pickedTeamId ?? "",
      rank: matchup.rank == null ? "" : String(matchup.rank),
    };
  }
  return drafts;
}

export function moveRow(
  order: readonly string[],
  drafts: Record<string, PickDraft>,
  matchupId: string,
  direction: -1 | 1,
): { order: string[]; drafts: Record<string, PickDraft> } {
  const index = order.indexOf(matchupId);
  const next = index + direction;
  const neighborId = next >= 0 && next < order.length ? order[next] : undefined;
  const current = drafts[matchupId];
  const neighbor = neighborId ? drafts[neighborId] : undefined;
  if (index < 0 || !neighborId || !current || !neighbor) {
    return { order: [...order], drafts };
  }
  const nextOrder = order.slice();
  nextOrder[index] = neighborId;
  nextOrder[next] = matchupId;
  const bothRanked = current.rank.trim() !== "" && neighbor.rank.trim() !== "";
  return {
    order: nextOrder,
    drafts: bothRanked
      ? {
          ...drafts,
          [matchupId]: { ...current, rank: neighbor.rank },
          [neighborId]: { ...neighbor, rank: current.rank },
        }
      : drafts,
  };
}

export function saveBody(
  seasonWeekId: string,
  order: readonly string[],
  drafts: Record<string, PickDraft>,
): { seasonWeekId: string; picks: { matchupId: string; pickedTeamId: string | null; rank: number | null }[] } {
  return {
    seasonWeekId,
    picks: order.map((matchupId) => {
      const draft = drafts[matchupId];
      const pickedTeamId = draft?.pickedTeamId.trim() ?? "";
      const rawRank = draft?.rank.trim() ?? "";
      const rank = Number(rawRank);
      return {
        matchupId,
        pickedTeamId: pickedTeamId === "" ? null : pickedTeamId,
        rank:
          rawRank === "" || !Number.isInteger(rank) || rank < 1 ? null : rank,
      };
    }),
  };
}

export function pickResult(matchup: GridMatchup): PickResult | null {
  if (!matchup.winningTeamId || !matchup.pickedTeamId) {
    return null;
  }
  if (
    matchup.homeTeamScore != null &&
    matchup.awayTeamScore != null &&
    matchup.homeTeamScore === matchup.awayTeamScore
  ) {
    return null;
  }
  return matchup.pickedTeamId === matchup.winningTeamId
    ? "correct"
    : "incorrect";
}

function parseWeek(value: unknown): GridWeek {
  if (!value || typeof value !== "object") {
    throw new Error("Game main response was invalid");
  }
  const record = value as Record<string, unknown>;
  if (typeof record.id !== "string" || typeof record.weekNumber !== "number") {
    throw new Error("Game main response was invalid");
  }
  if (!Array.isArray(record.matchups)) {
    throw new Error("Game main response was invalid");
  }
  return {
    id: record.id,
    weekNumber: record.weekNumber,
    currentOrPast: record.currentOrPast === true,
    matchups: record.matchups.map(parseMatchup),
  };
}

function parseMatchup(value: unknown): GridMatchup {
  if (!value || typeof value !== "object") {
    throw new Error("Game main response was invalid");
  }
  const record = value as Record<string, unknown>;
  if (typeof record.matchupId !== "string") {
    throw new Error("Game main response was invalid");
  }
  return {
    matchupId: record.matchupId,
    awayTeam: parseTeam(record.awayTeam),
    homeTeam: parseTeam(record.homeTeam),
    awayTeamScore: parseScore(record.awayTeamScore),
    homeTeamScore: parseScore(record.homeTeamScore),
    pickedTeamId:
      typeof record.pickedTeamId === "string" ? record.pickedTeamId : null,
    rank: typeof record.rank === "number" ? record.rank : null,
    winningTeamId:
      typeof record.winningTeamId === "string" ? record.winningTeamId : null,
  };
}

function parseTeam(value: unknown): GridTeam {
  if (!value || typeof value !== "object") {
    throw new Error("Game main response was invalid");
  }
  const record = value as Record<string, unknown>;
  if (typeof record.id !== "string" || typeof record.name !== "string") {
    throw new Error("Game main response was invalid");
  }
  return { id: record.id, name: record.name };
}

function parseScore(value: unknown): number | null {
  return typeof value === "number" ? value : null;
}
