package com.zerocode.deploy.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.Test;

class HttpGithubActionsClientTests {

    @Test
    void sendsWorkflowDispatchRequest() throws Exception {
        RecordingHttpClient httpClient = new RecordingHttpClient();
        HttpGithubActionsClient client = new HttpGithubActionsClient(new ObjectMapper(), httpClient);

        GithubActionsDispatchResult result = client.dispatch(new GithubActionsDispatchCommand(
                "https://api.github.test",
                "secret-token",
                "pppisnew",
                "zeroCode-ai-platform",
                "deploy.yml",
                "main",
                Map.of(
                        "app_id", "10",
                        "version_no", "2",
                        "project_type", "vue",
                        "artifact_url", "https://example.com/app.zip")));

        assertThat(result.statusCode()).isEqualTo(204);
        assertThat(httpClient.request.uri()).isEqualTo(URI.create(
                "https://api.github.test/repos/pppisnew/zeroCode-ai-platform/actions/workflows/deploy.yml/dispatches"));
        assertThat(httpClient.request.headers().firstValue("Authorization"))
                .contains("Bearer secret-token");
        assertThat(httpClient.body).contains(
                "\"ref\":\"main\"",
                "\"inputs\"",
                "\"app_id\":\"10\"",
                "\"artifact_url\":\"https://example.com/app.zip\"");
    }

    private static class RecordingHttpClient extends HttpClient {
        private HttpRequest request;
        private String body;

        @Override
        public <T> HttpResponse<T> send(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException {
            this.request = request;
            this.body = body(request);
            return new StaticHttpResponse<>(request, 204, null);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException("sendAsync is not used");
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException("sendAsync is not used");
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return null;
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        private static String body(HttpRequest request) throws IOException {
            HttpRequest.BodyPublisher publisher = request.bodyPublisher()
                    .orElseThrow(() -> new IOException("Missing request body"));
            BodySubscriber subscriber = new BodySubscriber();
            publisher.subscribe(subscriber);
            return new String(subscriber.bytes(), StandardCharsets.UTF_8);
        }
    }

    private record StaticHttpResponse<T>(
            HttpRequest request,
            int statusCode,
            T body) implements HttpResponse<T> {

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (name, value) -> true);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }

    private static class BodySubscriber implements java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer> {
        private final java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();

        @Override
        public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(java.nio.ByteBuffer item) {
            byte[] bytes = new byte[item.remaining()];
            item.get(bytes);
            outputStream.writeBytes(bytes);
        }

        @Override
        public void onError(Throwable throwable) {
            throw new IllegalStateException(throwable);
        }

        @Override
        public void onComplete() {
        }

        byte[] bytes() {
            return outputStream.toByteArray();
        }
    }
}
