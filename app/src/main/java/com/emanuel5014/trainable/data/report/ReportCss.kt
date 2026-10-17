package com.emanuel5014.trainable.data.report

/**
 * Style of the HTML report. Written for the narrowest screen first and widened from there, with what every browser
 * of the last years understands: no web fonts, no `color-mix`, no flex `gap`, no sideways scrolling anywhere (long
 * names, notes and prescriptions wrap), a dark theme that follows the system and a clean print.
 */
internal object ReportCss {

    private const val LIGHT = """
        --bg: #F3F4F9; --surface: #FFFFFF; --surface-2: #F0F2F9; --line: #E0E3EE;
        --text: #14161F; --muted: #575C70;
        --primary: #3A59D1; --on-primary: #FFFFFF; --primary-soft: #E6EAFD;
        --accent: #86477E; --accent-soft: #F6E8F4;
        --note-bg: #FFF6D6; --note-line: #D9A400; --note-text: #453400;
        --danger: #B3261E; --danger-soft: #FCE9E7; --ok: #1B7A46; --ok-soft: #E1F4E8;
        --shadow: 0 1px 2px rgba(20, 22, 31, .06), 0 6px 16px rgba(20, 22, 31, .05);
    """

    private const val DARK = """
        --bg: #0F1117; --surface: #171A23; --surface-2: #1F2330; --line: #2A3042;
        --text: #E9EBF3; --muted: #A2A8BD;
        --primary: #9FB2FF; --on-primary: #0B1230; --primary-soft: #232D5C;
        --accent: #E6B5DF; --accent-soft: #392538;
        --note-bg: #2B2612; --note-line: #C9A227; --note-text: #F2E3A4;
        --danger: #FFB4AB; --danger-soft: #45201D; --ok: #86DBA7; --ok-soft: #173826;
        --shadow: none;
    """

