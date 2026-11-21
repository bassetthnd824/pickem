# Theme colors

Closed catalog of **18** themes: generic **Light** and **Dark**, plus one per
**SEC school (16)**. Source of truth for CSS `data-theme` keys (US-14) and the
`themes` seed documents (US-12). Hex values for the 16 schools are the official
primary / secondary colors from the modernization plan (teamcolorcodes.com /
school brand guides).

Managers may activate, deactivate, and rename these rows. They cannot add a
palette that is not compiled into CSS. Theme `themePath` is the **theme key**
and must not begin with `/`.

Default theme is **Light** (`light`). First visit with no user `themeId` uses
Light. Users already on a deactivated theme keep it until they pick another.

## Semantic tokens

Every theme supplies these CSS custom properties. Components use semantic
utilities (`bg-surface`, `text-primary`) and never hard-code a hex.

| Token                | Role                                                          |
| -------------------- | ------------------------------------------------------------- |
| `--color-bg`         | Page canvas (`#f5f5f5` on light themes)                       |
| `--color-surface`    | Cards and panels                                              |
| `--color-text`       | Body text (`#333333` on light themes)                         |
| `--color-heading`    | Titles on the canvas                                          |
| `--color-muted`      | Secondary text                                                |
| `--color-border`     | Card and table lines (`#bcc3cd` on light themes)              |
| `--color-field`      | Input borders                                                 |
| `--color-primary`    | Page band and card edge (school primary)                      |
| `--color-primary-fg` | Text on `--color-primary`                                     |
| `--color-secondary`  | School secondary                                              |
| `--color-accent`     | School highlight when the secondary is not white or black     |
| `--color-highlight`  | Focus and outline buttons. Motion blue `#0C6CD4`; sky on Dark |
| `--color-action`     | Filled buttons. Motion blue on light themes; sky on Dark      |
| `--color-action-fg`  | Text on `--color-action`                                      |
| `--color-wash`       | Tinted table header and status chip, mixed from primary       |
| `--color-success`    | Correct-pick icon                                             |
| `--color-danger`     | Incorrect-pick icon                                           |

Selector: `[data-theme="{key}"]` on `<html>`.

Type, spacing, and corners follow Motion's measured scale: Source Sans at
16px/20px body, 30px/36px page titles, 26px/30px section titles, and 22px/28px
subsections, all bold. Controls are 10px radius. Filled and outline buttons
are 48px tall with 24px of side padding. The nav is a white bar. School color is the page-title band and the card's top edge. Filled and
outline buttons, links, and focus use Motion blue (`#0C6CD4` on light themes,
sky on Dark).

**WCAG AA:** text on surfaces and `--color-primary-fg` on `--color-primary`
must meet AA (US-23). Very dark primaries (Vanderbilt / Missouri black, Texas
A&M / South Carolina deep maroon / garnet) use a **light neutral page
surface** with the team color on the page band — not as the
full-page background. Where the secondary is effectively **white** (Arkansas,
Texas A&M), derive a neutral surface and use the primary as the accent.

## Catalog (18)

| Name                       | Theme key           | Primary                    | Secondary / accent    |
| -------------------------- | ------------------- | -------------------------- | --------------------- |
| Light                      | `light`             | Slate `#1F2937`            | Blue `#2563EB`        |
| Dark                       | `dark`              | Slate `#0F172A`            | Sky `#38BDF8`         |
| Alabama Crimson Tide       | `alabama`           | Crimson `#9E1B32`          | Cool Gray `#828A8F`   |
| Arkansas Razorbacks        | `arkansas`          | Cardinal Red `#9D2235`     | White `#FFFFFF`       |
| Auburn Tigers              | `auburn`            | Navy Blue `#0C2340`        | Orange `#E87722`      |
| Florida Gators             | `florida`           | Blue `#0021A5`             | Orange `#FA4616`      |
| Georgia Bulldogs           | `georgia`           | Bulldog Red `#BA0C2F`      | Black `#000000`       |
| Kentucky Wildcats          | `kentucky`          | Wildcat Blue `#0033A0`     | Black `#000000`       |
| LSU Tigers                 | `lsu`               | Purple `#461D7C`           | Gold `#FDD023`        |
| Mississippi State Bulldogs | `mississippi-state` | Maroon `#660000`           | Gray `#75787B`        |
| Missouri Tigers            | `missouri`          | Black `#000000`            | MU Gold `#F1B82D`     |
| Oklahoma Sooners           | `oklahoma`          | Crimson `#841617`          | Cream `#FDF9D8`       |
| Ole Miss Rebels            | `ole-miss`          | Red `#CE1126`              | Navy Blue `#14213D`   |
| South Carolina Gamecocks   | `south-carolina`    | Garnet `#73000A`           | Black `#000000`       |
| Tennessee Volunteers       | `tennessee`         | Tennessee Orange `#FF8200` | Smokey Gray `#58595B` |
| Texas Longhorns            | `texas`             | Burnt Orange `#BF5700`     | Dark Gray `#333F48`   |
| Texas A&M Aggies           | `texas-am`          | Maroon `#500000`           | White `#FFFFFF`       |
| Vanderbilt Commodores      | `vanderbilt`        | Black `#000000`            | Old Gold `#866D4B`    |

Light and Dark are baseline palettes (not school brand colors). Light is a
light surface with slate chrome and a blue accent. Dark is a dark surface with
slate chrome and a sky accent.

## Notes for derived surfaces

- **Missouri:** MU Gold is the distinctive color but black is the brand
  primary — gold accent on a dark/black primary, light-enough text surface.
- **Arkansas, Texas A&M:** secondary is white — primary as accent, neutral
  surface, WCAG AA body text.
- **Vanderbilt, Missouri, Texas A&M, South Carolina:** do not paint the whole
  page with the team color.

Correct / incorrect pick icons always use `--color-success` / `--color-danger`,
not school colors.
