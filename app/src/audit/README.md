# Glass UI audit

`assembleAudit` builds an isolated `com.emanuel5014.trainable.audit` app. Only this variant includes
`GlassAuditActivity`, which seeds fictional routines, advanced weekly prescriptions, a superset,
cardio, eight finished workouts, body weights, physical checks, and a one-rep maximum. The production
navigation graph and view models render the fixtures. The normal fast/debug app and its data are separate.

Install `app/build/outputs/apk/audit/app-audit.apk`, then run `tools/glass-audit.ps1`. The script opens
every registered destination, checks the foreground package, saves accessibility trees and screenshots,
and produces contact sheets in `docs/liquid-glass/audit`. `-Light` repeats in a light theme; `-Screens`
limits a pass. Nested sheets and dialogs require additional touch checks.

`-Oled` captures the pure-black theme. The audit activity also accepts `--ez oled true` directly.

`tools/glass-touch-audit.ps1` checks routine edit forms, weekday selection, blank-name validation,
week-deletion cancellation, advanced block editing and the exercise picker. It keeps the keyboard
and action-footer layout visible in screenshots. `tools/glass-onboarding-audit.ps1` visits the eleven
onboarding pages without submitting the profile. `tools/glass-scroll-audit.ps1` records bounded
settings/routine scroll samples with foreground-screen assertions; stop Gradle and other emulators
before measuring. These emulator samples are smoke checks, not physical-device guarantees.

The audit activity also accepts `--ez solid true` and `--ef intensity 0.25` for material fallback checks.
Audit fixtures and exported activity are excluded from fast/release APKs.

`tools/glass-appearance-audit.ps1` checks blank/enabled Create, weekday selection, the History FAB
menu and solid-background persistence across force-stops. Use `-Light` for the light-theme pass.
`tools/glass-wallpaper-motion-audit.ps1` imports a local test image through the Android photo picker,
checks photo persistence, simulates acceleration/gyroscope input, and verifies the reflection toggle
and solid-material fallback. `-SkipPhoto` repeats only the motion checks. Both scripts default to
`emulator-5554`, accept `-Serial` and `-OutputDirectory`, and use English labels. Captures are stored
in Git-ignored `docs/liquid-glass/rework-*` directories. Only the audit activity supports the
`--ez motionProbe true` diagnostic log; production builds contain no probe or fixture activity.

Audit theme settings follow the saved glass/background/motion preferences. Explicit `--ez solid true`
or `--ef intensity` extras still override the glass material for fallback checks.

`tools/glass-workout-polish-audit.ps1` reproduces the compact quick-workout name dialog, exercise
search, a three-set Arnold Press workout named `ciao`, set editing/logging, and settings switch
tap/drag/persistence. Its fixture is selected with `--es screen workout-compact`. Use a fresh audit
database between passes (`adb shell pm clear com.emanuel5014.trainable.audit`), an English locale,
1080x2340 at 480 dpi, and `-Light` for light mode. Restore emulator size/density after testing.
The script enables inline workout modifications only in the audit preferences to exercise the picker.
Captured mid-drag frames and accessibility trees are written under `docs/liquid-glass/polish-*`.