    val STYLE: String = """
:root { color-scheme: light dark; $LIGHT }
@media (prefers-color-scheme: dark) { :root { $DARK } }

*, *::before, *::after { box-sizing: border-box; }
html { -webkit-text-size-adjust: 100%; text-size-adjust: 100%; scroll-behavior: smooth; }
body {
  margin: 0; background: var(--bg); color: var(--text);
  font: 16px/1.5 system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif;
  -webkit-font-smoothing: antialiased;
  overflow-wrap: break-word; overflow-wrap: anywhere;
}
h1, h2, h3, h4, p, ul, ol { margin: 0; }
ul, ol { padding: 0; list-style: none; }
svg { display: block; flex: none; }
a { color: var(--primary); }
a:focus-visible, summary:focus-visible { outline: 3px solid var(--primary); outline-offset: 2px; }
.sr { position: absolute; width: 1px; height: 1px; margin: -1px; padding: 0; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
.txt { white-space: pre-wrap; }

/* Page */
.page {
  max-width: 56rem; margin: 0 auto; padding: 16px 16px 140px;
  padding-left: max(16px, env(safe-area-inset-left)); padding-right: max(16px, env(safe-area-inset-right));
}
.plans-nav { margin-bottom: 40px; }
.plans-nav .section-title { margin-top: 4px; }
.plan + .plan { margin-top: 56px; padding-top: 40px; border-top: 2px dashed var(--line); }
.section-title { font-size: 1.05rem; font-weight: 800; margin: 28px 0 10px; }
.empty { margin-top: 24px; padding: 20px; text-align: center; color: var(--muted); background: var(--surface); border: 1px dashed var(--line); border-radius: 14px; }

/* Header */
.eyebrow { font-size: .75rem; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; color: var(--primary); }
.title { font-size: 1.65rem; line-height: 1.2; font-weight: 800; letter-spacing: -.02em; margin-top: 4px; }
.facts { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin-top: 16px; }
.fact { min-width: 0; padding: 12px 14px; background: var(--surface); border: 1px solid var(--line); border-radius: 14px; }
.fact-wide { grid-column: 1 / -1; }
.fact-value { display: block; font-size: 1.15rem; line-height: 1.3; font-weight: 800; font-variant-numeric: tabular-nums; }
.fact-label { display: block; margin-top: 2px; font-size: .75rem; color: var(--muted); }

/* Notes: the same yellow note wherever someone wrote something */
.note, .set-note { display: flex; align-items: flex-start; background: var(--note-bg); color: var(--note-text); border: 1px solid var(--note-line); border-left-width: 4px; }
.note { margin-top: 14px; padding: 12px 14px; border-radius: 12px; }
.note svg, .set-note svg { margin-right: 10px; margin-top: 3px; }
.note-body, .set-note .txt { flex: 1; min-width: 0; }
.note-label { font-size: .7rem; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; opacity: .85; }
.set-note { margin-top: 8px; padding: 7px 10px; border-radius: 8px; font-size: .88rem; }
.set-note svg { margin-right: 8px; }

/* Contents */
.toc-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 15rem), 1fr)); gap: 6px; }
.toc-link { display: flex; align-items: center; min-height: 44px; padding: 6px 12px; color: var(--text); text-decoration: none; background: var(--surface); border: 1px solid var(--line); border-radius: 12px; }
.toc-n { flex: none; width: 26px; height: 26px; margin-right: 10px; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: var(--primary-soft); color: var(--primary); font-size: .75rem; font-weight: 800; }
.toc-name { flex: 1; min-width: 0; font-weight: 600; line-height: 1.3; }
.toc-meta { flex: none; margin-left: 8px; font-size: .75rem; color: var(--muted); white-space: nowrap; }

/* Exercise card */
.exercise { margin-top: 12px; background: var(--surface); border: 1px solid var(--line); border-radius: 18px; box-shadow: var(--shadow); }
.exercise.is-removed { border-style: dashed; }
.ex-head { display: block; padding: 14px; cursor: pointer; list-style: none; -webkit-tap-highlight-color: transparent; border-radius: 18px; }
.ex-head::-webkit-details-marker { display: none; }
.ex-row { display: flex; align-items: center; min-height: 28px; }
.ex-num { flex: none; width: 32px; height: 32px; margin-right: 12px; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: var(--primary); color: var(--on-primary); font-size: .85rem; font-weight: 800; }
.ex-id { flex: 1; min-width: 0; display: block; }
.ex-name { display: block; font-size: 1.05rem; line-height: 1.3; font-weight: 800; }
.ex-tags { display: flex; flex-wrap: wrap; margin: 3px -3px -3px; }
.ex-tags .tag { margin: 3px; }
.ex-chevron { flex: none; margin-left: 10px; color: var(--muted); transition: transform .15s; }
details[open] > .ex-head .ex-chevron { transform: rotate(180deg); }
.ex-body { padding: 0 14px 16px; }
.sub-title { margin: 22px 0 8px; font-size: .75rem; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; color: var(--muted); }
.back-link { display: inline-flex; align-items: center; margin-top: 16px; padding: 6px 0; font-size: .8rem; font-weight: 700; text-decoration: none; }
.back-link svg { margin-right: 6px; }

/* Chips */
.chip, .tag { display: inline-block; padding: 2px 9px; border-radius: 10px; font-size: .72rem; font-weight: 700; line-height: 1.5; background: var(--surface-2); color: var(--muted); }
.chip-plan, .chip-week, .chip-kind { background: var(--primary-soft); color: var(--primary); }
.chip-done, .chip-scope-plan { background: var(--accent-soft); color: var(--accent); }
.chip-scope-workout { background: var(--surface); }
.chip-ok { background: var(--ok-soft); color: var(--ok); }
.chip-miss { background: var(--danger-soft); color: var(--danger); }
.chip-over { background: var(--primary-soft); color: var(--primary); }
.chip-rpe { background: var(--surface-2); color: var(--text); }
.tag-removed { background: var(--danger-soft); color: var(--danger); }
.chips { display: flex; flex-wrap: wrap; margin: 6px -2px -2px; }
.chips .chip { margin: 2px; }

/* Figures of an exercise */
.metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 7rem), 1fr)); gap: 8px; }
.metric { min-width: 0; padding: 10px 12px; background: var(--surface-2); border-radius: 12px; }
.metric-label { display: block; font-size: .72rem; color: var(--muted); }
.metric-value { display: block; margin-top: 2px; font-size: 1.05rem; line-height: 1.3; font-weight: 800; font-variant-numeric: tabular-nums; }
.metric-hint { display: block; margin-top: 1px; font-size: .72rem; color: var(--muted); }

/* Program changes and swaps */
.events { display: grid; grid-template-columns: minmax(0, 1fr); gap: 8px; }
.event { padding: 10px 12px; background: var(--surface-2); border-left: 4px solid var(--primary); border-radius: 12px; }
.event-swap { border-left-color: var(--accent); }
.event-head { display: flex; flex-wrap: wrap; align-items: center; margin: -3px; }
.event-head > * { margin: 3px; }
.event-date { font-size: .85rem; font-weight: 700; }
.changes { margin-top: 8px; }
.changes li { position: relative; margin-top: 3px; padding-left: 16px; font-size: .9rem; font-weight: 700; color: var(--primary); }
.changes li::before { content: ""; position: absolute; left: 2px; top: .6em; width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
.compare { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 14rem), 1fr)); gap: 8px; margin-top: 10px; }
.compare-col { min-width: 0; padding: 8px 10px; background: var(--surface); border: 1px solid var(--line); border-radius: 10px; }
.compare-label { margin-bottom: 4px; font-size: .68rem; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; color: var(--muted); }
.blocks li { display: flex; align-items: baseline; margin-top: 3px; font-size: .9rem; }
.blk-n { flex: none; width: 1.5rem; font-size: .75rem; font-weight: 700; color: var(--muted); }
.blk-t { flex: 1; min-width: 0; }
.blocks .none { color: var(--muted); }
.full-toggle { display: block; margin-top: 4px; padding: 8px 0; cursor: pointer; list-style: none; -webkit-tap-highlight-color: transparent; font-size: .8rem; font-weight: 700; color: var(--primary); }
.full-toggle::-webkit-details-marker { display: none; }
.tg-chev { display: inline-block; vertical-align: -3px; margin-right: 6px; transition: transform .15s; }
.full-program[open] > .full-toggle .tg-chev { transform: rotate(180deg); }
.nb { white-space: nowrap; }
.swap-line { display: flex; flex-wrap: wrap; align-items: baseline; margin-top: 6px; font-size: .9rem; font-weight: 600; }
.swap-line > span { margin-right: 8px; }
.swap-arrow { color: var(--accent); font-weight: 800; }

/* Workouts and their sets */
.sessions { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 19rem), 1fr)); gap: 10px; align-items: start; }
.session { min-width: 0; overflow: hidden; background: var(--surface); border: 1px solid var(--line); border-radius: 14px; }
.session-head { display: flex; flex-wrap: wrap; align-items: center; padding: 8px 12px; background: var(--surface-2); border-bottom: 1px solid var(--line); }
.session-date { margin-right: 8px; font-size: .92rem; font-weight: 800; }
.session-total { margin-left: auto; padding-left: 8px; font-size: .8rem; font-weight: 700; color: var(--text); font-variant-numeric: tabular-nums; }
.session-total .lbl { font-weight: 500; color: var(--muted); }
.set { display: grid; grid-template-columns: 1.75rem minmax(0, 1fr); column-gap: 10px; padding: 9px 12px; border-top: 1px solid var(--line); }
.set:first-child { border-top: 0; }
.set-n { width: 1.75rem; height: 1.75rem; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: var(--primary-soft); color: var(--primary); font-size: .78rem; font-weight: 800; font-variant-numeric: tabular-nums; }
.set-body { min-width: 0; }
.set-line { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: space-between; min-height: 1.75rem; }
.set-line .chip { margin-left: 8px; }
.fig { font-size: 1rem; font-weight: 700; font-variant-numeric: tabular-nums; }
.fig .u { font-size: .85em; font-weight: 600; color: var(--muted); }
.fig .x, .fig .sep { margin: 0 .2em; font-weight: 500; color: var(--muted); }

/* The workout diary */
.journal { margin-top: 28px; }
.journal-list { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: minmax(0, 1fr); gap: 10px; }
.journal-entry { min-width: 0; padding: 10px 12px; background: var(--surface); border: 1px solid var(--line); border-radius: 14px; }
.journal-head { display: flex; flex-wrap: wrap; align-items: center; gap: 4px 10px; }
.stars { color: var(--primary); letter-spacing: 2px; font-size: .95rem; }
.journal-entry .chips { margin-top: 6px; }
.journal-note { margin: 8px 0 0; white-space: pre-line; overflow-wrap: anywhere; }

/* The sheet next to what was done */
.weeks { display: grid; grid-template-columns: minmax(0, 1fr); gap: 12px; }
.week { padding: 12px; background: var(--surface-2); border-radius: 16px; border: 1px solid transparent; }
.week.is-current { border-color: var(--primary); }
.week.is-missed { border: 1px dashed var(--danger); }
.week.is-upcoming { opacity: .8; }
.week-head { display: flex; flex-wrap: wrap; align-items: center; margin: -3px; }
.week-head > * { margin: 3px; }
.week-n { font-size: 1rem; font-weight: 800; }
.week-repeat { font-size: .78rem; color: var(--muted); }
.sheet { margin: 8px 0 0; padding: 10px 12px; background: var(--surface); border: 1px solid var(--line); border-left: 4px solid var(--primary); border-radius: 12px; }
.sheet-label { margin-bottom: 4px; font-size: .68rem; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; color: var(--muted); }
.sheet-plain { font-weight: 700; font-variant-numeric: tabular-nums; }
.done-label { margin: 14px 0 6px; }
.blk-meta { display: inline-block; margin-left: 8px; padding: 0 8px; border-radius: 8px; background: var(--surface-2); color: var(--muted); font-size: .72rem; font-weight: 700; }
.blk-note { display: block; margin-top: 3px; padding-left: 8px; border-left: 3px solid var(--note-line); font-size: .82rem; color: var(--muted); }
.adherence { display: flex; flex-wrap: wrap; padding: 6px 10px; border-bottom: 1px solid var(--line); }
.adherence .chip { margin: 2px; }
.week-rows { display: grid; grid-template-columns: minmax(0, 1fr); gap: 6px; }
.week-row { display: flex; flex-wrap: wrap; align-items: center; padding: 8px 12px; background: var(--surface); border: 1px solid var(--line); border-radius: 12px; }
.week-row.is-missed { border-style: dashed; border-color: var(--danger); }
.week-row.is-current { border-color: var(--primary); }
.week-row > .chip { margin: 2px 0 2px 6px; }
.wr-n { min-width: 4.2rem; font-weight: 800; }
.wr-dates { flex: 1; min-width: 7rem; font-size: .85rem; color: var(--muted); }

/* Footer */
.footer { margin-top: 40px; padding-top: 16px; border-top: 1px solid var(--line); text-align: center; font-size: .8rem; color: var(--muted); }

@media (min-width: 700px) {
  .page { padding: 32px 24px 140px; }
  .title { font-size: 2.4rem; }
  .facts { grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr)); gap: 12px; }
  .fact-wide { grid-column: span 2; }
  .fact { padding: 16px 18px; }
  .fact-value { font-size: 1.3rem; }
  .section-title { font-size: 1.25rem; margin-top: 36px; }
  .exercise { border-radius: 20px; }
  .ex-head { padding: 18px 20px; border-radius: 20px; }
  .ex-body { padding: 0 20px 22px; }
  .set { padding: 10px 14px; }
}

@media (prefers-reduced-motion: reduce) {
  html { scroll-behavior: auto; }
  .ex-chevron, .tg-chev { transition: none; }
}

@media print {
  @page { margin: 12mm; }
  :root { $LIGHT }
  html { scroll-behavior: auto; }
  body { background: #fff; }
  * { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
  .page { max-width: none; padding: 0; }
  .exercise { box-shadow: none; }
  .ex-head { cursor: auto; break-after: avoid; page-break-after: avoid; }
  .ex-chevron, .back-link { display: none; }
  .full-toggle { display: none; }
  .session, .event, .set, .metric, .sheet, .week-row { break-inside: avoid; page-break-inside: avoid; }
}
"""
}
