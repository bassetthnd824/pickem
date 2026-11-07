import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { PicksGrid } from "../components/picks-grid";
import { QueryProvider } from "../components/query-provider";

const main = {
  seasonId: "season-1",
  season: "2026",
  numberOfConferenceTeams: 16,
  weeks: [
    {
      id: "week-1",
      weekNumber: 1,
      beginDate: "2026-09-03",
      endDate: "2026-09-09",
      currentOrPast: true,
      matchups: [
        {
          matchupId: "correct",
          awayTeam: { id: "auburn", name: "Auburn" },
          awayTeamScore: 14,
          homeTeam: { id: "alabama", name: "Alabama" },
          homeTeamScore: 31,
          pickedTeamId: "alabama",
          rank: 5,
          winningTeamId: "alabama",
        },
        {
          matchupId: "wrong",
          awayTeam: { id: "georgia", name: "Georgia" },
          awayTeamScore: 24,
          homeTeam: { id: "florida", name: "Florida" },
          homeTeamScore: 10,
          pickedTeamId: "florida",
          rank: 1,
          winningTeamId: "georgia",
        },
        {
          matchupId: "unscored",
          awayTeam: { id: "kentucky", name: "Kentucky" },
          awayTeamScore: null,
          homeTeam: { id: "tennessee", name: "Tennessee" },
          homeTeamScore: null,
          pickedTeamId: "tennessee",
          rank: 2,
          winningTeamId: null,
        },
        {
          matchupId: "tied",
          awayTeam: { id: "missouri", name: "Missouri" },
          awayTeamScore: 17,
          homeTeam: { id: "arkansas", name: "Arkansas" },
          homeTeamScore: 17,
          pickedTeamId: "arkansas",
          rank: 3,
          winningTeamId: "arkansas",
        },
      ],
    },
    {
      id: "week-2",
      weekNumber: 2,
      beginDate: "2026-09-10",
      endDate: "2026-09-16",
      currentOrPast: false,
      matchups: [
        {
          matchupId: "future-a",
          awayTeam: { id: "lsu", name: "LSU" },
          awayTeamScore: 99,
          homeTeam: { id: "ole-miss", name: "Ole Miss" },
          homeTeamScore: 1,
          pickedTeamId: "lsu",
          rank: 3,
          winningTeamId: "ole-miss",
        },
        {
          matchupId: "future-b",
          awayTeam: { id: "texas", name: "Texas" },
          awayTeamScore: 40,
          homeTeam: { id: "oklahoma", name: "Oklahoma" },
          homeTeamScore: 21,
          pickedTeamId: "oklahoma",
          rank: 2,
          winningTeamId: "texas",
        },
        {
          matchupId: "future-c",
          awayTeam: { id: "vandy", name: "Vanderbilt" },
          awayTeamScore: null,
          homeTeam: { id: "carolina", name: "South Carolina" },
          homeTeamScore: null,
          pickedTeamId: "vandy",
          rank: 1,
          winningTeamId: null,
        },
      ],
    },
  ],
};

function renderGrid() {
  return render(
    <QueryProvider>
      <PicksGrid />
    </QueryProvider>,
  );
}

