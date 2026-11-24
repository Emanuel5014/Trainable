package com.emanuel5014.trainable.data.report

/**
 * Style of the HTML report, drawn like the app: its font, the colours of the user's theme, joined rows with large
 * outer corners, pill chips and the cookie-shaped numbers of Material 3 Expressive. Written for the narrowest phone
 * first; from 1000px a sticky index takes the left side. Nothing scrolls sideways (long names, notes and programs
 * wrap), the theme follows the system of whoever opens the file (or the app, in its own preview) and print is clean.
 */
internal object ReportCss {

    fun style(theme: ReportTheme): String {
        val light = theme.light.cssVariables(ReportPalette.DEFAULT_LIGHT)
        val dark = theme.dark.cssVariables(ReportPalette.DEFAULT_DARK)
        val font = theme.fontWoff2Base64?.takeIf { BASE64.matches(it) }?.let {
            "@font-face{font-family:\"Report Sans\";src:url(data:font/woff2;base64,$it) format(\"woff2\");font-weight:100 900;font-display:swap}\n"
        }.orEmpty()
        return font + """
:root { color-scheme: light dark; $light }
@media (prefers-color-scheme: dark) { :root { $dark } }
:root[data-scheme="light"] { color-scheme: light; $light }
:root[data-scheme="dark"] { color-scheme: dark; $dark }
""" + BODY + """
@media print {
  :root, :root[data-scheme="dark"] { color-scheme: light; $light }
$PRINT
}
"""
    }

    private val BASE64 = Regex("[A-Za-z0-9+/=]+")

