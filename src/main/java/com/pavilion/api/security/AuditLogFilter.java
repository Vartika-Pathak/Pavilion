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

// Logs every successful admin write (create/update/delete) across the app automatically, so
// new admin resources get audited without extra code at each call site. Skips auth and this
// endpoint's own read path; everything else that mutates state is covered.
@Component
public class AuditLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuditLogFilter.class);

    private static final Set<String> AUDITED_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final List<String> SKIP_PATH_PREFIXES = List.of("/api/auth", "/api/audit-logs");
    private static final Map<String, String> VERB_LABELS =
            Map.of("POST", "Created", "PUT", "Updated", "PATCH", "Updated", "DELETE", "Deleted");
    private static final String[] LABEL_FIELDS = {"name", "title", "billNumber", "flatNumber", "email"};

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
        if (!AUDITED_METHODS.contains(request.getMethod()) || SKIP_PATH_PREFIXES.stream().anyMatch(path::startsWith)) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, wrappedResponse);
            if (wrappedResponse.getStatus() < 300) {
                recordAuditLog(request, wrappedResponse, path);
            }
        } finally {
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void recordAuditLog(HttpServletRequest request, ContentCachingResponseWrapper response, String path) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication != null && authentication.getPrincipal() instanceof User user)) {
                return;
            }

            String resource = resourceNameFromPath(path);
            String verb = VERB_LABELS.getOrDefault(request.getMethod(), request.getMethod());
            String label = labelFromBody(response.getContentAsByteArray());
            if (label == null) {
                label = labelFromPathVariables(request);
            }
            String summary = verb + " " + resource + (label != null && !label.isEmpty() ? " (" + label + ")" : "");

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
