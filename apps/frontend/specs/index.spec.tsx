import { render, screen } from "@testing-library/react";
import { QueryProvider } from "../components/query-provider";
import Page from "../app/page";

describe("Page", () => {
  it("renders the pickem shell", () => {
    render(
      <QueryProvider>
        <Page />
      </QueryProvider>,
    );
    expect(
      screen.getByRole("heading", { name: "Kenney's Pickem" }),
    ).toBeTruthy();
  });
});
