"""Mixes audio/ onto video.mp4 and writes ryyppy-ad.mp4. Times are video seconds;
each cue's start is offset so the clip's transient lands on the listed beat."""
import os, subprocess

FF = os.environ.get("FF", "ffmpeg")
MUSIC_GAIN_DB = -4
TARGET_LUFS = -14

# (file, hit time in video, where the hit sits inside the clip, max length, gain dB, fade-out s)
CUES = [
    ("bottle_popcling", 1.00, 0.47, 1.2, 0, 0.3),   # logo lands
    ("woosh2", 3.94, 0.22, 1.0, -3, 0.2),            # wipe into party scene
    ("woosh2", 9.20, 0.22, 1.0, -6, 0.2),            # tile pops out of the phone
    ("clink", 11.00, 0.29, 1.4, 0, 0.3),             # the tap
    ("pour", 11.30, 0.77, 3.0, -4, 0.6),             # progress bar fills
    ("woosh2", 14.10, 0.22, 1.0, -6, 0.2),           # edit overlay slides in
    ("woosh2", 17.10, 0.22, 1.0, -6, 0.2),           # counter scene slides in
    *[("tick", 18.0 + 0.25 * k, 0.0, 0.2, -2, 0.0) for k in range(1, 8)],  # counter steps
    ("ding", 20.00, 0.08, 1.0, -2, 0.2),             # counter lands on 0.80
    ("woosh2", 21.94, 0.22, 1.0, -3, 0.2),           # wipe into crew chart
    ("woosh2", 25.94, 0.22, 1.0, -3, 0.2),           # wipe into end card
    ("popper", 27.00, 0.07, 0.6, 0, 0.1),            # logo on the song's final hit
    ("crowd", 27.00, 0.30, 3.0, -6, 1.0),            # cheer under the end card
]

inputs = ["-i", "video.mp4", "-i", "audio/music_short_theme.mp3"]
filters = [f"[1]atrim=0:30,afade=t=in:d=0.05,afade=t=out:st=29:d=1,volume={MUSIC_GAIN_DB}dB[m]"]
labels = ["[m]"]
for n, (name, hit, offset, length, gain, fade) in enumerate(CUES):
    inputs += ["-i", f"audio/{name}.mp3"]
    start = hit - offset
    trim_from = max(0.0, -start)
    delay_ms = int(round(max(0.0, start) * 1000))
    chain = f"[{n + 2}]aresample=44100,aformat=channel_layouts=stereo,atrim={trim_from}:{trim_from + length},asetpts=PTS-STARTPTS"
    if fade:
        chain += f",afade=t=out:st={max(0, length - fade)}:d={fade}"
    chain += f",volume={gain}dB,adelay={delay_ms}|{delay_ms}[c{n}]"
    filters.append(chain)
    labels.append(f"[c{n}]")
mix = f"{''.join(labels)}amix=inputs={len(labels)}:duration=first:normalize=0"

# Measure the raw mix, then apply one static gain so the song's ending keeps its dynamics.
measured = subprocess.run([FF, "-hide_banner", *inputs, "-filter_complex", ";".join(filters + [mix + ",ebur128[out]"]),
                           "-map", "[out]", "-t", "30", "-f", "null", "-"], capture_output=True, text=True).stderr
integrated = float(measured.rsplit("I:", 1)[1].split("LUFS")[0])
gain = TARGET_LUFS - integrated
print(f"mix {integrated:.1f} LUFS, gain {gain:+.1f} dB")

subprocess.run([FF, "-y", "-loglevel", "error", *inputs, "-filter_complex",
                ";".join(filters + [mix + f",volume={gain:.2f}dB,alimiter=limit=0.84:level=false[out]"]),
                "-map", "0:v", "-map", "[out]", "-c:v", "copy", "-c:a", "aac", "-b:a", "192k",
                "-t", "30", "ryyppy-ad.mp4"], check=True)
