package drinkcounter.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;

/**
 * No handler uses flash attributes. The default manager keeps them in the session, which
 * lives in the database, so it would read the session on every request, static files included.
 */
public class NoFlashMapManager implements FlashMapManager {

    @Override
    public FlashMap retrieveAndUpdate(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    @Override
    public void saveOutputFlashMap(FlashMap flashMap, HttpServletRequest request, HttpServletResponse response) {
        if (!flashMap.isEmpty()) {
            throw new UnsupportedOperationException("Flash attributes are not supported");
        }
    }
}