    private const val BODY = """
*, *::before, *::after { box-sizing: border-box; }
html { -webkit-text-size-adjust: 100%; text-size-adjust: 100%; scroll-behavior: smooth; scroll-padding-top: 80px; }
body {
  margin: 0; background: var(--surface); color: var(--on);
  font: 500 15px/1.45 "Report Sans", system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  -webkit-font-smoothing: antialiased; font-variant-numeric: tabular-nums;
  overflow-wrap: break-word; overflow-wrap: anywhere;
}
h1, h2, h3, h4, p, ol, ul, figure { margin: 0; }
ol, ul { padding: 0; list-style: none; }
a { color: inherit; text-decoration: none; }
svg { display: block; flex: none; }
summary { list-style: none; cursor: pointer; -webkit-tap-highlight-color: transparent; }
summary::-webkit-details-marker { display: none; }
:focus-visible { outline: 3px solid var(--primary); outline-offset: 2px; border-radius: 12px; }
.sr { position: absolute; width: 1px; height: 1px; margin: -1px; padding: 0; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
.txt { white-space: pre-wrap; }
.nb { white-space: nowrap; }

/* Layout: one column on a phone, a sticky index on the side from a laptop up */
.shell { max-width: 1240px; margin: 0 auto; }
.side { display: none; }
.main {
  min-width: 0; padding: 20px 16px 140px;
  padding-left: max(16px, env(safe-area-inset-left)); padding-right: max(16px, env(safe-area-inset-right));
}
.plan + .plan { margin-top: 56px; }

/* Header, like the app's screen headers */
.eyebrow { font-size: .78rem; font-weight: 800; letter-spacing: .09em; text-transform: uppercase; color: var(--primary); }
.title { margin-top: 2px; font-size: 2.5rem; line-height: 1.04; font-weight: 900; letter-spacing: -.025em; }
.subtitle { margin-top: 10px; color: var(--on-var); font-weight: 600; }

/* Sections of a routine: the app's segmented pill, sticky while the page scrolls */
.tabs { position: sticky; top: 0; z-index: 5; margin: 16px -16px 0; padding: 10px 16px; background: var(--surface); }
.tabs ol { display: flex; padding: 4px; border-radius: 999px; background: var(--c); overflow-x: auto; scrollbar-width: none; }
.tabs ol::-webkit-scrollbar { display: none; }
.tabs li { flex: 1 0 auto; }
.tabs a { display: block; padding: 10px 12px; border-radius: 999px; text-align: center; font-size: .76rem; font-weight: 800; letter-spacing: .04em; text-transform: uppercase; color: var(--on-var); white-space: nowrap; }
.tabs a.on { background: var(--surface); color: var(--primary); }

.sec { padding-top: 26px; }
.sec-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.sec-title { font-size: 1.75rem; line-height: 1.1; font-weight: 800; letter-spacing: -.015em; }
.pill { flex: none; padding: 6px 14px; border-radius: 999px; background: var(--s-cont); color: var(--on-s-cont); font-size: .8rem; font-weight: 700; white-space: nowrap; }
.empty { padding: 22px; border-radius: 28px; background: var(--c); color: var(--on-var); text-align: center; font-weight: 600; }

/* Overview */
.stats { display: grid; grid-template-columns: 1fr 1fr; gap: 3px; border-radius: 28px; overflow: hidden; }
.stat { min-width: 0; padding: 16px 18px; background: var(--c); }
.stat:last-child:nth-child(odd) { grid-column: 1 / -1; }
.stat-v { display: block; font-size: 2rem; line-height: 1.1; font-weight: 800; letter-spacing: -.02em; }
.stat-l { display: block; margin-top: 2px; font-size: .85rem; color: var(--on-var); font-weight: 600; }
.card { margin-top: 12px; padding: 18px; border-radius: 28px; background: var(--c); }
.card-k { font-size: .75rem; font-weight: 800; letter-spacing: .09em; text-transform: uppercase; color: var(--primary); }
.card-t { margin-top: 2px; font-size: 1.25rem; font-weight: 800; }
.wk-strip { display: grid; grid-template-columns: repeat(auto-fill, minmax(64px, 1fr)); gap: 6px; margin-top: 14px; }
.wk { display: flex; flex-direction: column; align-items: center; padding: 10px 4px 9px; border-radius: 18px; background: var(--c-high); }
.wk-n { font-size: .78rem; font-weight: 800; color: var(--on-var); }
.wk-c { display: flex; align-items: center; justify-content: center; min-width: 28px; height: 28px; margin: 6px 0 4px; padding: 0 6px; border-radius: 9px; font-weight: 800; background: var(--c-highest); color: var(--on-var); }
.wk-l { font-size: .7rem; line-height: 1.2; font-weight: 700; color: var(--on-var); text-align: center; }
.wk.is-done .wk-c { background: var(--p-cont); color: var(--on-p-cont); }
.wk.is-current { background: var(--primary); }
.wk.is-current .wk-n, .wk.is-current .wk-l { color: var(--on-primary); }
.wk.is-current .wk-c { background: var(--on-primary); color: var(--primary); }
.wk.is-missed .wk-c { background: var(--e-cont); color: var(--on-e-cont); }
.wk.is-upcoming { opacity: .7; }

/* Notes: the app's tinted container wherever someone wrote something */
.note { display: flex; gap: 12px; margin-top: 12px; padding: 14px 16px; border-radius: 24px; background: var(--s-cont); color: var(--on-s-cont); }
.note svg { margin-top: 2px; }
.note-body { flex: 1; min-width: 0; }
.note-label { font-size: .72rem; font-weight: 800; letter-spacing: .09em; text-transform: uppercase; margin-bottom: 2px; }
.set-note { display: flex; gap: 8px; margin-top: 6px; padding: 7px 10px; border-radius: 12px; background: var(--s-cont); color: var(--on-s-cont); font-size: .88rem; }
.set-note svg { margin-top: 2px; }
.set-note .txt { flex: 1; min-width: 0; }

/* Joined rows, like the Routines and History lists */
.rows { display: flex; flex-direction: column; gap: 3px; border-radius: 28px; overflow: hidden; }
.rows > li { background: var(--c); }
.row { display: flex; align-items: center; gap: 14px; min-height: 72px; padding: 14px 14px 14px 16px; }
.chev { color: var(--on-var); transition: transform .2s; }
details[open] > summary .chev { transform: rotate(180deg); }
.badge { position: relative; flex: none; width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; }
.badge svg { position: absolute; top: 0; left: 0; width: 100%; height: 100%; fill: var(--s-cont); }
.badge span { position: relative; font-weight: 800; color: var(--on-s-cont); }
details[open] > summary .badge svg { fill: var(--primary); }
details[open] > summary .badge span { color: var(--on-primary); }
.ex-main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.ex-name { font-size: 1.08rem; line-height: 1.25; font-weight: 700; }
.ex-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 4px 6px; margin-top: 2px; font-size: .85rem; color: var(--on-var); font-weight: 600; }
.ex-fig { display: flex; flex-direction: column; align-items: flex-end; text-align: right; }
.ex-best { font-size: 1.12rem; font-weight: 800; white-space: nowrap; }
.ex-best small { font-size: .75em; color: var(--on-var); font-weight: 700; }
.delta { margin-top: 3px; padding: 1px 8px; border-radius: 999px; background: var(--t-cont); color: var(--on-t-cont); font-size: .75rem; font-weight: 800; white-space: nowrap; }
.spark { display: none; width: 72px; height: 30px; fill: none; stroke: var(--primary); stroke-width: 2.2; stroke-linejoin: round; stroke-linecap: round; }
.spark .dot { stroke-width: 7; }
.ex.is-removed .ex-name { color: var(--on-var); }
.ex-body { padding: 4px 16px 18px; }
.sub-title { margin: 22px 0 10px; font-size: .75rem; font-weight: 800; letter-spacing: .09em; text-transform: uppercase; color: var(--on-var); }

/* Figures of an exercise */
.metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 8.5rem), 1fr)); gap: 6px; }
.metric { min-width: 0; padding: 12px 14px; border-radius: 18px; background: var(--c-high); }
.metric-label { display: block; font-size: .78rem; color: var(--on-var); font-weight: 600; }
.metric-value { display: block; margin-top: 2px; font-size: 1.2rem; font-weight: 800; }
.metric-hint { display: block; font-size: .75rem; color: var(--on-var); font-weight: 600; }

/* The best set of each workout, as a line */
.chart { margin-top: 6px; padding: 14px 14px 10px; border-radius: 18px; background: var(--c-low); }
.chart-plot { position: relative; height: 120px; margin-left: 64px; }
.chart-plot svg { width: 100%; height: 100%; overflow: visible; }
.chart .grid line { stroke: var(--outline); stroke-width: 1; stroke-dasharray: 2 4; }
.chart .area { fill: var(--primary); opacity: .14; }
.chart .line { fill: none; stroke: var(--primary); stroke-width: 2.5; stroke-linejoin: round; }
.chart .dots path { stroke: var(--primary); stroke-width: 7; stroke-linecap: round; }
.chart .dots .best { stroke-width: 11; }
.ax { position: absolute; left: -64px; width: 58px; text-align: right; font-size: .72rem; font-weight: 700; color: var(--on-var); white-space: nowrap; }
.ax-top { top: -7px; }
.ax-bottom { bottom: -7px; }
.chart figcaption { display: flex; justify-content: space-between; gap: 8px; margin: 8px 0 0 64px; font-size: .72rem; font-weight: 700; color: var(--on-var); }

/* Chips */
.chip, .tag { display: inline-block; padding: 3px 10px; border-radius: 999px; font-size: .72rem; line-height: 1.4; font-weight: 800; background: var(--c-highest); color: var(--on-var); }
.chip-plan, .chip-week, .chip-kind { background: var(--p-cont); color: var(--on-p-cont); }
.chip-done, .chip-scope-plan { background: var(--s-cont); color: var(--on-s-cont); }
.chip-ok { background: var(--t-cont); color: var(--on-t-cont); }
.chip-miss, .tag-removed { background: var(--e-cont); color: var(--on-e-cont); }
.chip-over { background: var(--p-cont); color: var(--on-p-cont); }
.chip-current { background: var(--primary); color: var(--on-primary); }
.chips { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 6px; }

/* Program changes and swaps */
.events { display: flex; flex-direction: column; gap: 6px; }
.event { display: flex; gap: 12px; padding: 12px 14px; border-radius: 18px; background: var(--c-high); }
.event > svg { margin-top: 2px; color: var(--primary); }
.event-body { flex: 1; min-width: 0; }
.event-head { display: flex; flex-wrap: wrap; align-items: center; gap: 4px 6px; }
.event-date { margin-right: 2px; font-size: .85rem; font-weight: 800; }
.changes { margin-top: 8px; }
.changes li { margin-top: 2px; font-weight: 700; }
.compare { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 13rem), 1fr)); gap: 6px; margin-top: 8px; }
.compare-col { min-width: 0; padding: 8px 12px; border-radius: 14px; background: var(--c-low); }
.compare-label { margin-bottom: 2px; font-size: .68rem; font-weight: 800; letter-spacing: .09em; text-transform: uppercase; color: var(--on-var); }
.full-toggle { display: inline-flex; align-items: center; gap: 4px; margin-top: 6px; padding: 4px 0; font-size: .8rem; font-weight: 800; color: var(--primary); }
.full-program[open] > .full-toggle .chev { transform: rotate(180deg); }
.full-toggle .chev { color: var(--primary); }
.swap-line { display: flex; flex-wrap: wrap; align-items: baseline; gap: 0 8px; margin-top: 4px; font-weight: 700; }
.swap-arrow { color: var(--primary); font-weight: 800; }

/* The sheet next to what was done */
.weeks { display: flex; flex-direction: column; gap: 10px; }
.week { padding: 12px; border-radius: 22px; background: var(--c-low); }
.week.is-current { box-shadow: inset 0 0 0 2px var(--primary); }
.week.is-missed { box-shadow: inset 0 0 0 1px var(--on-e-cont); }
.week.is-upcoming, .week.is-excluded { opacity: .75; }
.week-head { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin-bottom: 10px; }
.week-n { margin-right: auto; font-size: 1rem; font-weight: 800; }
.week-repeat { width: 100%; font-size: .78rem; font-weight: 600; color: var(--on-var); }
.sheet { display: flex; align-items: baseline; gap: 12px; padding: 10px 14px; border-radius: 14px; background: var(--p-cont); color: var(--on-p-cont); }
.sheet-label { flex: none; font-size: .68rem; font-weight: 800; letter-spacing: .09em; text-transform: uppercase; }
.sheet-plain { font-weight: 700; }
.done-label { display: block; margin: 12px 0 0; color: var(--on-var); }
.blocks { flex: 1; min-width: 0; }
.blocks li { display: flex; align-items: baseline; gap: 8px; font-weight: 700; }
.blocks li + li { margin-top: 3px; }
.blk-n { flex: none; width: 1rem; font-size: .78rem; opacity: .7; }
.blk-t { flex: 1; min-width: 0; }
.blocks .none { opacity: .7; }
.blk-meta { display: inline-block; margin-left: 8px; font-size: .78rem; font-weight: 700; opacity: .8; }
.blk-note { display: block; margin-top: 2px; font-size: .82rem; font-weight: 600; opacity: .85; }

/* Workouts and their sets */
.sessions { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 17rem), 1fr)); gap: 8px; margin-top: 8px; align-items: start; }
.session { min-width: 0; border-radius: 16px; background: var(--c-high); overflow: hidden; }
.session-head { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; padding: 10px 12px 4px; }
.session-date { margin-right: 4px; font-weight: 800; }
.session-total { margin-left: auto; font-size: .8rem; font-weight: 800; white-space: nowrap; }
.session-total .lbl { font-weight: 600; color: var(--on-var); }
.adherence { display: flex; flex-wrap: wrap; gap: 4px; padding: 2px 12px 4px; }
.sets { padding: 0 8px 8px; }
.set { display: flex; align-items: flex-start; gap: 10px; padding: 6px; }
.set + .set { border-top: 1px solid var(--outline); }
.set-n { flex: none; width: 24px; height: 24px; margin-top: 1px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: .75rem; font-weight: 800; background: var(--c-highest); color: var(--on-var); }
.set-body { flex: 1; min-width: 0; }
.set-line { display: flex; flex-wrap: wrap; align-items: center; gap: 4px 8px; min-height: 26px; }
.set-line .chips { margin-top: 0; }
.fig { font-weight: 800; }
.fig .u { font-size: .8rem; font-weight: 700; color: var(--on-var); }
.fig .x, .fig .sep { margin: 0 .2em; font-weight: 600; color: var(--on-var); }
.rpe { margin-left: auto; font-size: .75rem; font-weight: 800; color: var(--on-var); white-space: nowrap; }
.older { margin-top: 8px; }
.older > summary { display: flex; align-items: center; gap: 8px; padding: 10px 12px; border-radius: 14px; background: var(--c-high); font-weight: 700; font-size: .9rem; }

/* The workouts of the routine, month by month, with their diary */
.month + .month { margin-top: 22px; }
.month-head { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: space-between; gap: 4px 10px; padding: 0 6px 10px; }
.month-head h3 { font-size: 1.05rem; font-weight: 800; }
.month-head span { font-size: .85rem; font-weight: 700; color: var(--on-var); }
.dt { flex: none; width: 52px; height: 56px; border-radius: 16px; background: var(--c-highest); display: flex; flex-direction: column; align-items: center; justify-content: center; line-height: 1.05; }
.dt b { font-size: 1.45rem; font-weight: 800; }
.dt span { font-size: .72rem; font-weight: 700; color: var(--on-var); }
.wo-j { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin-top: 5px; }
.stars { color: var(--primary); letter-spacing: 1px; font-size: .9rem; }
.stars .off { color: var(--outline); }
.wo-body { padding: 0 16px 16px; }
.wo-body > .note { margin: 0 0 10px; border-radius: 16px; padding: 10px 14px; }
.wo-list li { display: flex; flex-direction: column; gap: 6px; padding: 10px 4px; }
.wo-list li + li { border-top: 1px solid var(--outline); }
.wo-ex { font-weight: 700; }
.wo-sets { display: flex; flex-wrap: wrap; gap: 4px; }
.sc { padding: 3px 9px; border-radius: 9px; background: var(--c-high); font-size: .85rem; font-weight: 800; }
.sc .u { margin-left: 2px; font-size: .75rem; font-weight: 700; color: var(--on-var); }
.sc .x { margin: 0 3px; font-weight: 600; color: var(--on-var); }
.sc .r { margin-left: 4px; font-size: .75rem; color: var(--on-var); }

/* Index of several routines */
.plans { margin-bottom: 40px; }
.plans .row { min-height: 64px; }

.footer { margin-top: 40px; text-align: center; font-size: .8rem; color: var(--on-var); font-weight: 600; }
.to-top { display: inline-flex; align-items: center; gap: 6px; margin-bottom: 14px; color: var(--primary); font-weight: 800; font-size: .85rem; }

/* Wider phones and tablets */
@media (min-width: 560px) {
  .spark { display: block; }
  .stats { grid-template-columns: none; grid-auto-flow: column; grid-auto-columns: 1fr; }
  .stat:last-child:nth-child(odd) { grid-column: auto; }
  .title { font-size: 3.2rem; }
}

/* Laptop and desktop: the index on the side, the tabs are not needed */
@media (min-width: 1000px) {
  html { scroll-padding-top: 24px; }
  .shell { display: grid; grid-template-columns: 280px minmax(0, 1fr); gap: 32px; padding: 0 32px; }
  .side { display: block; }
  .side-in { position: sticky; top: 0; max-height: 100vh; overflow-y: auto; padding: 32px 0 24px; }
  .brand { padding: 0 14px 16px; font-size: .78rem; font-weight: 800; letter-spacing: .09em; text-transform: uppercase; color: var(--primary); }
  .sb-plan { margin: 18px 0 4px; padding: 0 14px; font-size: .72rem; font-weight: 800; letter-spacing: .09em; text-transform: uppercase; color: var(--on-var); }
  .sb-sec { display: block; padding: 10px 14px; border-radius: 999px; font-weight: 800; }
  .sb-sec:hover, .sb-ex a:hover { background: var(--c); }
  .sb-sec.on { background: var(--p-cont); color: var(--on-p-cont); }
  .sb-ex { margin: 2px 0 8px; }
  .sb-ex a { display: flex; align-items: center; gap: 10px; padding: 7px 14px; border-radius: 999px; font-size: .9rem; font-weight: 600; color: var(--on-var); }
  .sb-ex a.on { background: var(--c); color: var(--on); }
  .sb-n { flex: none; width: 22px; height: 22px; border-radius: 7px; display: flex; align-items: center; justify-content: center; font-size: .7rem; font-weight: 800; background: var(--c-high); }
  .side-foot { margin-top: 20px; padding: 0 14px; font-size: .75rem; color: var(--on-var); }
  .main { padding: 40px 0 80px; }
  .tabs { display: none; }
  .title { font-size: 4rem; }
  .row { padding: 16px 20px; }
  .ex-body { padding: 4px 24px 24px; }
  .wo-body { padding: 0 24px 20px; }
}

@media (prefers-reduced-motion: reduce) {
  html { scroll-behavior: auto; }
  .chev { transition: none; }
}
"""

    private const val PRINT = """
  @page { margin: 12mm; }
  html { scroll-behavior: auto; }
  body { background: #fff; }
  * { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
  .side, .tabs, .chev, .to-top { display: none; }
  .shell { display: block; }
  .main { padding: 0; }
  .rows { overflow: visible; }
  .row, .session, .event, .metric, .sheet, .week-head, .wo-list li { break-inside: avoid; page-break-inside: avoid; }
"""
}
