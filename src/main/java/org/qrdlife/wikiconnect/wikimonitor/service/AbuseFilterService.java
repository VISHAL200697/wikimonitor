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
    private final java.util.Map<Long, List<org.springframework.expression.Expression>> expressionCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Checks if a RecentChange event matches the user's filter rules.
     *
     * @param rc   The RecentChange event to evaluate.
     * @param user The user whose rules should be applied.
     * @return true if the event matches all valid rules, false otherwise.
     */
    public boolean matches(RecentChange rc, User user) {
        if (rc == null || user == null)
            return false;

        String code = user.getFilterCode();
        if (code == null || code.trim().isEmpty()) {
            return false; // No filters, show nothing? Or show everything?
            // Request said: "If the first filter fails...". Implies filtering down.
            // Typically if no filters, user probably wants to see everything?
            // "I would like to make the filters appear in a single input... execute them in
            // order."
            // If input is empty, maybe nothing matches? Or maybe everything?
            // Let's assume valid filter is required to match anything for now, or maybe if
            // empty, match everything?
            // Actually, if I want to "filter", usually empty means no constraints -> show
            // all?
            // But previous logic was "List of rules", if empty list -> show nothing (based
            // on app.js `if (!event.flagged) return;`).
            // Only flagged events were sent. Flagged meant at least one rule matched.
            // So if no rules, nothing matches.
            // I will stick to: If no code, return false (nothing matches).
        }

        List<org.springframework.expression.Expression> expressions = expressionCache.computeIfAbsent(user.getId(),
                k -> {
                    List<org.springframework.expression.Expression> exprs = new java.util.ArrayList<>();
                    StringBuilder currentExpression = new StringBuilder();

                    for (String line : code.split("\\R")) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#") || line.startsWith("//"))
                            continue;

                        // Check if line ends with an operator (&&, ||, and, or)
                        // This allows spreading a single logical expression across multiple lines
                        boolean endsWithOperator = line.matches("(?i).*\\s+(?:&&?|\\|\\|?|and|or)$");

                        if (currentExpression.length() > 0) {
                            currentExpression.append(" ");
                        }
                        currentExpression.append(line);

                        if (!endsWithOperator) {
                            // End of an expression statement
                            try {
                                exprs.add(parser.parseExpression(currentExpression.toString()));
                            } catch (Exception e) {
                                log.warn("Invalid expression for user {}: {}", user.getUsername(), currentExpression);
                            }
                            currentExpression.setLength(0);
                        }
                    }

                    // If there is leftover content (e.g. last line ended with an operator, or just
                    // no newline at end),
                    // try to parse it.
                    // Note: If last line ended with operator, SpEL parser will likely fail, which
                    // is correct behavior for invalid syntax.
                    if (currentExpression.length() > 0) {
                        try {
                            exprs.add(parser.parseExpression(currentExpression.toString()));
                        } catch (Exception e) {
                            log.warn("Invalid expression for user {}: {}", user.getUsername(), currentExpression);
                        }
                    }

                    return exprs;
                });

        if (expressions.isEmpty())
            return false;

        // Wrap RecentChange in FilterFunctions
        org.qrdlife.wikiconnect.wikimonitor.FilterFunctions root = new org.qrdlife.wikiconnect.wikimonitor.FilterFunctions(
                rc);

        SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withRootObject(root)
                .withMethodResolvers((ctx, target, name, args) -> {
                    if (target instanceof org.qrdlife.wikiconnect.wikimonitor.FilterFunctions) {
                        return new org.springframework.expression.spel.support.ReflectiveMethodResolver()
                                .resolve(ctx, target, name, args);
                    }
                    return null;
                })
                .build();
        log.debug("Evaluating {} rules for user {}", expressions.size(), user.getUsername());

        for (org.springframework.expression.Expression expr : expressions) {
            try {
                Boolean result = expr.getValue(context, Boolean.class);
                if (!Boolean.TRUE.equals(result)) {
                    log.debug("Rule failed for user {}: {}", user.getUsername(), expr.getExpressionString());
                    return false;
                }
            } catch (Exception e) {
                log.warn("Filter execution error for user {}: {}", user.getUsername(), e.getMessage());
                return false; // Error in execution -> fail
            }
        }

        log.info("Match found for user {} on change {}", user.getUsername(), rc.getTitle());
        return true; // All passed
    }

    /**
     * Clears the compiled expression cache for a specific user.
     * Should be called when a user updates their filter rules.
     *
     * @param user The user to refresh rules for.
     */
    public void refreshRules(User user) {
        if (user != null && user.getId() != null) {
            expressionCache.remove(user.getId());
        }
    }
}
