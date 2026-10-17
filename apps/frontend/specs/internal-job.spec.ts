import { type AxiosRequestConfig } from "axios";
import { NextRequest } from "next/server";
import { POST as catchAll } from "../app/api/[...path]/route";
import { POST as jobRoute } from "../app/api/internal/jobs/[job]/route";
import { verifySchedulerOidc } from "../lib/server/job-oidc";
import {
  handleInternalJob,
  JOB_SECRET_HEADER,
} from "../lib/server/internal-job";
import { springHttp } from "../lib/server/spring-proxy";

vi.mock("../lib/server/job-oidc", () => ({
  verifySchedulerOidc: vi.fn<
    (
      idToken: string,
      audience: string,
      allowedEmails: readonly string[],
    ) => Promise<boolean>
  >(async () => false),
}));

const verify = vi.mocked(verifySchedulerOidc);
const requestMock = vi.spyOn(springHttp, "request");

const ORIGINAL_BASE = process.env.SPRING_API_BASE_URL;
const ORIGINAL_SERVICE = process.env.K_SERVICE;
const ORIGINAL_SECRET = process.env.PICKEM_INTERNAL_JOB_SECRET;
const ORIGINAL_AUDIENCE = process.env.PICKEM_INTERNAL_JOB_AUDIENCE;
const ORIGINAL_OIDC_EMAILS = process.env.PICKEM_INTERNAL_JOB_OIDC_EMAILS;

const SCHEDULER_TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJzYSJ9.sig";

function restoreEnv(): void {
  restore("SPRING_API_BASE_URL", ORIGINAL_BASE);
  restore("K_SERVICE", ORIGINAL_SERVICE);
  restore("PICKEM_INTERNAL_JOB_SECRET", ORIGINAL_SECRET);
  restore("PICKEM_INTERNAL_JOB_AUDIENCE", ORIGINAL_AUDIENCE);
  restore("PICKEM_INTERNAL_JOB_OIDC_EMAILS", ORIGINAL_OIDC_EMAILS);
}

function restore(name: string, value: string | undefined): void {
  if (value === undefined) {
    delete process.env[name];
  } else {
    process.env[name] = value;
  }
}

function configOf(call: number): AxiosRequestConfig {
  const config = requestMock.mock.calls[call]?.[0];
  if (!config || typeof config === "string") {
    throw new Error("expected an axios request config");
  }
  return config;
}

function springOk() {
  return {
    status: 202,
    data: new TextEncoder().encode(JSON.stringify({ job: "cfbd-scores" }))
      .buffer,
    headers: { "content-type": "application/json" },
    statusText: "Accepted",
    config: {},
  };
}

beforeEach(() => {
  restoreEnv();
  verify.mockReset();
  verify.mockResolvedValue(false);
  requestMock.mockReset();
  process.env.SPRING_API_BASE_URL = "http://127.0.0.1:8080";
  delete process.env.K_SERVICE;
  delete process.env.PICKEM_INTERNAL_JOB_SECRET;
  delete process.env.PICKEM_INTERNAL_JOB_AUDIENCE;
  delete process.env.PICKEM_INTERNAL_JOB_OIDC_EMAILS;
});

afterAll(() => {
  restoreEnv();
  requestMock.mockRestore();
});

