package com.reliaquest.api.util;

import com.reliaquest.api.exception.MockServerUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpServerErrorException;
import java.io.IOException;

public class ServerStatusInterceptor implements ClientHttpRequestInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ServerStatusInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        try {
            return execution.execute(request, body);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Mock server is unavailable: {}", e.getMessage());
            throw new MockServerUnavailableException("Mock server is unavailable. Please try again later.", e);
        }
    }
}

