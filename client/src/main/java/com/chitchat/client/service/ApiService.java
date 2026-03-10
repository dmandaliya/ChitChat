package com.chitchat.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;

public class ApiService {

    private static final MediaType JSON = MediaType.get("application/json");
    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;

    public ApiService(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Map<?, ?> login(String username, String password) throws IOException {
        return post("/api/auth/login", Map.of("username", username, "password", password));
    }

    public Map<?, ?> register(String fname, String lname, String username, String password) throws IOException {
        return post("/api/auth/register",
                Map.of("fname", fname, "lname", lname, "username", username, "password", password));
    }

    public void logout(String username) throws IOException {
        post("/api/auth/logout", Map.of("username", username));
    }

    private Map<?, ?> post(String path, Object body) throws IOException {
        String json = mapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .post(RequestBody.create(json, JSON))
                .build();
        try (Response response = http.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";
            Map<?, ?> result = mapper.readValue(responseBody, Map.class);
            if (!response.isSuccessful()) {
                Object err = result.get("error");
                throw new IOException(err != null ? err.toString() : "Request failed");
            }
            return result;
        }
    }
}
