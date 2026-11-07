"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { ApiError, api } from "../lib/api-client";
import {
  draftsFrom,
  moveRow,
  parseGameMain,
  pickResult,
  saveBody,
  type GridMatchup,
  type GridWeek,
  type PickDraft,
} from "../lib/picks-grid";
import { PickResultIcon } from "./pick-result-icon";
import { Button } from "./ui/button";

const MAIN_KEY = ["game", "main"] as const;

export function PicksGrid() {
  const query = useQuery({
    queryKey: MAIN_KEY,
    queryFn: async () => parseGameMain(await api.get<unknown>("/api/game/main")),
  });

  if (query.isPending) {
    return <p>Loading picks.</p>;
  }

  if (query.isError || !query.data) {
    return <p role="alert">Could not load picks.</p>;
  }

  return (
    <div className="flex flex-col gap-8">
      {query.data.weeks.map((week) => (
        <WeekPicks key={week.id} week={week} />
      ))}
    </div>
  );
}

function WeekPicks({ week }: { week: GridWeek }) {
  const queryClient = useQueryClient();
  const locked = week.currentOrPast;
  const [drafts, setDrafts] = useState<Record<string, PickDraft>>(() =>
    draftsFrom(week),
  );
  const [order, setOrder] = useState<string[]>(() =>
    week.matchups.map((matchup) => matchup.matchupId),
  );
  const [message, setMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setDrafts(draftsFrom(week));
    setOrder(week.matchups.map((matchup) => matchup.matchupId));
  }, [week]);

  async function save() {
    if (locked || saving) {
      return;
    }
    setSaving(true);
    setMessage(null);
    try {
      await api.post("/api/game/picks", saveBody(week.id, order, drafts));
      await queryClient.invalidateQueries({ queryKey: MAIN_KEY });
    } catch (error) {
      setMessage(errorDetail(error));
    } finally {
      setSaving(false);
    }
  }

  return (
    <section aria-label={`Week ${week.weekNumber}`} className="flex flex-col gap-3">
      <h2 className="text-lg font-semibold text-text">Week {week.weekNumber}</h2>
      <p className="text-sm text-muted">
        {locked ? "Picks are locked" : "Picks are open"}
      </p>
      <div className="overflow-x-auto">
        <table className="w-full border-collapse text-left text-sm text-text">
          <thead>
            <tr className="border-b border-border">
              <th scope="col" className="px-2 py-2 font-medium">
                Away
              </th>
              <th scope="col" className="px-2 py-2 font-medium">
                Home
              </th>
              <th scope="col" className="px-2 py-2 font-medium">
                Rank
              </th>
              {locked ? (
                <th scope="col" className="px-2 py-2 font-medium">
                  Score
                </th>
              ) : null}
              {locked ? (
                <th scope="col" className="px-2 py-2 font-medium">
                  Result
                </th>
              ) : null}
              <th scope="col" className="px-2 py-2 font-medium">
                Reorder
              </th>
            </tr>
          </thead>
          <tbody>
            {order.map((matchupId, index) => {
              const matchup = week.matchups.find(
                (item) => item.matchupId === matchupId,
              );
              if (!matchup) {
                return null;
              }
              const draft = drafts[matchup.matchupId] ?? {
                pickedTeamId: "",
                rank: "",
              };
              return (
                <tr key={matchup.matchupId} className="border-b border-border">
                  <TeamCell
                    weekNumber={week.weekNumber}
                    team={matchup.awayTeam}
                    selected={draft.pickedTeamId}
                    locked={locked}
                    group={matchup.matchupId}
                    onSelect={(teamId) =>
                      setDrafts((current) =>
                        updateDraft(current, matchup.matchupId, {
                          pickedTeamId: teamId,
                        }),
                      )
                    }
                  />
                  <TeamCell
                    weekNumber={week.weekNumber}
                    team={matchup.homeTeam}
                    selected={draft.pickedTeamId}
                    locked={locked}
                    group={matchup.matchupId}
                    onSelect={(teamId) =>
                      setDrafts((current) =>
                        updateDraft(current, matchup.matchupId, {
                          pickedTeamId: teamId,
                        }),
                      )
                    }
                  />
                  <td className="px-2 py-2">
                    <input
                      aria-label={`Rank for ${matchup.awayTeam.name} at ${matchup.homeTeam.name}`}
                      inputMode="numeric"
                      disabled={locked}
                      value={draft.rank}
                      onChange={(event) =>
                        setDrafts((current) =>
                          updateDraft(current, matchup.matchupId, {
                            rank: event.target.value,
                          }),
                        )
                      }
                      className="w-16 rounded border border-border bg-surface px-2 py-1 text-text disabled:opacity-60"
                    />
                  </td>
                  {locked ? (
                    <td className="px-2 py-2">
                      {scoreText(matchup.awayTeamScore, matchup.homeTeamScore)}
                    </td>
                  ) : null}
                  {locked ? (
                    <td className="px-2 py-2">
                      <ResultCell matchup={matchup} />
                    </td>
                  ) : null}
                  <td className="px-2 py-2">
                    <div className="flex gap-2">
                      <Button
                        type="button"
                        variant="nav"
                        className="bg-primary"
                        disabled={locked || index === 0}
                        aria-label={`Move ${matchup.awayTeam.name} at ${matchup.homeTeam.name} up`}
                        onClick={() => {
                          const moved = moveRow(order, drafts, matchup.matchupId, -1);
                          setOrder(moved.order);
                          setDrafts(moved.drafts);
                        }}
                      >
                        Up
                      </Button>
                      <Button
                        type="button"
                        variant="nav"
                        className="bg-primary"
                        disabled={locked || index === order.length - 1}
                        aria-label={`Move ${matchup.awayTeam.name} at ${matchup.homeTeam.name} down`}
                        onClick={() => {
                          const moved = moveRow(order, drafts, matchup.matchupId, 1);
                          setOrder(moved.order);
                          setDrafts(moved.drafts);
                        }}
                      >
                        Down
                      </Button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {message ? (
        <p role="alert" className="text-sm text-danger">
          {message}
        </p>
      ) : null}
      <Button type="button" disabled={locked || saving} onClick={() => void save()}>
        Save week {week.weekNumber}
      </Button>
    </section>
  );
}

function TeamCell({
  weekNumber,
  team,
  selected,
  locked,
  group,
  onSelect,
}: {
  weekNumber: number;
  team: GridMatchup["homeTeam"];
  selected: string;
  locked: boolean;
  group: string;
  onSelect: (teamId: string) => void;
}) {
  return (
    <td className="px-2 py-2">
      <label className="inline-flex items-center gap-2">
        <input
          type="radio"
          name={`${group}-week-${weekNumber}`}
          value={team.id}
          checked={selected === team.id}
          disabled={locked}
          aria-label={`Pick ${team.name} in week ${weekNumber}`}
          onChange={() => onSelect(team.id)}
        />
        <span>{team.name}</span>
      </label>
    </td>
  );
}

function ResultCell({ matchup }: { matchup: GridMatchup }) {
  const result = pickResult(matchup);
  if (!result) {
    return null;
  }
  return <PickResultIcon result={result} />;
}

function updateDraft(
  drafts: Record<string, PickDraft>,
  matchupId: string,
  patch: Partial<PickDraft>,
): Record<string, PickDraft> {
  const current = drafts[matchupId] ?? { pickedTeamId: "", rank: "" };
  return { ...drafts, [matchupId]: { ...current, ...patch } };
}

function scoreText(away: number | null, home: number | null): string {
  if (away == null || home == null) {
    return "";
  }
  return `${away} - ${home}`;
}

function errorDetail(error: unknown): string {
  if (error instanceof ApiError && error.body && typeof error.body === "object") {
    const detail = (error.body as { detail?: unknown }).detail;
    if (typeof detail === "string" && detail.trim() !== "") {
      return detail;
    }
  }
  return "Could not save picks.";
}
