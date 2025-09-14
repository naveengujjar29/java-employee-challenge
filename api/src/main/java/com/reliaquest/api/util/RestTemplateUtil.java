package com.reliaquest.api.util;

import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestTemplateUtil {

    private final RestTemplate restTemplate;

    public RestTemplateUtil() {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);       // TCP connect in milliseconds
        requestFactory.setConnectionRequestTimeout(7000); // pool wait in milliseconds
        this.restTemplate = new RestTemplate(requestFactory);
        this.restTemplate.getInterceptors().add(new ServerStatusInterceptor());
    }

    public <T> ResponseEntity<T> get(String url, Class<T> responseType) {
        return restTemplate.getForEntity(url, responseType);
    }

    public <T> ResponseEntity<T> get(String url, org.springframework.core.ParameterizedTypeReference<T> responseType) {
        return restTemplate.exchange(url, HttpMethod.GET, null, responseType);
    }

    public <T> ResponseEntity<T> post(String url, Object requestBody, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);
        return restTemplate.postForEntity(url, entity, responseType);
    }

    public <T> ResponseEntity<T> post(String url, Object requestBody,
            org.springframework.core.ParameterizedTypeReference<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
    }

    public <T> ResponseEntity<T> delete(String url,
            org.springframework.core.ParameterizedTypeReference<T> responseType) {
        return restTemplate.exchange(url, HttpMethod.DELETE, null, responseType);
    }
}
