package drinkcounter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.session.Session;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Within one request, serves a session already read or saved from memory, so a page that commits
 * its response while rendering reads its session from the database once.
 */
class RequestSessionCache implements MethodInterceptor {

    private static final ThreadLocal<Map<String, Session>> SESSIONS = new ThreadLocal<>();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Map<String, Session> sessions = SESSIONS.get();
        if (sessions == null) {
            return invocation.proceed();
        }
        Object[] arguments = invocation.getArguments();
        switch (invocation.getMethod().getName()) {
            case "findById" -> {
                String id = (String) arguments[0];
                Session cached = sessions.get(id);
                if (cached != null && cached.getId().equals(id) && !cached.isExpired()) {
                    return cached;
                }
                Object found = invocation.proceed();
                if (found != null) {
                    sessions.put(id, (Session) found);
                }
                return found;
            }
            case "save" -> {
                Object result = invocation.proceed();
                Session saved = (Session) arguments[0];
                sessions.put(saved.getId(), saved);
                return result;
            }
            case "deleteById" -> {
                sessions.remove((String) arguments[0]);
                return invocation.proceed();
            }
            default -> {
                return invocation.proceed();
            }
        }
    }

    /** Bounds the cache to one request; must run outside Spring Session's filter. */
    static class Scope extends HttpFilter {

        @Override
        protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            if (SESSIONS.get() != null) {
                chain.doFilter(request, response);
                return;
            }
            SESSIONS.set(new HashMap<>());
            try {
                chain.doFilter(request, response);
            } finally {
                SESSIONS.remove();
            }
        }
    }
}
