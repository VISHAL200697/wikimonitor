package org.qrdlife.wikiconnect.wikimonitor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qrdlife.wikiconnect.wikimonitor.model.Filter;
import org.qrdlife.wikiconnect.wikimonitor.model.RecentChange;
import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.qrdlife.wikiconnect.wikimonitor.repository.FilterRepository;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbuseFilterServiceTest {

    private AbuseFilterService abuseFilterService;
    private FilterRepository filterRepository;
    private User user;
    private RecentChange rc;

    @BeforeEach
    void setUp() {
        filterRepository = mock(FilterRepository.class);
        abuseFilterService = new AbuseFilterService(filterRepository);
        
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        rc = new RecentChange();
        rc.setTitle("Test Page");
        rc.setUser("WikiUser");
        rc.setBot(false);
    }

    private void mockActiveFilters(String... codes) {
        long idCounter = 1;
        List<Filter> filters = new java.util.ArrayList<>();
        for (String code : codes) {
            Filter f = new Filter();
            f.setId(idCounter);
            f.setName("Filter " + idCounter);
            f.setUser(user);
            f.setActive(true);
            f.setFilterCode(code);
            filters.add(f);
            idCounter++;
        }
        when(filterRepository.findByUserAndIsActiveTrue(user)).thenReturn(filters);
        when(filterRepository.findByUser(user)).thenReturn(filters);
    }

    @Test
    void testMatches_NoFilters() {
        when(filterRepository.findByUserAndIsActiveTrue(user)).thenReturn(Collections.emptyList());
        assertTrue(abuseFilterService.matches(rc, user).isEmpty(), "Should return empty list for no active filters");
    }

    @Test
    void testMatches_SimpleMatch() {
        mockActiveFilters("title == 'Test Page'");
        assertFalse(abuseFilterService.matches(rc, user).isEmpty(), "Should match exact title");
    }

    @Test
    void testMatches_SimpleMismatch() {
        mockActiveFilters("title == 'Other Page'");
        assertTrue(abuseFilterService.matches(rc, user).isEmpty(), "Should not match different title");
    }

    @Test
    void testMatches_MultipleConditions() {
        mockActiveFilters("title matches '.*Test.*'\nuser == 'WikiUser'");
        assertFalse(abuseFilterService.matches(rc, user).isEmpty(), "Should match all conditions");
    }

    @Test
    void testMatches_MultipleFilters_OrLogic() {
        // One failing, one passing -> should pass
        mockActiveFilters("title == 'Other Page'", "user == 'WikiUser'");
        assertFalse(abuseFilterService.matches(rc, user).isEmpty(), "Should match if at least one filter passes");
    }

    @Test
    void testMatches_InvalidExpression() {
        mockActiveFilters("invalid syntax ???");
        assertTrue(abuseFilterService.matches(rc, user).isEmpty(), "Should return empty for invalid expression");
    }

    @Test
    void testRefreshRules() {
        mockActiveFilters("title == 'Test Page'");
        assertFalse(abuseFilterService.matches(rc, user).isEmpty());

        // Let's say filter code was changed but not refreshed
        mockActiveFilters("title == 'Modified Page'");
        assertFalse(abuseFilterService.matches(rc, user).isEmpty(), "Should still match because cache is stale");

        abuseFilterService.refreshRules(user);
        assertTrue(abuseFilterService.matches(rc, user).isEmpty(), "Should verify mismatch after cache refresh");
    }

    @Test
    void testMatches_LineAdded_Url() {
        rc.setLineAdded("Check out this link: http://malicious-site.com/spam");
        rc.setDiffLoaded(true);
        mockActiveFilters("added_lines matches '.*https?://.*'");
        assertFalse(abuseFilterService.matches(rc, user).isEmpty(), "Should match URL in lineAdded");
    }

    @Test
    void testMatches_Alias_Namespace() {
        rc.setNamespace(0);
        mockActiveFilters("page_namespace == 0");
        assertFalse(abuseFilterService.matches(rc, user).isEmpty(), "Should match page_namespace == 0");
    }

    @Test
    void testMatches_MultilineMixed() {
        mockActiveFilters("title == 'Wrong' ||\ntitle == 'Test Page'\nuser == 'WikiUser'");
        assertFalse(abuseFilterService.matches(rc, user).isEmpty(), "Should handle mixed OR/AND logic properly");
    }

    @Test
    void testMatches_SecurityRestriction() {
        mockActiveFilters("getClass().getName() == 'org.qrdlife.wikiconnect.wikimonitor.FilterFunctions'");
        assertTrue(abuseFilterService.matches(rc, user).isEmpty(), "Should block access to restricted methods like getClass()");

        abuseFilterService.refreshRules(user);
        mockActiveFilters("getTitle() == 'Test Page'");
        assertFalse(abuseFilterService.matches(rc, user).isEmpty(), "Should allow access to allowed methods like getTitle()");
    }
}
