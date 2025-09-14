package com.reliaquest.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wrapper class for Mock API responses
 *
 * @param <T> The type of data contained in the response
 */
/**
 * @author Naveen Kumar
 */
public class ApiResponse<T> {

    @JsonProperty("data")
    private T data;

    @JsonProperty("status")
    private String status;

    // Default constructor
    public ApiResponse() {}

    // All-args constructor
    public ApiResponse(T data, String status) {
        this.data = data;
        this.status = status;
    }

    // Getters and Setters
    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "MockApiResponse{" + "data=" + data + ", status='" + status + '\'' + '}';
    }
}
