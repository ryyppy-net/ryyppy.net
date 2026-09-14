package drinkcounter.web;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The list of drink sound clips, as content-hashed URLs, for the browser to
 * preload.
 *
 * The clips are discovered by scanning classpath:/public/static/sounds/ rather
 * than being listed anywhere, so a file dropped into that directory is played
 * without a second place to remember - 8.mp3 sat there unreferenced for years
 * because the old hardcoded "1..7" loop in the JavaScript never picked it up.
 *
 * Each name is resolved through ResourceUrlProvider, which runs the same
 * resource chain the /static/sounds/** handler is registered with, turning
 * /static/sounds/3.mp3 into /static/sounds/3-<content hash>.mp3. That is what
 * lets the handler serve them as immutable: the URL changes whenever the bytes
 * do, so a replaced clip is picked up without any cache busting, and an
 * unchanged one is never revalidated.
 *
 * Only .mp3 is listed. MP3 is the one format every browser decodes (the .ogg
 * copies next to them are a leftover from when that was not true), so the
 * client needs no format negotiation.
 */
@Component
public class SoundManifest {

    private static final String SOUNDS_CLASSPATH = "classpath:/public/static/sounds/*.mp3";
    private static final String SOUNDS_LOOKUP_PATH = "/static/sounds/";

    private final ResourceUrlProvider resourceUrlProvider;

    /**
     * Resolved once and reused: the clips are packaged in the jar, so neither
     * the file list nor any content hash can change while the app is running.
     */
    private volatile List<String> soundUrls;

    public SoundManifest(ResourceUrlProvider resourceUrlProvider) {
        this.resourceUrlProvider = resourceUrlProvider;
    }

    public List<String> getSoundUrls() {
        List<String> resolved = soundUrls;
        if (resolved == null) {
            synchronized (this) {
                resolved = soundUrls;
                if (resolved == null) {
                    resolved = resolveSoundUrls();
                    soundUrls = resolved;
                }
            }
        }
        return resolved;
    }

    private List<String> resolveSoundUrls() {
        List<String> names = new ArrayList<>();
        try {
            for (Resource resource : new PathMatchingResourcePatternResolver().getResources(SOUNDS_CLASSPATH)) {
                String filename = resource.getFilename();
                if (filename != null) {
                    names.add(filename);
                }
            }
        } catch (IOException e) {
            // No clips is a silent page, not a broken one - every caller
            // treats an empty manifest as "no sounds".
            return List.of();
        }

        // 1.mp3, 2.mp3, ... 10.mp3 rather than 1, 10, 2: the clips are picked
        // at random, so the order is cosmetic, but a stable numeric one keeps
        // the rendered manifest readable and diffable.
        names.sort(Comparator.comparing(SoundManifest::sortKey));

        List<String> urls = new ArrayList<>(names.size());
        for (String name : names) {
            String lookupPath = SOUNDS_LOOKUP_PATH + name;
            String versioned = resourceUrlProvider.getForLookupPath(lookupPath);
            urls.add(versioned != null ? versioned : lookupPath);
        }
        return List.copyOf(urls);
    }

    /** Sorts numeric filenames numerically, anything else lexically after them. */
    private static String sortKey(String filename) {
        String stem = filename.substring(0, filename.lastIndexOf('.'));
        try {
            return String.format("0%020d", Long.parseLong(stem));
        } catch (NumberFormatException e) {
            return "1" + stem;
        }
    }
}
