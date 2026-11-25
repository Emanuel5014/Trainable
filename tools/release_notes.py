#!/usr/bin/env python3
"""Release notes for Trainable's releases: one changelog.json for both apps.

The Android app reads `changelog.json`, attached to the GitHub release as an asset, and shows it in the update sheet
in the user's language. The iOS app carries the file of its own version among its resources (`bundle` below) and
shows it once after an update, since AltStore's notes box is hard to read. The GitHub release text is generated
from the same file, in the usual table style. Items with "platforms" go only to those apps.

Usage:
    python3 tools/release_notes.py check changelog.json                      # validate, exit 1 on problems
    python3 tools/release_notes.py markdown changelog.json [android|ios]     # print the GitHub release text
    python3 tools/release_notes.py bundle changelog.json                     # check, then copy into the apps
                                                                             # (do it before building the IPA)

Publishing (the APK and the notes travel together):
    python3 tools/release_notes.py markdown changelog.json > notes.md
    gh release create v3.0.0-beta3 Trainable.apk changelog.json --prerelease --notes-file notes.md

Format (every text is an object with all of LANGUAGES):
{
  "version": "3.0.0-beta3",                     same as the tag without "v"
  "headline": {"en": "...", "it": "...", ...},  one sentence under the title
  "notice":   {"en": "..."},                    optional, GitHub only (e.g. "install this beta by hand")
  "items": [
    {
      "type": "new" | "improved" | "fixed",
      "area": one of AREAS (picks the icon in the app and the category on GitHub),
      "title":   {...},                         a few words, no full stop
      "summary": {...},                         one line, at most SUMMARY_MAX characters
      "details": {...},                         optional: the full text, shown when the row is opened
      "issues":  [57, 58],                      optional GitHub issue numbers
      "platforms": ["android"]                  optional: only these apps (android, ios); none = both
    }
  ]
}
"""
import json
import os
import sys

LANGUAGES = ["en", "it", "de", "es", "fr", "pt"]
TYPES = ["new", "improved", "fixed"]
PLATFORMS = ["android", "ios"]
# Where the apps read the notes of their own version (sharedUi BUNDLED_CHANGELOG)
BUNDLED = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "sharedUi", "src", "commonMain",
                       "composeResources", "files", "changelog.json")
SUMMARY_MAX = 110

# area -> (emoji, English category) for the GitHub table; the app maps the same names to icons (areaIcon)
AREAS = {
    "updates": ("🔄", "Updates"),
    "dashboard": ("🏠", "Dashboard"),
    "routines": ("📋", "Routines"),
    "history": ("📅", "History"),
    "analytics": ("📊", "Analytics"),
    "workouts": ("🏋️", "Workouts"),
    "exercises": ("🏃", "Exercises"),
    "timers": ("⏱️", "Timers"),
    "onboarding": ("👋", "Onboarding"),
    "notifications": ("🔔", "Notifications"),
    "appearance": ("🎨", "Appearance"),
    "liquid_glass": ("🫧", "Liquid Glass"),
    "backup": ("💾", "Backup"),
    "import_export": ("📤", "Import & Export"),
    "ai": ("🧠", "AI"),
    "widgets": ("🧩", "Widgets"),
    "journal": ("📓", "Journal"),
    "settings": ("⚙️", "Settings"),
    "physical_check": ("📸", "Physical Check"),
    "report": ("📄", "Report"),
    "other": ("✨", "Other"),
}
FIX_CATEGORY = "🛠️ **Fixes & Stability**"


