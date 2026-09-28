# 30-second ad video

`ryyppy-ad.mp4` is 1080x1920 and 30 fps. It has a rock backing track and sound effects synced to the on-screen hits.

## Audio

All audio is CC0 (public domain), so it is free to use commercially without attribution. The credits below are a courtesy.

| File | Source | Author |
|---|---|---|
| `music_short_theme.mp3` | [Short Theme [Rock/Metal]](https://opengameart.org/content/short-theme-rockmetal) (OpenGameArt), cut from 19.475 s | nene |
| `bottle_popcling.mp3` | [Beer bottle - Pop and cling](https://freesound.org/people/ldezem/sounds/386169/) | ldezem |
| `clink.mp3` | [Glass Clink.wav](https://freesound.org/people/Dentrabert/sounds/673315/) | Dentrabert |
| `pour.mp3` | [Pouring a Beer from the Tap](https://freesound.org/people/zembacraftworks/sounds/428334/) | zembacraftworks |
| `woosh2.mp3` | [quick woosh](https://freesound.org/people/florianreichelt/sounds/683101/) | florianreichelt |
| `tick.mp3` | [Button Tick](https://freesound.org/people/NenadSimic/sounds/268108/) | NenadSimic |
| `ding.mp3` | [Correct.mp3](https://freesound.org/people/LittleRainySeasons/sounds/335908/) | LittleRainySeasons |
| `popper.mp3` | [partypopper.flac](https://freesound.org/people/Streety/sounds/26349/) | Streety |
| `crowd.mp3` | [cheering and clapping crowd 2](https://freesound.org/people/AlaskaRobotics/sounds/221567/) | AlaskaRobotics |

The track runs at 120 BPM. The cut starts on a downbeat, so bars start on even seconds of the video. The song's final hit lands at 27 s, where the end-card logo lands.

## Rebuild

1. Start the app on :8080, then run `node capture.mjs`. This seeds a demo party and writes `shots/`.
2. Run `FF=/path/to/ffmpeg node render.mjs`. This steps `render(t)` in `ad.html` frame by frame and writes `video.mp4`.
3. Run `FF=/path/to/ffmpeg python3 mix.py`. This lays the music and the `CUES` sound effects onto `video.mp4` and writes `ryyppy-ad.mp4`.

The scripts load Playwright from `e2e/node_modules`. Run `npm install` in `e2e/` first, and set `PLAYWRIGHT_CHROMIUM_EXECUTABLE` if you use a preinstalled Chromium.

To preview single frames, run `node render.mjs 3.0,11.3,27.3`. The frames are written to `preview/`.

## Discord upload

`ryyppy-ad-discord.mp4` is the same video in about 7 MB. It is a two-pass x264 encode at 1850 kb/s video and 128 kb/s AAC audio, for upload limits of 8 MB:

    ffmpeg -i ryyppy-ad.mp4 -c:v libx264 -preset veryslow -tune animation -b:v 1850k -pass 1 -an -f null /dev/null
    ffmpeg -i ryyppy-ad.mp4 -c:v libx264 -preset veryslow -tune animation -b:v 1850k -pass 2 -c:a aac -b:a 128k -movflags +faststart ryyppy-ad-discord.mp4
