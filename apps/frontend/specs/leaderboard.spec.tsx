import { render, screen } from "@testing-library/react";
import { LeaderboardTable } from "../components/leaderboard-table";
import { QueryProvider } from "../components/query-provider";
import { parseLeaderboard } from "../lib/leaderboard";

const board = {
  seasonId: "season-1",
  season: "2026",
  standings: [
    { rank: 1, uid: "ada", nickName: "Ada", score: 12 },
    { rank: 2, uid: "cy", nickName: "Zed", score: 12 },
    { rank: 3, uid: "bea", nickName: null, score: 0 },
  ],
};

describe("parseLeaderboard", () => {
  it("keeps US-10 order and scores, including a missing nickname", () => {
    expect(parseLeaderboard(board)).toEqual({
      seasonId: "season-1",
      season: "2026",
      standings: [
        { rank: 1, uid: "ada", nickName: "Ada", score: 12 },
        { rank: 2, uid: "cy", nickName: "Zed", score: 12 },
        { rank: 3, uid: "bea", nickName: "", score: 0 },
      ],
    });
  });

  it("rejects a payload that is not the leaderboard shape", () => {
    expect(() => parseLeaderboard(null)).toThrow("Leaderboard response was invalid");
    expect(() =>
      parseLeaderboard({
        seasonId: "season-1",
        season: "2026",
        standings: [{ rank: 1, uid: "ada", nickName: "Ada", score: 1.5 }],
      }),
    ).toThrow("Leaderboard response was invalid");
    expect(() =>
      parseLeaderboard({
        seasonId: " ",
        season: "2026",
        standings: [],
      }),
    ).toThrow("Leaderboard response was invalid");
  });
});

describe("LeaderboardTable", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows rank, nickname, and score in API order", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: RequestInfo | URL) => {
        expect(String(input)).toBe("/api/game/leaderboard");
        return Response.json(board);
      }),
    );

    render(
      <QueryProvider>
        <LeaderboardTable />
      </QueryProvider>,
    );

    expect(screen.getByText("Loading leaderboard.")).toBeTruthy();
    expect(await screen.findByRole("columnheader", { name: "Rank" })).toBeTruthy();
    expect(screen.getByRole("columnheader", { name: "User" })).toBeTruthy();
    expect(screen.getByRole("columnheader", { name: "Score" })).toBeTruthy();

    const rows = screen.getAllByRole("row").slice(1);
    expect(rows.map((row) => row.textContent)).toEqual(["1Ada12", "2Zed12", "30"]);
    const ada = screen.getByText("Ada");
    const zed = screen.getByText("Zed");
    expect(ada.compareDocumentPosition(zed) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it("shows an empty table when the season has no users", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        Response.json({ seasonId: "season-1", season: "2026", standings: [] }),
      ),
    );

    render(
      <QueryProvider>
        <LeaderboardTable />
      </QueryProvider>,
    );

    expect(await screen.findByRole("columnheader", { name: "User" })).toBeTruthy();
    expect(screen.getAllByRole("row")).toHaveLength(1);
  });

  it("reports a failed load", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("missing", { status: 404 })),
    );

    render(
      <QueryProvider>
        <LeaderboardTable />
      </QueryProvider>,
    );

    const alert = await screen.findByRole("alert");
    expect(alert.textContent).toBe("Could not load the leaderboard.");
  });
});
