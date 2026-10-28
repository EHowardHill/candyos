# Phase tracker

`TASKS.md` is the CandyOS working tracker — open work, loose ends, and
the QA backlog. It previously lived at `frameworks/base/TASKS.md` in the
old dev forks and was relocated here, unchanged, when the project moved
to the single-repo model.

REVISED 2026-10-28 (user direction): the file was rewritten as a working
tracker and the per-phase P0–P27 historical record was cleared out of it.
That record is not lost — it lives in this file's git history (commits up
to 8fadfe8) and in the forks' tagged history (kitkat/p<n> phase tags on
the EHowardHill/* read-only mirrors). What ships today is mapped in
[docs/architecture.md](../architecture.md).

* P0–P27 are closed. Commits referencing "frameworks/base/TASKS.md" or
  "TASKS.md Phase N" refer to the historical record.
* Open work: P28 (KitKat shade + QS re-land), P29 (lock screen),
  P30 (Settings pass), P31 (single-repo migration loose ends), plus the
  deferred backlog and the QA passes riding the next builds — all in
  `TASKS.md`. Pick one up, and record progress there as you go.