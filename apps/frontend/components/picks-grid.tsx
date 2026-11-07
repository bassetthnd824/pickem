"use client";

import { Toolbar } from "@base-ui/react/toolbar";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { ApiError, api } from "../lib/api-client";
import {
  draftsFromMain,
  moveMatchup,
  parseGameMain,
  pickResult,
  saveBody,
  venueLabel,
  type GridMain,
  type GridMatchup,
  type GridWeek,
  type WeekDraft,
} from "../lib/picks-grid";
import { useAuth } from "./auth-provider";
import { PickResultIcon } from "./pick-result-icon";
import { Button } from "./ui/button";
import { Radio, RadioGroup } from "./ui/radio";

const MAIN_KEY = ["game", "main"] as const;

async function loadMain(): Promise<GridMain> {
  return parseGameMain(await api.get<unknown>("/api/game/main"));
}

export function PicksGrid() {
  const { profile } = useAuth();
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: MAIN_KEY,
    queryFn: loadMain,
  });
  const [drafts, setDrafts] = useState<Record<string, WeekDraft> | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (query.data && drafts === null) {
    setDrafts(draftsFromMain(query.data));
  }

  if (query.isPending) {
    return <p>Loading picks.</p>;
  }

  if (!query.data) {
    return <p role="alert">Could not load picks.</p>;
  }

  const main = query.data;
  const canSave = main.weeks.some((week) => !week.currentOrPast);

  async function reload(): Promise<GridMain | null> {
    await queryClient.cancelQueries({ queryKey: MAIN_KEY });
    try {
      return await queryClient.fetchQuery({
        queryKey: MAIN_KEY,
        queryFn: loadMain,
        staleTime: 0,
      });
    } catch {
      return null;
    }
  }

  async function save() {
    if (!canSave || busy || !drafts) {
      return;
    }
    const snapshot = drafts;
    setBusy(true);
    setMessage(null);
    let failed = false;
    try {
      for (const week of main.weeks) {
        const draft = snapshot[week.id];
        if (week.currentOrPast || !draft) {
          continue;
        }
        await api.post("/api/game/picks", saveBody(week.id, draft));
      }
    } catch (error) {
      failed = true;
      setMessage(errorDetail(error));
    } finally {
      const fresh = await reload();
      if (fresh) {
        setDrafts(draftsFromMain(fresh));
      } else if (!failed) {
        setMessage("Could not reload picks.");
      }
      setBusy(false);
    }
  }

  async function cancel() {
    if (busy) {
      return;
    }
    setBusy(true);
    setMessage(null);
    const fresh = await reload();
    if (!fresh) {
      setMessage("Could not reload picks.");
    } else {
      setDrafts(draftsFromMain(fresh));
    }
    setBusy(false);
  }

  return (
    <div className="flex flex-col gap-8">
      <h2 className="text-xl font-semibold text-text">
        Welcome {profile?.firstName ?? ""}, Season {main.season}
      </h2>
      {main.weeks.map((week) => {
        const draft = drafts?.[week.id];
        if (!draft) {
          return null;
        }
        return (
          <WeekPicks
            key={week.id}
            week={week}
            draft={draft}
            conferenceTeams={main.numberOfConferenceTeams}
            busy={busy}
            onChange={(next) =>
              setDrafts((current) => ({
                ...current,
                [week.id]: next,
              }))
            }
          />
        );
      })}
      {message ? (
        <p role="alert" className="text-sm text-danger">
          {message}
        </p>
      ) : null}
      <div className="flex justify-end gap-2">
        <Button
          type="button"
          variant="secondary"
          disabled={busy}
          onClick={() => void cancel()}
        >
          Cancel
        </Button>
        <Button type="button" disabled={!canSave || busy} onClick={() => void save()}>
          Save
        </Button>
      </div>
    </div>
  );
}

