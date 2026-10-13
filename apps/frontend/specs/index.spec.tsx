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
    const heading = screen.getByRole("heading", { name: "Kenney's Pickem" });
    expect(heading.className).toContain("text-primary-fg");
    expect(heading.closest("header")?.className).toContain("bg-primary");
    expect(
      screen.getByLabelText("Correct pick").getAttribute("class"),
    ).toContain("text-success");
    expect(
      screen.getByLabelText("Incorrect pick").getAttribute("class"),
    ).toContain("text-danger");
  });
});
