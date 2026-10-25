"""Shared 1x source-window clock for the spoken-plan film."""


def merge_windows(windows, duration):
    result = []
    for start, end in sorted(windows):
        if not 0 <= start < end <= duration:
            raise ValueError("Cut exceeds genuine capture")
        if result and start <= result[-1][1]:
            result[-1] = (result[-1][0], max(result[-1][1], end))
        else:
            result.append((start, end))
    return result


def remap_interval(start, end, windows):
    cursor = 0
    for a, b in windows:
        if a <= start < end <= b:
            return cursor + start - a, cursor + end - a
        cursor += b - a
    raise ValueError("Spoken interval crosses a cut")


def story_windows(chapter, duration):
    beats = {b["name"]: b["at"] for b in chapter["beats"]}
    windows = [(p["at"] - .3, p["at"] + p["seconds"] + .5) for p in chapter.get("prepared", [])]
    name = chapter["id"]
    if name == "request":
        windows += [(beats["voice-home"] - .3, beats["voice-home"] + 1),
                    (chapter["prepared"][0]["at"] - .3, beats["native-speech-done"] + .5)]
    elif name == "refinement":
        windows += [(chapter["prepared"][0]["at"] - .3, beats["native-speech-done"] + .5)]
    else:
        windows += [(beats["accepted"] - 2, min(duration, beats["accepted"] + 1))]
        if name == "departure":
            windows += [(beats["driving-lock"] - .5, min(duration, beats["driving-lock"] + 1.2))]
        else:
            arrival, completion = ("laundry-arrival", "laundry-completed") if name == "remaining" else ("medicine-arrival", "medicine-completed")
            windows += [(beats[arrival] - .4, beats[arrival] + 1.2),
                        (beats[completion] + 1, beats["native-speech-done"] + .5)]
            if name == "home":
                windows += [(beats["home-arrival"] - .5, duration)]
    return merge_windows(windows, duration)
