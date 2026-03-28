package com.chitchat.client.service;

import com.chitchat.shared.UserPreferences;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for all REST communication with the ChitChat server.
 *
 * Uses OkHttp3 for making requests and Jackson for JSON serialization/deserialization.
 * Every public method in this class corresponds to one server endpoint. We keep the
 * actual HTTP mechanics (building requests, reading responses, error handling) in a few
 * private helpers at the bottom so the public API stays clean and readable.
 *
 * The baseUrl is injected at construction time so the same class works against
 * localhost during development and the DigitalOcean backend in production.
 */
public class ApiService {

    private static final MediaType JSON = MediaType.get("application/json");
    private final OkHttpClient http = new OkHttpClient();

    public OkHttpClient getHttp() { return http; }
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;

    public ApiService(String baseUrl) {
        this.baseUrl = baseUrl;
        // Register JavaTimeModule so LocalDateTime fields serialize to ISO strings
        // instead of numeric arrays, which is what the server expects.
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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

    public List<Map<String, Object>> getFriends(String username) throws IOException {
        return getList("/api/users/" + username + "/friends");
    }

    public List<Map<String, Object>> getPendingFriendRequests(String username) throws IOException {
        return getList("/api/users/" + username + "/friend-requests");
    }

    public void sendFriendRequest(String username, String targetUsername) throws IOException {
        post("/api/users/" + username + "/friend-requests", Map.of("targetUsername", targetUsername));
    }

    public void acceptFriendRequest(String username, String requesterUsername) throws IOException {
        post("/api/users/" + username + "/friend-requests/accept", Map.of("requesterUsername", requesterUsername));
    }

    public void rejectFriendRequest(String username, String requesterUsername) throws IOException {
        post("/api/users/" + username + "/friend-requests/reject", Map.of("requesterUsername", requesterUsername));
    }

    public void removeFriend(String username, String friendUsername) throws IOException {
        delete("/api/users/" + username + "/friends/" + friendUsername);
    }

    public List<Map<String, Object>> getBlockedUsers(String username) throws IOException {
        return getList("/api/users/" + username + "/blocked");
    }

    public void blockUser(String username, String targetUsername) throws IOException {
        post("/api/users/" + username + "/blocked", Map.of("targetUsername", targetUsername));
    }

    public void unblockUser(String username, String targetUsername) throws IOException {
        delete("/api/users/" + username + "/blocked/" + targetUsername);
    }

    public List<Map<String, Object>> searchUsers(String query) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/api/users/search");
        if (url == null) {
            throw new IOException("Invalid search URL");
        }
        HttpUrl built = url.newBuilder().addQueryParameter("q", query).build();
        return getListByUrl(built);
    }

    public Map<String, Object> getProfile(String username) throws IOException {
        return getMap("/api/users/" + username + "/profile");
    }

    public void updateProfile(String username, String status, String bio) throws IOException {
        put("/api/users/" + username + "/profile", Map.of(
                "status", status != null ? status : "Online",
                "bio", bio != null ? bio : ""
        ));
    }

    public UserPreferences getPreferences(String username) throws IOException {
        return getObject("/api/users/" + username + "/preferences", UserPreferences.class);
    }

    public void updatePreferences(String username, UserPreferences prefs) throws IOException {
        put("/api/users/" + username + "/preferences", prefs);
    }

    public List<Map<String, Object>> getRooms() throws IOException {
        return getList("/api/rooms");
    }

    public Map<String, Object> createRoom(String name, String description, String username) throws IOException {
        return post("/api/rooms", Map.of("name", name, "description", description, "username", username));
    }

    public Map<String, Object> joinRoom(long roomId, String username) throws IOException {
        return post("/api/rooms/" + roomId + "/join", Map.of("username", username));
    }

    public void leaveRoom(long roomId, String username) throws IOException {
        post("/api/rooms/" + roomId + "/leave", Map.of("username", username));
    }

    public void addReaction(String messageId, String username, String emoji) throws IOException {
        post("/api/messages/" + messageId + "/react", Map.of("username", username, "emoji", emoji));
    }