describe("PicksGrid", () => {
  const calls: { url: string; method: string; body: string | null }[] = [];

  beforeEach(() => {
    calls.length = 0;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input);
        const method = init?.method ?? "GET";
        calls.push({
          url,
          method,
          body: typeof init?.body === "string" ? init.body : null,
        });
        if (url === "/api/game/main") {
          return Response.json(main);
        }
        if (url === "/api/game/picks" && method === "POST") {
          return Response.json([]);
        }
        return new Response("missing", { status: 404 });
      }),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("locks the begun week and hides future scores and result icons", async () => {
    renderGrid();
    const begun = within(await screen.findByRole("region", { name: "Week 1" }));
    const future = within(screen.getByRole("region", { name: "Week 2" }));

    expect(begun.getByText("Picks are locked")).toBeTruthy();
    expectDisabled(begun.getByRole("radio", { name: "Pick Alabama in week 1" }));
    expectDisabled(begun.getByRole("radio", { name: "Pick Auburn in week 1" }));
    expectDisabled(begun.getByLabelText("Rank for Auburn at Alabama"));
    expectDisabled(begun.getByRole("button", { name: "Move Auburn at Alabama up" }));
    expectDisabled(begun.getByRole("button", { name: "Move Auburn at Alabama down" }));
    expectDisabled(begun.getByRole("button", { name: "Save week 1" }));
    expect(begun.getByText("14 - 31")).toBeTruthy();
    expect(begun.getByText("24 - 10")).toBeTruthy();
    expect(begun.getByLabelText("Correct pick")).toBeTruthy();
    expect(begun.getByLabelText("Incorrect pick")).toBeTruthy();
    expect(begun.getAllByLabelText(/^(Correct|Incorrect) pick$/)).toHaveLength(2);

    expect(future.getByText("Picks are open")).toBeTruthy();
    expectEnabled(future.getByRole("radio", { name: "Pick LSU in week 2" }));
    expectEnabled(future.getByRole("radio", { name: "Pick Ole Miss in week 2" }));
    expectEnabled(future.getByLabelText("Rank for LSU at Ole Miss"));
    expectEnabled(future.getByRole("button", { name: "Move LSU at Ole Miss down" }));
    expectEnabled(future.getByRole("button", { name: "Save week 2" }));
    expect(future.queryByText("99 - 1")).toBeNull();
    expect(future.queryByText("40 - 21")).toBeNull();
    expect(future.queryByLabelText("Correct pick")).toBeNull();
    expect(future.queryByLabelText("Incorrect pick")).toBeNull();
    expect(future.queryByRole("columnheader", { name: "Score" })).toBeNull();
    expect(future.queryByRole("columnheader", { name: "Result" })).toBeNull();
  });

  it("saves an edited future week and reorders ranks", async () => {
    renderGrid();
    const future = within(await screen.findByRole("region", { name: "Week 2" }));

    fireEvent.click(future.getByRole("radio", { name: "Pick Ole Miss in week 2" }));
    fireEvent.click(future.getByRole("button", { name: "Move LSU at Ole Miss down" }));

    const lsuRank = future.getByLabelText("Rank for LSU at Ole Miss");
    const texasRank = future.getByLabelText("Rank for Texas at Oklahoma");
    expect((lsuRank as HTMLInputElement).value).toBe("2");
    expect((texasRank as HTMLInputElement).value).toBe("3");
    expect(
      texasRank.compareDocumentPosition(lsuRank) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
    expectEnabled(future.getByRole("button", { name: "Move LSU at Ole Miss down" }));

    fireEvent.click(future.getByRole("button", { name: "Move LSU at Ole Miss down" }));
    expect((future.getByLabelText("Rank for LSU at Ole Miss") as HTMLInputElement).value).toBe(
      "1",
    );
    expect(
      (future.getByLabelText("Rank for Texas at Oklahoma") as HTMLInputElement).value,
    ).toBe("3");
    expect(
      (future.getByLabelText("Rank for Vanderbilt at South Carolina") as HTMLInputElement)
        .value,
    ).toBe("2");
    expectDisabled(future.getByRole("button", { name: "Move LSU at Ole Miss down" }));

    fireEvent.click(future.getByRole("button", { name: "Save week 2" }));

    await waitFor(() => {
      expect(calls.some((call) => call.method === "POST")).toBe(true);
    });
    const post = calls.find((call) => call.method === "POST");
    expect(post?.url).toBe("/api/game/picks");
    expect(JSON.parse(post?.body ?? "{}")).toEqual({
      seasonWeekId: "week-2",
      picks: [
        { matchupId: "future-b", pickedTeamId: "oklahoma", rank: 3 },
        { matchupId: "future-c", pickedTeamId: "vandy", rank: 2 },
        { matchupId: "future-a", pickedTeamId: "ole-miss", rank: 1 },
      ],
    });
  });

  it("keeps a rank when a pick moves onto an unpicked row", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input);
        const method = init?.method ?? "GET";
        calls.push({
          url,
          method,
          body: typeof init?.body === "string" ? init.body : null,
        });
        if (url === "/api/game/main") {
          return Response.json({
            ...main,
            weeks: [
              {
                ...main.weeks[1],
                matchups: [
                  main.weeks[1].matchups[0],
                  {
                    ...main.weeks[1].matchups[2],
                    pickedTeamId: null,
                    rank: null,
                  },
                ],
              },
            ],
          });
        }
        if (method === "POST") {
          return Response.json([]);
        }
        return new Response("missing", { status: 404 });
      }),
    );

    renderGrid();
    const future = within(await screen.findByRole("region", { name: "Week 2" }));
    fireEvent.click(future.getByRole("button", { name: "Move LSU at Ole Miss down" }));
    expect(
      (future.getByLabelText("Rank for LSU at Ole Miss") as HTMLInputElement).value,
    ).toBe("3");
    expect(
      (
        future.getByLabelText("Rank for Vanderbilt at South Carolina") as HTMLInputElement
      ).value,
    ).toBe("");
    fireEvent.click(future.getByRole("button", { name: "Save week 2" }));

    await waitFor(() => {
      expect(calls.some((call) => call.method === "POST")).toBe(true);
    });
    const post = calls.find((call) => call.method === "POST");
    expect(JSON.parse(post?.body ?? "{}")).toEqual({
      seasonWeekId: "week-2",
      picks: [
        { matchupId: "future-c", pickedTeamId: null, rank: null },
        { matchupId: "future-a", pickedTeamId: "lsu", rank: 3 },
      ],
    });
  });

  it("shows the server message when a save is rejected", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input);
        const method = init?.method ?? "GET";
        if (url === "/api/game/main") {
          return Response.json({
            ...main,
            weeks: main.weeks.map((week) => ({ ...week, currentOrPast: false })),
          });
        }
        if (method === "POST") {
          return Response.json({ detail: "picks are locked" }, { status: 409 });
        }
        return new Response("missing", { status: 404 });
      }),
    );

    renderGrid();
    const week = within(await screen.findByRole("region", { name: "Week 1" }));
    fireEvent.click(week.getByRole("button", { name: "Save week 1" }));
    expect((await screen.findByRole("alert")).textContent).toContain("picks are locked");
  });
});

function expectDisabled(element: HTMLElement) {
  expect((element as HTMLButtonElement).disabled).toBe(true);
}

function expectEnabled(element: HTMLElement) {
  expect((element as HTMLButtonElement).disabled).toBe(false);
}
