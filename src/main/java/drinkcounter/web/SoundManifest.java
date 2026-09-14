package drinkcounter.web;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Content-hashed URLs of the drink sound clips, scanned from the sounds
 * directory so that adding a clip means only adding a file. Only .mp3 is
 * listed - it is the one format every browser decodes.
 */
@Component
public class SoundManifest {

    private static final String SOUNDS_CLASSPATH = "classpath:/public/static/sounds/*.mp3";
    private static final String SOUNDS_LOOKUP_PATH = "/static/sounds/";

    private final ResourceUrlProvider resourceUrlProvider;

    /** The clips ship inside the jar, so the resolved list cannot change at runtime. */
    private final AtomicReference<List<String>> soundUrls = new AtomicReference<>();

    public SoundManifest(ResourceUrlProvider resourceUrlProvider) {
        this.resourceUrlProvider = resourceUrlProvider;
    }

    public List<String> getSoundUrls() {
        return soundUrls.updateAndGet(cached -> cached != null ? cached : resolveSoundUrls());
    }

    private List<String> resolveSoundUrls() {
        return Arrays.stream(findClips())
                .map(Resource::getFilename)
                .filter(Objects::nonNull)
                .sorted()
                .map(filename -> SOUNDS_LOOKUP_PATH + filename)
                .map(path -> Objects.requireNonNullElse(resourceUrlProvider.getForLookupPath(path), path))
                .toList();
    }

    private Resource[] findClips() {
        try {
            return new PathMatchingResourcePatternResolver().getResources(SOUNDS_CLASSPATH);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