    public void deleteMessage(String messageId, String username) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/messages/" + messageId + "?username=" + username)
                .delete()
                .build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "{}";
                Map<?, ?> result = mapper.readValue(body, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>>(){});
                Object err = result.get("error");
                throw new IOException(err != null ? err.toString() : "Delete failed");
            }
        }
    }

    public List<Map<String, Object>> getPublicMessages() throws IOException {
        return getList("/api/messages/public");
    }

    public List<Map<String, Object>> getPrivateMessages(String user1, String user2) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/api/messages/private");
        if (url == null) {
            throw new IOException("Invalid private history URL");
        }
        HttpUrl built = url.newBuilder()
                .addQueryParameter("user1", user1)
                .addQueryParameter("user2", user2)
                .build();
        return getListByUrl(built);
    }

    public List<Map<String, Object>> getRoomMessages(String roomId) throws IOException {
        return getList("/api/messages/room/" + roomId);
    }

    // ───── Private HTTP helpers ──────────────────────────────────────────────

    // These helpers keep the public methods above free of boilerplate.
    // All HTTP calls are synchronous — callers are expected to run them off
    // the JavaFX Application Thread (e.g. inside a CompletableFuture).

    private Map<String, Object> post(String path, Object body) throws IOException {
        Request request = buildJsonRequest(baseUrl + path, "POST", body);
        return executeMap(request);
    }

    private Map<String, Object> put(String path, Object body) throws IOException {
        Request request = buildJsonRequest(baseUrl + path, "PUT", body);
        return executeMap(request);
    }

    private Map<String, Object> getMap(String path) throws IOException {
        Request request = new Request.Builder().url(baseUrl + path).get().build();
        return executeMap(request);
    }

    private <T> T getObject(String path, Class<T> type) throws IOException {
        Request request = new Request.Builder().url(baseUrl + path).get().build();
        return executeObject(request, type);
    }

    private List<Map<String, Object>> getList(String path) throws IOException {
        Request request = new Request.Builder().url(baseUrl + path).get().build();
        return executeList(request);
    }

    // Separate overload for cases where we need to build a URL with query parameters
    // (search, private message history) before passing it to OkHttp.
    private List<Map<String, Object>> getListByUrl(HttpUrl url) throws IOException {
        Request request = new Request.Builder().url(url).get().build();
        return executeList(request);
    }

    private void delete(String path) throws IOException {
        Request request = new Request.Builder().url(baseUrl + path).delete().build();
        executeMap(request);
    }

    private Request buildJsonRequest(String url, String method, Object body) throws IOException {
        String json = mapper.writeValueAsString(body);
        RequestBody requestBody = RequestBody.create(json, JSON);
        Request.Builder builder = new Request.Builder().url(url);
        if ("POST".equals(method)) {
            builder.post(requestBody);
        } else if ("PUT".equals(method)) {
            builder.put(requestBody);
        } else {
            throw new IllegalArgumentException("Unsupported method: " + method);
        }
        return builder.build();
    }

    // Executes a request and deserializes the response body into a Map.
    // If the server returns a non-2xx status, we pull the "error" field from the
    // response body and throw it as an IOException so callers get a readable message.
    private Map<String, Object> executeMap(Request request) throws IOException {
        try (Response response = http.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";
            Map<String, Object> result = mapper.readValue(responseBody,
                    new TypeReference<Map<String, Object>>() {});
            if (!response.isSuccessful()) {
                Object err = result.get("error");
                throw new IOException(err != null ? err.toString() : "Request failed");
            }
            return result;
        }
    }

    private List<Map<String, Object>> executeList(Request request) throws IOException {
        try (Response response = http.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "[]";
            List<Map<String, Object>> result = mapper.readValue(responseBody,
                    new TypeReference<List<Map<String, Object>>>() {});
            if (!response.isSuccessful()) {
                throw new IOException("Request failed: " + response.code());
            }
            return result;
        }
    }

    private <T> T executeObject(Request request, Class<T> type) throws IOException {
        try (Response response = http.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";
            if (!response.isSuccessful()) {
                Map<String, Object> result = mapper.readValue(responseBody,
                        new TypeReference<Map<String, Object>>() {});
                Object err = result.get("error");
                throw new IOException(err != null ? err.toString() : "Request failed");
            }
            return mapper.readValue(responseBody, type);
        }
    }
}
