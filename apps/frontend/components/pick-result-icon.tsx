type PickResult = "correct" | "incorrect";

type PickResultIconProps = {
  result: PickResult;
};

export function PickResultIcon({ result }: PickResultIconProps) {
  const correct = result === "correct";
  const label = correct ? "Correct pick" : "Incorrect pick";

  return (
    <svg
      aria-label={label}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2.5}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={correct ? "h-6 w-6 text-success" : "h-6 w-6 text-danger"}
    >
      <title>{label}</title>
      {correct ? (
        <path d="M5 13l4 4L19 7" />
      ) : (
        <path d="M6 6l12 12M18 6L6 18" />
      )}
    </svg>
  );
}
