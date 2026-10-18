package drinkcounter.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.FlashMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class NoFlashMapManagerTest {

    private final NoFlashMapManager manager = new NoFlashMapManager();

    @Test
    void retrievingFlashAttributesDoesNotReadTheSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());
        MockHttpServletRequest spied = spy(request);

        assertNull(manager.retrieveAndUpdate(spied, new MockHttpServletResponse()));

        verify(spied, never()).getSession();
        verify(spied, never()).getSession(anyBoolean());
    }

    @Test
    void savingNoFlashAttributesIsAllowed() {
        assertDoesNotThrow(() -> manager.saveOutputFlashMap(
                new FlashMap(), new MockHttpServletRequest(), new MockHttpServletResponse()));
    }

    @Test
    void savingFlashAttributesFailsLoudly() {
        FlashMap flashMap = new FlashMap();
        flashMap.put("message", "saved");

        assertThrows(UnsupportedOperationException.class, () -> manager.saveOutputFlashMap(
                flashMap, new MockHttpServletRequest(), new MockHttpServletResponse()));
    }
}
