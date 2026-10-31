package drinkcounter.analytics;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostHogProxyControllerTest {

    private final PostHogHttp http = mock(PostHogHttp.class);
    private final PostHogProxyController controller = controller("phc_key");

    private PostHogProxyController controller(String key) {
        return new PostHogProxyController(
                new PostHogProperties(key, "https://eu.i.posthog.com", "https://eu-assets.i.posthog.com", "local"), http);
    }

    @SuppressWarnings("unchecked")
    private void upstreamAnswers(int status, byte[] body, Map<String, List<String>> headers) throws Exception {
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(response.headers()).thenReturn(HttpHeaders.of(headers, (a, b) -> true));
        when(http.send(any())).thenReturn(response);
    }

    private HttpRequest forwarded() throws Exception {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(captor.capture());
        return captor.getValue();
    }

    private static MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr("203.0.113.9");
        return request;
    }

    @Test
    void ingestionRequestsGoToTheIngestionHostWithTheClientAddress() throws Exception {
        upstreamAnswers(200, "{\"status\":1}".getBytes(), Map.of("Content-Type", List.of("application/json")));
        MockHttpServletRequest request = request("POST", "/ph/e/");
        request.setQueryString("compression=gzip-js&ver=1");
        request.setContent("payload".getBytes());
        request.addHeader("Content-Type", "text/plain");
        request.addHeader("Cookie", "SESSION=secret");

        ResponseEntity<byte[]> response = controller.proxy(request);

        HttpRequest upstream = forwarded();
        assertEquals("https://eu.i.posthog.com/e/?compression=gzip-js&ver=1", upstream.uri().toString());
        assertEquals("POST", upstream.method());
        assertEquals("203.0.113.9", upstream.headers().firstValue("X-Forwarded-For").orElseThrow());
        assertEquals("text/plain", upstream.headers().firstValue("Content-Type").orElseThrow());
        assertEquals(false, upstream.headers().firstValue("Cookie").isPresent());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("application/json", response.getHeaders().getFirst("Content-Type"));
        assertArrayEquals("{\"status\":1}".getBytes(), response.getBody());
    }

    @Test
    void libraryFilesGoToTheAssetsHost() throws Exception {
        upstreamAnswers(200, "js".getBytes(), Map.of("Content-Encoding", List.of("br"), "Cache-Control", List.of("max-age=300")));

        ResponseEntity<byte[]> response = controller.proxy(request("GET", "/ph/static/array.js"));

        assertEquals("https://eu-assets.i.posthog.com/static/array.js", forwarded().uri().toString());
        assertEquals("br", response.getHeaders().getFirst("Content-Encoding"));
        assertEquals("max-age=300", response.getHeaders().getFirst("Cache-Control"));
    }

    @Test
    void upstreamStatusIsPassedThrough() throws Exception {
        upstreamAnswers(404, new byte[0], Map.of());

        assertEquals(HttpStatus.NOT_FOUND, controller.proxy(request("GET", "/ph/static/missing.js")).getStatusCode());
    }

    @Test
    void answersNotFoundWhenAnalyticsIsOff() throws Exception {
        assertEquals(HttpStatus.NOT_FOUND, controller("").proxy(request("GET", "/ph/static/array.js")).getStatusCode());

        verify(http, never()).send(any());
    }

    @Test
    void refusesPathsThatCouldLeaveTheFixedHosts() throws Exception {
        for (String uri : List.of("/ph/../admin", "/ph@evil.example/x", "/ph/a%2e%2e/b", "/ph/a b")) {
            assertEquals(HttpStatus.BAD_REQUEST, controller.proxy(request("GET", uri)).getStatusCode(), uri);
        }

        verify(http, never()).send(any());
    }

    @Test
    void refusesAMalformedQueryString() throws Exception {
        MockHttpServletRequest request = request("GET", "/ph/decide/");
        request.setQueryString("v=3&x=a b");

        assertEquals(HttpStatus.BAD_REQUEST, controller.proxy(request).getStatusCode());
    }

    @Test
    void refusesAnOversizedBody() throws Exception {
        MockHttpServletRequest request = request("POST", "/ph/e/");
        request.setContent(new byte[2 * 1024 * 1024 + 1]);

        assertEquals(HttpStatus.CONTENT_TOO_LARGE, controller.proxy(request).getStatusCode());
        verify(http, never()).send(any());
    }

    @Test
    void answersBadGatewayWhenPostHogCannotBeReached() throws Exception {
        when(http.send(any())).thenThrow(new IOException("down"));

        assertEquals(HttpStatus.BAD_GATEWAY, controller.proxy(request("GET", "/ph/decide/")).getStatusCode());
    }
}