def check(notes):
    problems = []

    def text(where, value, required=True):
        if value is None or value == {}:
            if required:
                problems.append(f"{where}: missing")
            return
        if not isinstance(value, dict):
            problems.append(f"{where}: must be an object of languages")
            return
        missing = [lang for lang in LANGUAGES if not str(value.get(lang, "")).strip()]
        if missing:
            problems.append(f"{where}: missing {', '.join(missing)}")
        extra = [lang for lang in value if lang not in LANGUAGES]
        if extra:
            problems.append(f"{where}: unknown language {', '.join(extra)}")
        if "|" in str(value.get("en", "")):
            problems.append(f"{where}: no '|' in the English text (it breaks the GitHub table)")

    if not str(notes.get("version", "")).strip():
        problems.append("version: missing")
    text("headline", notes.get("headline"))
    items = notes.get("items") or []
    if not items:
        problems.append("items: empty")
    for i, item in enumerate(items):
        where = f"items[{i}]"
        if item.get("type") not in TYPES:
            problems.append(f"{where}.type: {item.get('type')!r} is not one of {TYPES}")
        if item.get("area") not in AREAS:
            problems.append(f"{where}.area: {item.get('area')!r} is not one of {sorted(AREAS)}")
        text(f"{where}.title", item.get("title"))
        text(f"{where}.summary", item.get("summary"))
        text(f"{where}.details", item.get("details"), required=False)
        for lang, value in (item.get("summary") or {}).items():
            if len(value) > SUMMARY_MAX:
                problems.append(f"{where}.summary.{lang}: {len(value)} characters, at most {SUMMARY_MAX}")
        for lang, value in (item.get("title") or {}).items():
            if value.rstrip().endswith("."):
                problems.append(f"{where}.title.{lang}: no full stop at the end")
        issues = item.get("issues", [])
        if not all(isinstance(n, int) and n > 0 for n in issues):
            problems.append(f"{where}.issues: must be issue numbers")
        platforms = item.get("platforms", [])
        if not isinstance(platforms, list) or any(p not in PLATFORMS for p in platforms):
            problems.append(f"{where}.platforms: must be a list of {PLATFORMS}")
    for platform in PLATFORMS:
        if items and not for_platform(items, platform):
            problems.append(f"items: nothing for {platform}")
    return problems


def for_platform(items, platform):
    return [item for item in items if not item.get("platforms") or platform in item["platforms"]]


SMALL_WORDS = {"a", "an", "the", "and", "or", "of", "in", "on", "to", "for", "with", "by", "at", "from", "as"}


def title_case(text):
    """English titles on GitHub keep the old style: "A deleted workout ends its timers" -> "A Deleted Workout Ends Its Timers"."""
    words = text.split(" ")
    out = []
    for i, word in enumerate(words):
        if i > 0 and word.lower() in SMALL_WORDS:
            out.append(word.lower())
        elif word[:1].islower():
            out.append(word[:1].upper() + word[1:])
        else:
            out.append(word)
    return " ".join(out)


def markdown(notes, platform=None):
    version = notes["version"]
    lines = []
    notice = (notes.get("notice") or {}).get("en", "").strip()
    if notice:
        lines += [f"> ⚠️ {notice}", ""]
    lines += [
        f"### 📝 What's New in this Release (v{version})",
        "",
        notes["headline"]["en"].strip(),
        "",
        "| Category | Feature / Fix | Description |",
        "| :--- | :--- | :--- |",
    ]
    items = for_platform(notes["items"], platform) if platform else notes["items"]
    ordered = sorted(items, key=lambda item: TYPES.index(item["type"]))
    for item in ordered:
        if item["type"] == "fixed":
            category = FIX_CATEGORY
        else:
            emoji, label = AREAS[item["area"]]
            category = f"{emoji} **{label}**"
        description = item["summary"]["en"].strip().rstrip(".")
        details = (item.get("details") or {}).get("en", "").strip().rstrip(".")
        if details:
            description = f"{description}. {details}"
        issues = " ".join(f"#{n}" for n in item.get("issues", []))
        if issues:
            description = f"{description} {issues}"
        lines.append(f"| {category} | **{title_case(item['title']['en'].strip())}** | {description} |")
    return "\n".join(lines) + "\n"


def main(argv):
    platform = argv[3] if len(argv) == 4 and argv[1] == "markdown" else None
    if len(argv) not in (3, 4) or argv[1] not in ("check", "markdown", "bundle") or (len(argv) == 4 and platform not in PLATFORMS):
        print(__doc__)
        return 2
    with open(argv[2], encoding="utf-8") as f:
        notes = json.load(f)
    problems = check(notes)
    if problems:
        print("\n".join(problems), file=sys.stderr)
        return 1
    if argv[1] == "check":
        counts = {t: sum(1 for item in notes["items"] if item["type"] == t) for t in TYPES}
        print(f"ok: v{notes['version']}, " + ", ".join(f"{n} {t}" for t, n in counts.items()))
        for p in PLATFORMS:
            print(f"    {p}: {len(for_platform(notes['items'], p))} items")
    elif argv[1] == "bundle":
        with open(BUNDLED, "w", encoding="utf-8") as f:
            json.dump(notes, f, ensure_ascii=False, indent=2)
            f.write("\n")
        print(f"bundled v{notes['version']} -> {os.path.relpath(BUNDLED)}")
    else:
        sys.stdout.write(markdown(notes, platform))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