function WeekPicks({
  week,
  draft,
  conferenceTeams,
  busy,
  onChange,
}: {
  week: GridWeek;
  draft: WeekDraft;
  conferenceTeams: number;
  busy: boolean;
  onChange: (draft: WeekDraft) => void;
}) {
  const locked = week.currentOrPast;
  const frozen = locked || busy;

  return (
    <section aria-label={`Week ${week.weekNumber}`} className="flex flex-col gap-3">
      <h3 className="text-lg font-semibold text-text">Week {week.weekNumber}</h3>
      <p className="text-sm text-muted">
        {locked ? "Picks are locked" : "Picks are open"}
      </p>
      <div className="overflow-x-auto">
        <table className="w-full border-collapse text-left text-sm text-text">
          <thead>
            <tr className="border-b border-border">
              {locked ? <Column label="Result" /> : null}
              <Column label="Date" className="hidden md:table-cell" />
              <Column label="Rivalry" className="hidden lg:table-cell" />
              <Column label="Away" />
              {locked ? <Column label="Score" /> : null}
              <Column label="Rank" />
              {locked ? <Column label="Score" /> : null}
              <Column label="Home" />
              <Column label="Venue" className="hidden md:table-cell" />
              <Column label="Reorder" />
            </tr>
          </thead>
          <tbody>
            {draft.order.map((matchupId, index) => {
              const matchup = week.matchups.find((item) => item.matchupId === matchupId);
              if (!matchup) {
                return null;
              }
              const pickedTeamId = draft.pickedTeamId[matchup.matchupId] ?? "";
              const rank = draft.rank[matchup.matchupId];
              const matchupLabel = `${matchup.awayTeam.name} at ${matchup.homeTeam.name}`;
              return (
                <RadioGroup
                  key={matchup.matchupId}
                  render={<tr />}
                  className="border-b border-border"
                  aria-label={matchupLabel}
                  name={`${matchup.matchupId}-week-${week.weekNumber}`}
                  value={pickedTeamId}
                  disabled={frozen}
                  onValueChange={(teamId) =>
                    onChange({
                      ...draft,
                      pickedTeamId: {
                        ...draft.pickedTeamId,
                        [matchup.matchupId]: String(teamId),
                      },
                    })
                  }
                >
                  {locked ? (
                    <td className="px-2 py-2">
                      <ResultCell matchup={matchup} />
                    </td>
                  ) : null}
                  <td className="hidden px-2 py-2 md:table-cell">{dateText(matchup.matchupDate)}</td>
                  <td className="hidden px-2 py-2 lg:table-cell">
                    {matchup.rivalryName ?? ""}
                  </td>
                  <TeamCell weekNumber={week.weekNumber} team={matchup.awayTeam} />
                  {locked ? (
                    <td className="px-2 py-2">{scoreText(matchup.awayTeamScore)}</td>
                  ) : null}
                  <td className="px-2 py-2">
                    <output aria-label={`Rank for ${matchupLabel}`}>
                      {rank ?? ""}
                    </output>
                  </td>
                  {locked ? (
                    <td className="px-2 py-2">{scoreText(matchup.homeTeamScore)}</td>
                  ) : null}
                  <TeamCell weekNumber={week.weekNumber} team={matchup.homeTeam} />
                  <td className="hidden px-2 py-2 md:table-cell">
                    {venueLabel(matchup.venue)}
                  </td>
                  <td className="px-2 py-2">
                    <Toolbar.Root
                      aria-label={`Reorder ${matchupLabel}`}
                      className="flex gap-2"
                    >
                      <Toolbar.Button
                        render={<Button variant="nav" className="bg-primary" />}
                        focusableWhenDisabled={false}
                        disabled={frozen || index === 0}
                        aria-label={`Move ${matchupLabel} up`}
                        onClick={() =>
                          onChange(
                            moveMatchup(draft, matchup.matchupId, -1, conferenceTeams),
                          )
                        }
                      >
                        Up
                      </Toolbar.Button>
                      <Toolbar.Button
                        render={<Button variant="nav" className="bg-primary" />}
                        focusableWhenDisabled={false}
                        disabled={frozen || index === draft.order.length - 1}
                        aria-label={`Move ${matchupLabel} down`}
                        onClick={() =>
                          onChange(
                            moveMatchup(draft, matchup.matchupId, 1, conferenceTeams),
                          )
                        }
                      >
                        Down
                      </Toolbar.Button>
                    </Toolbar.Root>
                  </td>
                </RadioGroup>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function Column({ label, className }: { label: string; className?: string }) {
  return (
    <th scope="col" className={["px-2 py-2 font-medium", className].filter(Boolean).join(" ")}>
      {label}
    </th>
  );
}

function TeamCell({
  weekNumber,
  team,
}: {
  weekNumber: number;
  team: GridMatchup["homeTeam"];
}) {
  return (
    <td className="px-2 py-2">
      <label className="inline-flex items-center gap-2">
        <Radio
          value={team.id}
          aria-label={`Pick ${team.name} in week ${weekNumber}`}
        />
        <span>{team.name}</span>
        {team.squad !== "" ? (
          <span className="hidden md:inline">{team.squad}</span>
        ) : null}
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

function dateText(value: string): string {
  const match = /^(\d{4}-\d{2}-\d{2})/.exec(value);
  return match?.[1] ?? value;
}

function scoreText(score: number | null): string {
  return score == null ? "" : String(score);
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
