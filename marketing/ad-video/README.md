# 15-second ad video

`ryyppy-ad.mp4` is 1080x1920, 30 fps, and 15 s long. It has the app's own drink sounds: one on the tap and one on the end card.

Rebuild it:

1. Start the app on :8080, then run `node capture.mjs`. This seeds a demo party and writes `shots/`.
2. Run `FF=/path/to/ffmpeg node render.mjs`. This steps `render(t)` in `ad.html` frame by frame and writes `video.mp4`.
3. Mix in the audio:
   `ffmpeg -i video.mp4 -i ../../src/main/resources/public/static/sounds/1.mp3 -i ../../src/main/resources/public/static/sounds/2.mp3 -filter_complex "[1]adelay=5950|5950[a];[2]adelay=13450|13450[b];anullsrc=r=44100:cl=stereo,atrim=0:15[s];[s][a][b]amix=inputs=3:duration=first:normalize=0[out]" -map 0:v -map "[out]" -c:v copy -c:a aac ryyppy-ad.mp4`

The scripts load Playwright from `e2e/node_modules`. Run `npm install` in `e2e/` first.

To preview single frames, run `node render.mjs 3.0,6.3,12.3`. The frames are written to `preview/`, which must exist.
