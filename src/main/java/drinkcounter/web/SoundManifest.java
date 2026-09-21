package drinkcounter.web;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Content-hashed URLs of the drink sound clips, scanned from the sounds
 * directory so that adding a clip means adding an .ogg and .mp3 pair with a
 * shared stem. Each clip is exposed as [oggUrl, mp3Url] - Howler tries them
 * in that order and plays whichever format the browser supports.
 */
@Component
public class SoundManifest {

    private static final String SOUNDS_CLASSPATH = "classpath:/public/static/sounds/*.*";
    private static final String SOUNDS_LOOKUP_PATH = "/static/sounds/";
    private static final List<String> FORMAT_PRIORITY = List.of("ogg", "mp3");

    private final ResourceUrlProvider resourceUrlProvider;

    /** The clips ship inside the jar, so the resolved list cannot change at runtime. */
    private final AtomicReference<List<List<String>>> soundUrls = new AtomicReference<>();

    public SoundManifest(ResourceUrlProvider resourceUrlProvider) {
        this.resourceUrlProvider = resourceUrlProvider;
    }

    public List<List<String>> getSoundUrls() {
        return soundUrls.updateAndGet(cached -> cached != null ? cached : resolveSoundUrls());
    }

    private List<List<String>> resolveSoundUrls() {
        Map<String, Map<String, String>> formatsByStem = new TreeMap<>();
        for (Resource clip : findClips()) {
            String filename = clip.getFilename();
            int dot = filename == null ? -1 : filename.lastIndexOf('.');
            if (dot < 0) {
                continue;
            }
            String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
            if (!FORMAT_PRIORITY.contains(ext)) {
                continue;
            }
            String stem = filename.substring(0, dot);
            formatsByStem.computeIfAbsent(stem, key -> new HashMap<>()).put(ext, resolveUrl(filename));
        }

        return formatsByStem.values().stream()
                .map(formats -> FORMAT_PRIORITY.stream().map(formats::get).filter(Objects::nonNull).toList())
                .filter(urls -> !urls.isEmpty())
                .toList();
    }

    private String resolveUrl(String filename) {
        String path = SOUNDS_LOOKUP_PATH + filename;
        return Objects.requireNonNullElse(resourceUrlProvider.getForLookupPath(path), path);
    }

    private Resource[] findClips() {
        try {
            return new PathMatchingResourcePatternResolver().getResources(SOUNDS_CLASSPATH);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
