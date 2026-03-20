package org.qrdlife.wikiconnect.wikimonitor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.qrdlife.wikiconnect.wikimonitor.model.RecentChange;
import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for evaluating user-defined filter rules against
 * RecentChange events.
 * Uses Spring Expression Language (SpEL) for dynamic rule evaluation.
 *
 * <p>
 * Supports multi-line expressions. Lines ending with logical operators (||, &&,
 * or, and)
 * are concatenated with the next line.
 * </p>
 *
 * <p>
 * Example using OR:
 * </p>
 *
 * <pre>
 *   user == 'BadUser' ||
 *   title matches '.*Spam.*'
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AbuseFilterService {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final org.qrdlife.wikiconnect.wikimonitor.repository.FilterRepository filterRepository;
    private final java.util.Map<Long, List<org.springframework.expression.Expression>> expressionCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<Long, List<org.qrdlife.wikiconnect.wikimonitor.model.Filter>> userFiltersCache = new java.util.concurrent.ConcurrentHashMap<>();

    private static final org.springframework.expression.spel.support.ReflectiveMethodResolver REFLECTIVE_METHOD_RESOLVER = new org.springframework.expression.spel.support.ReflectiveMethodResolver();

    private static final java.util.Set<String> ALLOWED_METHODS = java.util.Set.of(
            "test",
            "getId", "getType", "getNamespace", "getTitle", "getPageId", "getTitleUrl",
            "getUser", "getComment", "getParsedcomment", "getTimestamp", "getWiki",
            "isBot", "isMinor", "isPatrolled",
            "getNotifyUrl", "getServerUrl", "getServerName", "getServerScriptPath",
            "getServer_name", "getServer_url", "getServer_script_path",
            "getTitle_url", "getNotify_url", "getAdded_lines", "getRemoved_lines",
            "getUser_name", "getPage_namespace", "getOld_size", "getUser_rights", "getUser_groups",
            "contains", "containsIgnoreCase", "startsWith", "endsWith", "equals", "equalsIgnoreCase",
            "length", "isEmpty", "isBlank",
            "matches", "regexCount", "rcount", "count",
            "lower", "upper", "trim", "removeWhitespace", "normalizeArabic",
            "in", "anyContains", "allContains",
            "hour", "dayOfWeek", "isNightTime");

    /**
     * Checks if a RecentChange event matches the user's filter rules.
     *
     * @param rc   The RecentChange event to evaluate.
     * @param user The user whose rules should be applied.
     * @return List of matching filter names (empty if no match).
     */
    public List<String> matches(RecentChange rc, User user) {
        if (rc == null || user == null)
            return java.util.Collections.emptyList();

        List<org.qrdlife.wikiconnect.wikimonitor.model.Filter> activeFilters = userFiltersCache.computeIfAbsent(
            user.getId(), id -> filterRepository.findByUserAndIsActiveTrue(user)
        );
        
        if (activeFilters.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // Wrap RecentChange in FilterFunctions
        org.qrdlife.wikiconnect.wikimonitor.FilterFunctions root = new org.qrdlife.wikiconnect.wikimonitor.FilterFunctions(
                rc);

        SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withRootObject(root)
                .withMethodResolvers((ctx, target, name, args) -> {
                    if (target instanceof org.qrdlife.wikiconnect.wikimonitor.FilterFunctions) {
                        if (!ALLOWED_METHODS.contains(name)) {
                            // Method not in allowlist -> forbidden
                            return null;
                        }
                        return REFLECTIVE_METHOD_RESOLVER.resolve(ctx, target, name, args);
                    }
                    return null;
                })
                .build();
        List<String> matched = new java.util.ArrayList<>();
        
        for (org.qrdlife.wikiconnect.wikimonitor.model.Filter f : activeFilters) {
            String code = f.getFilterCode();
            if (code == null || code.trim().isEmpty()) continue;
            
            List<org.springframework.expression.Expression> exprs = expressionCache.computeIfAbsent(f.getId(), k -> {
                List<org.springframework.expression.Expression> parsed = new java.util.ArrayList<>();
                String[] lines = code.split("\\r?\\n");
                StringBuilder currentExpr = new StringBuilder();
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#"))
                        continue;
                    currentExpr.append(line).append(" ");
                    if (trimmed.endsWith("||") || trimmed.endsWith("&&") ||
                            trimmed.toLowerCase().endsWith(" or") || trimmed.toLowerCase().endsWith(" and")) {
                        continue;
                    }
                    try {
                        parsed.add(parser.parseExpression(currentExpr.toString().trim()));
                    } catch (Exception e) {
                        log.warn("Invalid expression in filter {}: {}", f.getName(), e.getMessage());
                    }
                    currentExpr.setLength(0);
                }
                return parsed;
            });
            
            if (exprs.isEmpty()) continue;
            
            boolean allStatementsMatched = true;
            for (org.springframework.expression.Expression expr : exprs) {
                try {
                    Boolean result = expr.getValue(context, Boolean.class);
                    if (!Boolean.TRUE.equals(result)) {
                        log.debug("Rule failed for user {}: {}", user.getUsername(), expr.getExpressionString());
                        allStatementsMatched = false;
                        break;
                    }
                } catch (Exception e) {
                    log.warn("Filter execution error for user {}: {}", user.getUsername(), e.getMessage());
                    allStatementsMatched = false;
                    break;
                }
            }
            
            if (allStatementsMatched) {
                log.info("Match found for user {} on change {} by filter {}", user.getUsername(), rc.getTitle(), f.getName());
                matched.add(f.getName());
            }
        }

        return matched; 
    }

    public void refreshRules(User user) {
        if (user != null && user.getId() != null) {
            userFiltersCache.remove(user.getId());
            List<org.qrdlife.wikiconnect.wikimonitor.model.Filter> filters = filterRepository.findByUser(user);
            for (org.qrdlife.wikiconnect.wikimonitor.model.Filter f : filters) {
                expressionCache.remove(f.getId());
            }
        }
    }
}
