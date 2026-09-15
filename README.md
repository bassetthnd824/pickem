# Kenney's Pickem

A season-long college football **confidence pick'em**. Each week, players pick a winner for every matchup and rank those picks by confidence. A correct pick scores its rank in points; the leaderboard is the season total.

This repository is the original Java EE application. A planned rewrite (Next.js + Spring Boot + Firestore) is documented under [`docs/`](docs/).

## How the game works

1. A **manager** sets up a season, weeks, teams, venues, rivalries, and weekly matchups.
2. A **player** picks a winner for each game in the current week.
3. Picks are ordered by **confidence rank**. Rank `N` is the player's most confident pick, down to `1`, where `N` is the number of conference teams.
4. After scores are entered, a correct pick earns its rank. Wrong picks earn `0`.
5. The **leaderboard** sums those points for the season.

Picks for a week are considered locked once that week's begin date has arrived. Results show for the current and past weeks.

## Features

**Players** (`/game/*`)

- Weekly picks grid with up/down reordering for confidence rank
- Season leaderboard
- Team schedule (win/loss for a selected team)
- Account: nickname, theme, and password

**Managers** (`/manager/*`)

CRUD for users, themes, teams, venues, rivalries, seasons, season weeks, and matchups.

Auth is container-managed **JAAS FORM** login on WildFly (`pickemDomain`). Roles are `player` and `manager`. New users can self-register.

## Stack

| Layer | Technology |
|---|---|
| App server | JBoss WildFly (Java EE 7 BOM 8.2.1) |
| Language | Java 7 |
| Web | Struts 2.3.24, Apache Tiles 3, JSP |
| UI | Bootstrap 3, jQuery, jQuery Mobile, Underscore |
| Persistence | JPA / Hibernate, Apache Derby |
| Services | CDI + EJB `@Stateless` beans |
| Validation | Bean Validation (JSR-303) |
| Build | Maven WAR (`com.curleesoft.pickem:pickem`) |

## Project layout

```
pickem/
  pom.xml
  docs/                          # modernization plan and user stories
  src/main/java/.../pickem/
    action/                      # Struts actions (player + manager)
    bean/                        # EJB/Hibernate DAOs
    model/                       # JPA entities (PCKM_* tables)
    form/                        # form DTOs
    filter/                      # login filter
  src/main/resources/
    struts.xml
    META-INF/persistence.xml
  src/main/webapp/
    tiles/                       # JSP views
    WEB-INF/                     # web.xml, Tiles, Derby datasource
    resources/                   # CSS, JS, images
```

Database tables are prefixed `PCKM_` (for example `PCKM_USER`, `PCKM_MATCHUP`, `PCKM_USER_PICK`).

## Prerequisites

- JDK 7
- Apache Maven 3.x
- WildFly (matching the Java EE 7 / 8.2.1 stack)
- Apache Derby, network server on port `1527`

The unmanaged datasource in `src/main/webapp/WEB-INF/pickem-ds.xml` expects:

```
jdbc:derby://localhost:1527/c:/DATA/derby/databases/pickem
user: pickem
password: pickempass
```

Adjust the URL, credentials, and WildFly security domain (`pickemDomain` in `jboss-web.xml`) for your environment.

## Build and deploy

```bash
mvn package
mvn package wildfly:deploy
```

Tests are skipped by default. To run Arquillian tests against WildFly:

```bash
mvn clean test -Parq-wildfly-managed
mvn clean test -Parq-wildfly-remote
```

After deploy, the WAR context root is `pickem`.

## Docs

- [Modernization plan](docs/modernization-plan.md) — rewrite to Next.js, Spring Boot, and Firestore
- [User stories](docs/user-stories.md) — implementable work for that rewrite
