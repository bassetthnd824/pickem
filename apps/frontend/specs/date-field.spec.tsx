import { useForm } from "@tanstack/react-form";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import * as v from "valibot";
import { DateField } from "../components/date-field";
import { fieldErrorMessage } from "../lib/field-error";
import { isoDateSchema } from "../lib/iso-date";

describe("DateField", () => {
  it("uses a native date input", () => {
    const onChange = vi.fn<(value: string) => void>();
    render(
      <DateField
        id="game-date"
        name="gameDate"
        label="Game date"
        value=""
        onChange={onChange}
      />,
    );

    const input = screen.getByLabelText("Game date");
    expect(input.tagName).toBe("INPUT");
    expect(input.getAttribute("type")).toBe("date");
    expect(
      document.querySelector(".bootstrap-datetimepicker-widget"),
    ).toBeNull();

    fireEvent.change(input, { target: { value: "2026-10-12" } });
    expect(onChange).toHaveBeenCalledWith("2026-10-12");
  });
});

describe("isoDateSchema", () => {
  it("accepts a calendar date and rejects a moment-style string", () => {
    expect(v.safeParse(isoDateSchema, "2026-10-12").success).toBe(true);
    expect(v.safeParse(isoDateSchema, "10/12/2026").success).toBe(false);
    expect(v.safeParse(isoDateSchema, "2026-02-31").success).toBe(false);
  });
});

function GameDateForm() {
  const form = useForm({
    defaultValues: {
      gameDate: "",
    },
    onSubmit: () => undefined,
  });

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        void form.handleSubmit();
      }}
    >
      <form.Field
        name="gameDate"
        validators={{
          onSubmit: isoDateSchema,
        }}
      >
        {(field) => (
          <DateField
            id="game-date"
            name={field.name}
            label="Game date"
            value={field.state.value}
            onBlur={field.handleBlur}
            onChange={(value) => {
              field.handleChange(value);
            }}
            error={fieldErrorMessage(field.state.meta.errors)}
          />
        )}
      </form.Field>
      <button type="submit">Save</button>
    </form>
  );
}

describe("GameDateForm", () => {
  it("shows the calendar-date message from the Valibot schema", async () => {
    render(<GameDateForm />);

    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    expect(await screen.findByText("Enter a date as YYYY-MM-DD")).toBeTruthy();

    fireEvent.change(screen.getByLabelText("Game date"), {
      target: { value: "2026-10-12" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => {
      expect(screen.queryByText("Enter a date as YYYY-MM-DD")).toBeNull();
    });
  });
});
