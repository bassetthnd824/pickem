import { render, screen } from "@testing-library/react";
import { PickResultIcon } from "../components/pick-result-icon";

describe("PickResultIcon", () => {
  it("colors a correct pick with the success token", () => {
    render(<PickResultIcon result="correct" />);
    const icon = screen.getByLabelText("Correct pick");
    const className = icon.getAttribute("class") ?? "";
    expect(className).toContain("text-success");
    expect(className).not.toContain("text-danger");
    expect(icon.outerHTML).not.toMatch(/#[0-9a-fA-F]{3,8}/);
  });

  it("colors an incorrect pick with the danger token", () => {
    render(<PickResultIcon result="incorrect" />);
    const icon = screen.getByLabelText("Incorrect pick");
    const className = icon.getAttribute("class") ?? "";
    expect(className).toContain("text-danger");
    expect(className).not.toContain("text-success");
  });
});
