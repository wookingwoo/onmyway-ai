# Motion Short revision design

## User direction and approach

The owner requested a faster, funnier, more immersive Short with animation,
transitions and contemporary meme language, continuing the existing PR. Keep
the silent-video/later-owner-TTS contract. Work in the existing isolated
`feat/youtube-shorts` worktree. Creative decisions are delegated; proceed through
production, verification and the existing PR without another approval gate.

Options considered: accelerate the same slides (still visually static); borrow
viral footage/audio (weak connection to this product); or build an original
workday/quest comedy around the genuine footage. Select the third. It adds
relatable humor without external media or unsupported product claims.

## Research checked 2026-10-25

- [YouTube, 2026 APAC creator interviews](https://blog.youtube/creator-and-artist-stories/cracking-the-code-to-youtube-shorts/):
  relatable recurring characters, surprise and recognizable community jokes.
- [YouTube, creative maximalism research](https://blog.youtube/culture-and-trends/next-gen-creativity/):
  faster editing and layered visual/internet references are an observed style.
  This does not prove a specific Korean meme is currently popular or guarantee
  retention. Use restrained visual layering suited to readable app evidence.
- [YouTube, adapting clips to Shorts](https://blog.youtube/creator-and-artist-stories/transitioning-your-long-form-content-to-youtube-shorts/):
  a concise standalone story, cropped key moments and legible captions.
- [Contest rules](https://appdevchallenge.auto-hmg.io/guide/shortform): preserve
  30–60 second portrait format, optional link and independent award boundary.

## Treatment

42 seconds, 18 scenes, 1080×1920 at 30 fps, no audio. Original Korean situational
humor: “퇴근했는데 퀘스트가 또 뜸,” mandatory tasks, milk asking to join, a deadline
plot twist, and milk accepting a rain check. This is our creative interpretation
of contemporary shortform grammar, not a claim to reproduce a current top meme.

Opening text is present at frame zero. Typical visual beats are 0.2–0.7 seconds:
staggered task cards, spring stamps, number emphasis, a drawn milk character,
route tracing and deliberate whip/wipe/zoom cuts. Avoid repetitive flashes.
Readable genuine app excerpts get 2–3 second scenes with large editorial summaries.
Persistent disclosure and narration captions do not move with scene transitions.
Titles and art move inside the safe area; leave right/bottom Shorts controls room.

Story timing: 0–8 s problem/characters; 8–16 s clarification/calculation/normal
result; 16–24 s alternate deadline, approval and milk callback; 24–32.5 s return
to normal example, separate completion, emulator driving lock and home approval;
32.5–42 s payoff/product/closing question that echoes the opening quest.

## Source and acceptance contract

Use the same hash-checked genuine raw recordings. No live-provider, physical-car,
measured-time-saving or award claim. Editorial overlays are visibly graphics,
not fabricated app controls. Preserve explicit A→B→A example changes, mandatory
errands, approval, arrival/completion separation and final home approval.

One timeline drives exact frame counts and plain UTF-8 SRT. Every duration must
align to a 30 fps frame; scene entries/transitions must fit their scenes, and
caption reading rate stays at or below 11 characters/second. Animation must
change image content within scenes, not just add fades between static slides.
No transition may introduce black opening frames or move/obscure disclosure.

Archive v1 locally; replace the current named deliverables with v2. Verify full
decode/encoding, exact 1,260 frames/42 seconds, no audio, SRT boundaries and hashes,
sample animated beats/scene cuts at phone size, continuous browser playback,
source-boundary tests and Python/Android regression checks. Keep all rendered
media local and update the existing PR with small source/caption/evidence files.
