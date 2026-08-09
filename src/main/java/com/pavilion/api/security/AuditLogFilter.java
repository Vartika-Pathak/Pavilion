package com.pavilion.api.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pavilion.api.entity.AuditLog;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.AuditLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Logs meaningful activity across the app automatically: every successful admin/resident/guard
// write (create/update/delete), successful logins (any role), and a curated set of "page view"
// reads (VIEW_PATH_LABELS) — deliberately not every GET, which would flood this with routine
// polling/dashboard-refresh noise instead of things worth showing an admin.
@Component
public class AuditLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuditLogFilter.class);

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final List<String> SKIP_PATH_PREFIXES = List.of("/api/auth", "/api/audit-logs");
    private static final Map<String, String> VERB_LABELS =
            Map.of("POST", "Created", "PUT", "Updated", "PATCH", "Updated", "DELETE", "Deleted");
    private static final String[] LABEL_FIELDS = {"name", "title", "billNumber", "flatNumber", "email"};

    private static final String LOGIN_PATH = "/api/auth/login";

    // A curated allow-list of GET reads worth recording as "viewed X" activity — not every GET
    // (that would log every dashboard poll/refresh), just the pages a resident/guard/admin
    // meaningfully navigates to.
    private static final Map<String, String> VIEW_PATH_LABELS = Map.ofEntries(
            Map.entry("/api/events", "Viewed Events"),
            Map.entry("/api/gallery", "Viewed Gallery"),
            Map.entry("/api/news", "Viewed News"),
            Map.entry("/api/notices", "Viewed Notices"),
            Map.entry("/api/society-rules", "Viewed Society Rules"),
            Map.entry("/api/services", "Viewed Services"),
            Map.entry("/api/amenities", "Viewed Amenities"),
            Map.entry("/api/amenities/bookings/mine", "Viewed My Amenity Bookings"),
            Map.entry("/api/complaints", "Viewed Complaints"),
            Map.entry("/api/maintenance", "Viewed Maintenance Requests"),
            Map.entry("/api/reports/dashboard-summary", "Viewed Dashboard"),
            Map.entry("/api/reports/due-list", "Viewed Due List"),
            Map.entry("/api/resident-meetings", "Viewed Resident Meetings"),
            Map.entry("/api/members", "Viewed Members"),
            Map.entry("/api/emergency-alerts", "Viewed Emergency Alerts"),
            Map.entry("/api/visits", "Viewed Visitor Entries"));

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogFilter(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean isLogin = "POST".equals(method) && LOGIN_PATH.equals(path);
        boolean isWrite = WRITE_METHODS.contains(method) && SKIP_PATH_PREFIXES.stream().noneMatch(path::startsWith);
        boolean isView = "GET".equals(method) && VIEW_PATH_LABELS.containsKey(path);

        if (!isLogin && !isWrite && !isView) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, wrappedResponse);
            if (wrappedResponse.getStatus() < 300) {
                if (isLogin) {
                    recordLogin(wrappedResponse, path);
                } else {
                    recordActivity(request, wrappedResponse, path, isView);
                }
            }
        } finally {
            wrappedResponse.copyBodyToResponse();
        }
    }

    // Login can't be attributed via SecurityContextHolder — the incoming request has no session
    // cookie yet (that's exactly what this response is about to set), so the user is read straight
    // from the login response body instead.
    private void recordLogin(ContentCachingResponseWrapper response, String path) {
        try {
            JsonNode node = objectMapper.readTree(response.getContentAsByteArray());
            if (node == null || !node.isObject() || node.get("id") == null || node.get("name") == null) {
                return;
            }

            AuditLog entry = new AuditLog();
            entry.setAdminId(node.get("id").asLong());
            entry.setAdminName(node.get("name").asText());
            entry.setMethod("POST");
            entry.setPath(path);
            entry.setStatusCode(response.getStatus());
            entry.setSummary("Logged in");
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log for login", e);
        }
    }

    private void recordActivity(
            HttpServletRequest request, ContentCachingResponseWrapper response, String path, boolean isView) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication != null && authentication.getPrincipal() instanceof User user)) {
                return;
            }

            String summary;
            if (isView) {
                summary = VIEW_PATH_LABELS.get(path);
            } else {
                String resource = resourceNameFromPath(path);
                String verb = VERB_LABELS.getOrDefault(request.getMethod(), request.getMethod());
                String label = labelFromBody(response.getContentAsByteArray());
                if (label == null) {
                    label = labelFromPathVariables(request);
                }
                summary = verb + " " + resource + (label != null && !label.isEmpty() ? " (" + label + ")" : "");
            }

            AuditLog entry = new AuditLog();
            entry.setAdminId(user.getId());
            entry.setAdminName(user.getName());
            entry.setMethod(request.getMethod());
            entry.setPath(path);
            entry.setStatusCode(response.getStatus());
            entry.setSummary(summary);
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log", e);
        }
    }

    // path is like "/api/buildings/5" -> ["", "api", "buildings", "5"], so index 2 is the resource.
    private static String resourceNameFromPath(String path) {
        String[] segments = path.split("/");
        return segments.length > 2 ? segments[2] : path;
    }

    private String labelFromBody(byte[] bodyBytes) {
        if (bodyBytes == null || bodyBytes.length == 0) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(bodyBytes);
            if (node == null || !node.isObject()) {
                return null;
            }
            for (String key : LABEL_FIELDS) {
                JsonNode value = node.get(key);
                if (value != null && value.isTextual() && !value.asText().isEmpty()) {
                    return value.asText();
                }
            }
            JsonNode idNode = node.get("id");
            if (idNode != null && idNode.isNumber()) {
                return "#" + idNode.asText();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String labelFromPathVariables(HttpServletRequest request) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (attribute instanceof Map<?, ?> pathVariables) {
            Object id = pathVariables.get("id");
            if (id != null) {
                return "#" + id;
            }
        }
        return null;
    }
}
