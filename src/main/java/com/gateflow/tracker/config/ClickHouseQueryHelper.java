package com.gateflow.tracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Direct ClickHouse HTTP query helper — bypasses JDBC PreparedStatement parameter
 * binding incompatibility with some ClickHouse JDBC driver versions.
 */
@Slf4j
@Component
public class ClickHouseQueryHelper {

    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ClickHouseQueryHelper(ClickHouseConfig config) {
        StringBuilder sb = new StringBuilder(config.getUrl());
        if (!sb.toString().contains("?")) sb.append("?");
        if (config.getUsername() != null && !config.getUsername().isEmpty()) {
            sb.append("&user=").append(config.getUsername());
        }
        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            sb.append("&password=").append(config.getPassword());
        }
        // Remove JDBC prefix
        String url = sb.toString().replace("jdbc:clickhouse://", "http://");
        this.baseUrl = url;
    }

    public List<Map<String, Object>> query(String sql) {
        try {
            String encoded = URLEncoder.encode(sql + " FORMAT JSONEachRow", StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "&query=" + encoded))
                    .timeout(Duration.ofSeconds(30))
                    .GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.error("CH query failed: {} body={}", resp.statusCode(), resp.body().substring(0, Math.min(300, resp.body().length())));
                return List.of();
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (String line : resp.body().split("\n")) {
                if (line.isBlank()) continue;
                result.add(parseLine(line.trim()));
            }
            return result;
        } catch (Exception e) {
            log.error("CH HTTP query failed", e);
            return List.of();
        }
    }

    public Map<String, Object> queryOne(String sql) {
        List<Map<String, Object>> rows = query(sql);
        return rows.isEmpty() ? Collections.emptyMap() : rows.get(0);
    }

    public long queryCount(String sql) {
        Map<String, Object> row = queryOne(sql);
        for (Object v : row.values()) {
            if (v instanceof Number n) return n.longValue();
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseLine(String line) {
        Map<String, Object> row = new LinkedHashMap<>();
        if (!line.startsWith("{") || !line.endsWith("}")) return row;
        String content = line.substring(1, line.length() - 1);
        StringBuilder key = new StringBuilder(), val = new StringBuilder();
        boolean inKey = true, inStr = false;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (inStr) {
                if (c == '\\') val.append(content.charAt(++i));
                else if (c == '"') {
                    if (inKey) { key = new StringBuilder(val.toString()); val.setLength(0); inKey = false; }
                    else {
                        String v = val.toString();
                        try { row.put(key.toString(), Long.parseLong(v)); }
                        catch (NumberFormatException e1) {
                            try { row.put(key.toString(), Double.parseDouble(v)); }
                            catch (NumberFormatException e2) { row.put(key.toString(), v); }
                        }
                        key.setLength(0); inKey = true;
                    }
                    inStr = false;
                } else val.append(c);
            } else if (c == '"') inStr = true;
        }
        return row;
    }

    public static String esc(String s) {
        if (s == null) return "''";
        return "'" + s.replace("'", "''").replace("\\", "\\\\") + "'";
    }
}
