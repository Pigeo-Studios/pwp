package com.pwp.core.model;

public class ApiResponse {
    public boolean success;
    public String error;
    public Object data;

    public static ApiResponse ok(Object data) {
        ApiResponse r = new ApiResponse();
        r.success = true;
        r.data = data;
        return r;
    }

    public static ApiResponse error(String message) {
        ApiResponse r = new ApiResponse();
        r.success = false;
        r.error = message;
        return r;
    }
}