describe("internal job gate", () => {
  it("rejects a session cookie before Spring sees it", async () => {
    const response = await jobRoute(
      new NextRequest("http://pickem.local/api/internal/jobs/cfbd-scores", {
        method: "POST",
        headers: { cookie: "__session=player-session" },
      }),
      { params: Promise.resolve({ job: "cfbd-scores" }) },
    );
    const text = await response.text();
    expect(response.status).toBe(403);
    expect(text).toContain("session cookie");
    expect(text).not.toContain("player-session");
    expect(requestMock).not.toHaveBeenCalled();
    expect(verify).not.toHaveBeenCalled();
  });

  it("rejects the same call on the catch-all and other prefixes", async () => {
    const internal = await catchAll(
      new NextRequest("http://pickem.local/api/internal/jobs/cfbd-scores", {
        method: "POST",
        headers: { cookie: "__session=player-session" },
      }),
      {
        params: Promise.resolve({
          path: ["internal", "jobs", "cfbd-scores"],
        }),
      },
    );
    expect(internal.status).toBe(403);
    const health = await catchAll(
      new NextRequest("http://pickem.local/api/health"),
      { params: Promise.resolve({ path: ["health"] }) },
    );
    expect(health.status).toBe(404);
    const outside = await catchAll(
      new NextRequest("http://pickem.local/api/admin/export", {
        method: "POST",
      }),
      { params: Promise.resolve({ path: ["admin", "export"] }) },
    );
    expect(outside.status).toBe(404);
    expect(requestMock).not.toHaveBeenCalled();
  });

  it("forwards a shared secret locally without a metadata token or cookie", async () => {
    process.env.PICKEM_INTERNAL_JOB_SECRET = "local-secret";
    requestMock.mockResolvedValue(springOk());

    const response = await handleInternalJob(
      new Request("http://pickem.local/api/internal/jobs/cfbd-scores", {
        method: "POST",
        headers: {
          cookie: "__session=player-session",
          [JOB_SECRET_HEADER]: "local-secret",
          authorization: "Bearer browser-token",
        },
      }),
      "cfbd-scores",
    );
    expect(response.status).toBe(202);
    expect(requestMock).toHaveBeenCalledTimes(1);
    const upstream = configOf(0);
    expect(upstream.url).toBe(
      "http://127.0.0.1:8080/api/internal/jobs/cfbd-scores",
    );
    expect(upstream.method).toBe("POST");
    expect(upstream.headers).toMatchObject({
      "x-pickem-job-secret": "local-secret",
    });
    expect(upstream.headers).not.toHaveProperty("cookie");
    expect(upstream.headers).not.toHaveProperty("authorization");
    expect(verify).not.toHaveBeenCalled();
  });

  it("forwards an OIDC token locally without a Cloud Run identity token", async () => {
    verify.mockResolvedValue(true);
    requestMock.mockResolvedValue(springOk());
    process.env.PICKEM_INTERNAL_JOB_AUDIENCE = "https://pickem.example";
    process.env.PICKEM_INTERNAL_JOB_OIDC_EMAILS =
      "scheduler@pickem.iam.gserviceaccount.com";

    const response = await jobRoute(
      new NextRequest("http://pickem.local/api/internal/jobs/cfbd-matchups", {
        method: "POST",
        headers: {
          cookie: "__session=player-session",
          authorization: `Bearer ${SCHEDULER_TOKEN}`,
        },
      }),
      { params: Promise.resolve({ job: "cfbd-matchups" }) },
    );
    expect(response.status).toBe(202);
    expect(verify).toHaveBeenCalledWith(
      SCHEDULER_TOKEN,
      "https://pickem.example",
      ["scheduler@pickem.iam.gserviceaccount.com"],
    );
    const upstream = configOf(0);
    expect(upstream.url).toBe(
      "http://127.0.0.1:8080/api/internal/jobs/cfbd-matchups",
    );
    expect(upstream.headers).not.toHaveProperty("cookie");
    expect(upstream.headers).not.toHaveProperty("authorization");
    expect(JSON.stringify(upstream.headers)).not.toContain(SCHEDULER_TOKEN);
  });

  it("does not treat a bearer token as Scheduler without audience and email", async () => {
    process.env.PICKEM_INTERNAL_JOB_AUDIENCE = "https://pickem.example";
    const missingEmail = await handleInternalJob(
      new Request("http://pickem.local/api/internal/jobs/cfbd-scores", {
        method: "POST",
        headers: { authorization: `Bearer ${SCHEDULER_TOKEN}` },
      }),
      "cfbd-scores",
    );
    expect(missingEmail.status).toBe(401);
    delete process.env.PICKEM_INTERNAL_JOB_AUDIENCE;
    process.env.PICKEM_INTERNAL_JOB_OIDC_EMAILS =
      "scheduler@pickem.iam.gserviceaccount.com";
    const missingAudience = await handleInternalJob(
      new Request("http://pickem.local/api/internal/jobs/cfbd-scores", {
        method: "POST",
        headers: { authorization: `Bearer ${SCHEDULER_TOKEN}` },
      }),
      "cfbd-scores",
    );
    expect(missingAudience.status).toBe(401);
    expect(verify).not.toHaveBeenCalled();
    expect(requestMock).not.toHaveBeenCalled();
  });

  it("keeps the Cloud Run identity token and still strips the session", async () => {
    process.env.K_SERVICE = "frontend";
    process.env.SPRING_API_BASE_URL = "https://pickem-api.run.app";
    process.env.PICKEM_INTERNAL_JOB_SECRET = "cloud-secret";
    requestMock.mockImplementation(async (config) => {
      const url = typeof config === "string" ? config : String(config?.url);
      if (url.includes("metadata.google.internal")) {
        return {
          status: 200,
          data: "header.payload.sig",
          headers: {},
          statusText: "OK",
          config: {},
        };
      }
      return springOk();
    });

    const response = await handleInternalJob(
      new Request("http://pickem.local/api/internal/jobs/cfbd-scores", {
        method: "POST",
        headers: {
          cookie: "__session=player-session",
          [JOB_SECRET_HEADER]: "cloud-secret",
        },
      }),
      "cfbd-scores",
    );
    expect(response.status).toBe(202);
    expect(String(configOf(0).url)).toContain("metadata.google.internal");
    expect(configOf(1).headers).toMatchObject({
      authorization: "Bearer header.payload.sig",
      "x-pickem-job-secret": "cloud-secret",
    });
    expect(configOf(1).headers).not.toHaveProperty("cookie");
  });

  it("does not call Spring for an unknown job or the wrong method", async () => {
    process.env.PICKEM_INTERNAL_JOB_SECRET = "local-secret";
    const unknown = await handleInternalJob(
      new Request("http://pickem.local/api/internal/jobs/other", {
        method: "POST",
        headers: { [JOB_SECRET_HEADER]: "local-secret" },
      }),
      "other",
    );
    expect(unknown.status).toBe(404);
    const method = await handleInternalJob(
      new Request("http://pickem.local/api/internal/jobs/cfbd-scores", {
        method: "GET",
        headers: { cookie: "__session=player-session" },
      }),
      "cfbd-scores",
    );
    expect(method.status).toBe(405);
    expect(requestMock).not.toHaveBeenCalled();
  });
});
