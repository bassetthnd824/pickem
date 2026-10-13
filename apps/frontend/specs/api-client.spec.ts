import { api, ApiError, resolveApiPath } from "../lib/api-client";

describe("resolveApiPath", () => {
  it("prefixes a relative path with /api", () => {
    expect(resolveApiPath("game/main")).toBe("/api/game/main");
    expect(resolveApiPath("/game/main")).toBe("/api/game/main");
  });

  it("keeps a path that already starts with /api", () => {
    expect(resolveApiPath("/api/game/main")).toBe("/api/game/main");
    expect(resolveApiPath("/api/game/leaderboard?seasonId=2026")).toBe(
      "/api/game/leaderboard?seasonId=2026",
    );
  });

  it("rejects an absolute Spring or Cloud Run origin", () => {
    expect(() => resolveApiPath("http://localhost:8080/api/game/main")).toThrow(
      /same-origin/,
    );
    expect(() =>
      resolveApiPath("https://pickem-abc.run.app/api/game/main"),
    ).toThrow(/same-origin/);
    expect(() => resolveApiPath("//localhost:8080/api/game/main")).toThrow(
      /same-origin/,
    );
  });
});

describe("api", () => {
  const fetchMock = vi.fn<typeof fetch>();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("calls a relative /api path with credentials included", async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ season: 2026 }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );

    await expect(api.get("/game/main")).resolves.toEqual({ season: 2026 });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] ?? [];
    expect(url).toBe("/api/game/main");
    expect(init).toMatchObject({
      method: "GET",
      credentials: "include",
    });
    expect(JSON.stringify(init)).not.toContain("localhost:8080");
    expect(JSON.stringify(init)).not.toContain("run.app");
  });

  it("sends JSON on POST", async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ saved: true }), { status: 200 }),
    );

    await api.post("/game/picks", { seasonWeekId: "week-1", picks: [] });

    const [url, init] = fetchMock.mock.calls[0] ?? [];
    expect(url).toBe("/api/game/picks");
    expect(init).toMatchObject({
      method: "POST",
      credentials: "include",
      body: JSON.stringify({ seasonWeekId: "week-1", picks: [] }),
    });
    const headers = new Headers(init?.headers);
    expect(headers.get("Content-Type")).toBe("application/json");
    expect(headers.get("Accept")).toBe("application/json");
  });

  it("does not call fetch for an off-origin path", async () => {
    await expect(
      api.get("https://pickem-abc.run.app/api/game/main"),
    ).rejects.toThrow(/same-origin/);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("throws ApiError with the response body", async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ message: "locked" }), { status: 409 }),
    );

    const error = await api
      .post("/game/picks", {})
      .catch((caught: unknown) => caught);
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 409, body: { message: "locked" } });
  });

  it("returns undefined for 204", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));
    await expect(api.delete("/game/picks")).resolves.toBeUndefined();
  });
});
