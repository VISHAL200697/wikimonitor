package org.qrdlife.wikiconnect.wikimonitor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qrdlife.wikiconnect.wikimonitor.model.RecentChange;
import org.qrdlife.wikiconnect.wikimonitor.model.User;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbuseFilterServiceTest {

    private AbuseFilterService abuseFilterService;
    private User user;
    private RecentChange rc;

    @BeforeEach
    void setUp() {
        abuseFilterService = new AbuseFilterService();
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        rc = new RecentChange();
        rc.setTitle("Test Page");
        rc.setUser("WikiUser");
        rc.setBot(false);
    }

    @Test
    void testMatches_NoFilterCode() {
        user.setFilterCode(null);
        assertFalse(abuseFilterService.matches(rc, user), "Should return false for null filter code");

        user.setFilterCode("");
        assertFalse(abuseFilterService.matches(rc, user), "Should return false for empty filter code");
    }

    @Test
    void testMatches_SimpleMatch() {
        user.setFilterCode("title == 'Test Page'");
        assertTrue(abuseFilterService.matches(rc, user), "Should match exact title");
    }

    @Test
    void testMatches_SimpleMismatch() {
        user.setFilterCode("title == 'Other Page'");
        assertFalse(abuseFilterService.matches(rc, user), "Should not match different title");
    }

    @Test
    void testMatches_MultipleConditions() {
        user.setFilterCode("title matches '.*Test.*'\nuser == 'WikiUser'");
        assertTrue(abuseFilterService.matches(rc, user), "Should match all conditions");
    }

    @Test
    void testMatches_MultipleConditions_FailOne() {
        user.setFilterCode("title matches '.*Test.*'\nuser == 'OtherUser'");
        assertFalse(abuseFilterService.matches(rc, user), "Should fail if one condition fails");
    }

    @Test
    void testMatches_InvalidExpression() {
        user.setFilterCode("invalid syntax ???");
        // Should handle gracefully and return false or just ignore the invalid line
        // depending on logic.
        // Current logic: catches exception and logs warning, but valid expressions
        // might still be in the list?
        // Wait, line 50 catches exception, so that line is skipped.
        // If "expressions.isEmpty()" -> returns false.
        // So effectively returns false if no valid expressions.
        assertFalse(abuseFilterService.matches(rc, user),
                "Should return false for invalid expression resulting in empty valid rules");
    }

    @Test
    void testRefreshRules() {
        user.setFilterCode("title == 'Test Page'");
        assertTrue(abuseFilterService.matches(rc, user));

        // Change logic but don't call refresh yet -> should theoretically use cached if
        // we didn't update user obj
        // But matches() gets code from user object passed in.
        // The cache key is user.getId().
        // If we change user.setFilterCode, the matches() method gets the string from
        // user.
        // But the cache computation "expressionCache.computeIfAbsent(user.getId(), k ->
        // ...)" uses the cache if present.
        // So checking if cache invalidation works requires:
        // 1. Run matches -> cached.
        // 2. Change code on user object.
        // 3. Run matches again -> should still use OLD logic if cache not cleared
        // (because it doesn't re-read code from user if ID in cache).

        user.setFilterCode("title == 'Modified Page'"); // Should fail now if updated
        assertTrue(abuseFilterService.matches(rc, user), "Should still match because cache is stale");

        abuseFilterService.refreshRules(user);
        assertFalse(abuseFilterService.matches(rc, user), "Should verify mismatch after cache refresh");
    }

    @Test
    void testMatches_LineAdded_Url() {
        // Mock diff data to avoid network calls
        rc.setLineAdded("Check out this link: http://malicious-site.com/spam");
        rc.setDiffLoaded(true);

        user.setFilterCode("added_lines matches '.*https?://.*'");
        assertTrue(abuseFilterService.matches(rc, user), "Should match URL in lineAdded");
    }

    @Test
    void testMatches_Alias_Namespace() {
        rc.setNamespace(0);
        user.setFilterCode("page_namespace == 0");
        assertTrue(abuseFilterService.matches(rc, user), "Should match page_namespace == 0");
    }

    @Test
    void testMatches_UserGroups() {
        // Check if property is accessible
        user.setFilterCode("user_groups != null");
        assertTrue(abuseFilterService.matches(rc, user), "Should access user_groups alias");

        abuseFilterService.refreshRules(user);
        // Updated to use in() function as method invocation on list is not supported in
        // SimpleCondition
        user.setFilterCode("!in('confirmed', user_groups)");
        assertTrue(abuseFilterService.matches(rc, user), "Should match ! in('confirmed', user_groups)");
    }

    @Test
    void testMatches_RCount() {
        String added = "link [https://spam.com]";
        rc.setLineAdded(added);
        rc.setDiffLoaded(true);

        // Simple regex: 'https'
        // Using rcount(text, regex)
        user.setFilterCode("rcount(added_lines, 'https') == 1");
        assertTrue(abuseFilterService.matches(rc, user), "Should match rcount(added_lines, 'https') == 1");

        // Complex regex with escaping
        // Regex: \[https
        // SpEL String: '\\[https'
        // Java String: "'\\\\[https'"
        user.setFilterCode("rcount(added_lines, '\\\\[https') == 1");
        assertTrue(abuseFilterService.matches(rc, user), "Should match rcount with escaped brackets");
    }

    @Test
    void testMatches_MultilineOR() {
        // user == 'WikiUser' (true)
        // OR
        // title == 'Wrong' (false)
        // Result should be TRUE because of OR
        user.setFilterCode("title == 'Wrong' ||\nuser == 'WikiUser'");
        assertTrue(abuseFilterService.matches(rc, user), "Should match if at least one OR condition is true");
    }

    @Test
    void testMatches_MultilineMixed() {
        // (title == 'Wrong' || title == 'Test Page') AND user == 'WikiUser'
        user.setFilterCode(
                "title == 'Wrong' ||\n" +
                        "title == 'Test Page'\n" +
                        "user == 'WikiUser'");
        assertTrue(abuseFilterService.matches(rc, user), "Should handle mixed OR/AND logic properly");
    }
}
