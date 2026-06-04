package com.arnav.serverowner.module;

import com.google.gson.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModrinthModule {
    private static final String API = "https://api.modrinth.com/v2";
    private static final String USER_AGENT = "ar_tyyuu/even-better-server-owner/1.0.0 (modrinth contact)";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();

    public record SearchResult(String slug, String title, String description, long downloads, String projectType) {}

    public static CompletableFuture<List<SearchResult>> search(String query, int limit) {
        String facets = "[[\"project_type:mod\"],[\"categories:fabric\"]]";
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = API + "/search?query=" + encoded + "&facets=" + URLEncoder.encode(facets, StandardCharsets.UTF_8) + "&limit=" + limit;
        return getJson(url).thenApply(json -> {
            List<SearchResult> results = new ArrayList<>();
            if (json == null || !json.isJsonObject()) return results;
            JsonArray hits = json.getAsJsonObject().getAsJsonArray("hits");
            if (hits == null) return results;
            for (JsonElement el : hits) {
                JsonObject obj = el.getAsJsonObject();
                results.add(new SearchResult(
                    obj.get("slug").getAsString(),
                    obj.get("title").getAsString(),
                    obj.get("description").getAsString(),
                    obj.get("downloads").getAsLong(),
                    obj.get("project_type").getAsString()
                ));
            }
            return results;
        });
    }

    public static CompletableFuture<String> getProjectInfo(String slug) {
        return getJson(API + "/project/" + slug).thenApply(json -> {
            if (json == null || !json.isJsonObject()) return null;
            JsonObject obj = json.getAsJsonObject();
            String title = obj.get("title").getAsString();
            String description = obj.has("body") ? obj.get("body").getAsString() : obj.get("description").getAsString();
            long downloads = obj.get("downloads").getAsLong();
            String type = obj.get("project_type").getAsString();
            if (description.length() > 300) description = description.substring(0, 300) + "...";
            return "§e" + title + " §7[" + type + "]\n§fDownloads: §7" + downloads + "\n§f" + description;
        });
    }

    private static CompletableFuture<JsonElement> getJson(String url) {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", USER_AGENT)
            .GET().build();
        return CLIENT.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenApply(resp -> {
            if (resp.statusCode() != 200) return null;
            try { return JsonParser.parseString(resp.body()); }
            catch (Exception e) { return null; }
        });
    }
}
