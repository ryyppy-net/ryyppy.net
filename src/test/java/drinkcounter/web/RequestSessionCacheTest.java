package drinkcounter.web;

import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.MapSession;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RequestSessionCacheTest {

    private MapSessionRepository store;
    private MapSession session;

    @BeforeEach
    void setUp() {
        store = spy(new MapSessionRepository(new ConcurrentHashMap<>()));
        session = store.createSession();
        session.setAttribute("greeting", "hello");
        store.save(session);
    }

    @Test
    void pageThatCommitsWhileRenderingReadsTheSessionOnce() throws Exception {
        run(new RequestSessionCache.Scope(), new SessionRepositoryFilter<>(cached(store)), renderingPage());

        verify(store, times(1)).findById(session.getId());
    }

    @Test
    void withoutTheScopeTheSessionIsReadOnEverySave() throws Exception {
        run(new SessionRepositoryFilter<>(cached(store)), renderingPage());

        verify(store, times(2)).findById(session.getId());
    }

    @Test
    void attributesSetDuringTheRequestAreSaved() throws Exception {
        HttpServlet page = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
                request.getSession().setAttribute("greeting", "moi");
                response.getWriter().write("page");
                response.flushBuffer();
            }
        };

        run(new RequestSessionCache.Scope(), new SessionRepositoryFilter<>(cached(store)), page);

        assertEquals("moi", store.findById(session.getId()).getAttribute("greeting"));
    }

    @Test
    void invalidatedSessionIsNotServedFromTheCache() throws Exception {
        SessionRepository<MapSession> repository = cached(store);
        HttpServlet logout = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
                request.getSession().invalidate();
                response.flushBuffer();
            }
        };

        run(new RequestSessionCache.Scope(), new SessionRepositoryFilter<>(repository), logout);

        assertNull(store.findById(session.getId()));
    }

    private HttpServlet renderingPage() {
        return new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
                request.getSession().getAttribute("greeting");
                response.getWriter().write("page");
                response.flushBuffer();
            }
        };
    }

    private void run(Object... filtersAndServlet) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/index.html");
        request.setCookies(new Cookie("SESSION",
                Base64.getEncoder().encodeToString(session.getId().getBytes())));
        Filter[] filters = new Filter[filtersAndServlet.length - 1];
        System.arraycopy(filtersAndServlet, 0, filters, 0, filters.length);
        HttpServlet servlet = (HttpServlet) filtersAndServlet[filtersAndServlet.length - 1];
        new MockFilterChain(servlet, filters).doFilter(request, new MockHttpServletResponse());
    }

    @SuppressWarnings("unchecked")
    private static SessionRepository<MapSession> cached(MapSessionRepository repository) {
        ProxyFactory proxyFactory = new ProxyFactory(repository);
        proxyFactory.addAdvice(new RequestSessionCache());
        return (SessionRepository<MapSession>) proxyFactory.getProxy();
    }
}
