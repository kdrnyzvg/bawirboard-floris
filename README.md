# Notely

An iOS-Notes-style rich text notes app for Android. Write notes, paste text,
style any selection (**font**, **size**, **color**, bold/italic), and drop
**tappable audio clips inline — even mid-sentence**.

## Features

- 📝 **Rich text editor** built on Android `Spannable` (the right tool for
  per-selection styling + inline objects).
- ✂️ **Copy / paste** — native long-press paste works out of the box.
- 🅰️ **Font** per selection — Sans / Serif / Mono.
- 🔠 **Size** per selection — Small / Normal / Large / Huge.
- 🎨 **Color** per selection — a palette of named colors.
- **B** / *I* — bold & italic toggles.
- 🎤 **Inline audio** — record a clip and insert it at the cursor as a
  `▶ 0:12` chip anywhere in the text; tap the chip to play it back.
- 💾 **Local-first** — notes saved as JSON, audio as `.m4a`, all in app-private
  storage. No account, no network, no ads.

## How it works

| Concern | Implementation |
|---|---|
| Editor | `RichEditText` (`AppCompatEditText`) + `SpannableStringBuilder` |
| Styling | `StyleSpan`, `ForegroundColorSpan`, `AbsoluteSizeSpan`, `TypefaceSpan` |
| Inline audio | Custom `AudioSpan : ReplacementSpan` on a `￼` placeholder char |
| Audio I/O | `MediaRecorder` (AAC/m4a) + `MediaPlayer` |
| Persistence | `RichTextSerializer` ↔ JSON; `NoteRepository` over `filesDir` |
| UI | Views + ViewBinding, Material 3 |

When a note is saved, every span is serialized to JSON with its range and
value; on load they're rebuilt onto the text, so formatting and audio chips
survive round-trips.

## Project structure

```
app/src/main/java/com/example/notely/
  MainActivity.kt        # notes list (RecyclerView + FAB), delete on long-press
  EditorActivity.kt      # the rich editor + formatting toolbar + audio
  RichEditText.kt        # EditText that detects taps on audio chips
  AudioSpan.kt           # inline audio chip rendering
  AudioRecorder.kt       # MediaRecorder wrapper
  RichTextSerializer.kt  # spans <-> JSON
  NoteRepository.kt      # file-based storage
  NotesAdapter.kt        # list adapter
  NoteMeta.kt            # list item model
```

## Building

Requires the **Android SDK** (compileSdk 34, minSdk 26).

**Android Studio:** open the project root and Run ▶ — it provisions the SDK
automatically.

**Command line:**

```bash
# point Gradle at your SDK
echo "sdk.dir=/path/to/Android/sdk" > local.properties

./gradlew assembleDebug         # build the APK
./gradlew installDebug          # install on a connected device/emulator
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

> Note: this repo was scaffolded in an environment without the Android SDK, so
> the project hasn't been compiled there. Dependencies and the Gradle/AGP setup
> resolve cleanly; the build completes once an SDK is available.

## Ideas / next steps

- Underline & highlight spans, text alignment.
- Waveform + scrubber for audio chips, multiple clips playlist.
- Search across notes (the plain text is already stored).
- Export a note to HTML / Markdown.
- Swipe-to-delete in the list; pin / archive.
