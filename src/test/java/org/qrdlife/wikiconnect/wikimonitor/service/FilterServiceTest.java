package org.qrdlife.wikiconnect.wikimonitor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.qrdlife.wikiconnect.wikimonitor.exception.MaxActiveFiltersExceededException;
import org.qrdlife.wikiconnect.wikimonitor.model.Filter;
import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.qrdlife.wikiconnect.wikimonitor.repository.FilterRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FilterServiceTest {

    private FilterRepository filterRepository;
    private SettingsService settingsService;
    private AbuseFilterService abuseFilterService;
    private WikiStreamService wikiStreamService;
    private FilterService filterService;

    private User testUser;

    @BeforeEach
    void setUp() {
        filterRepository = mock(FilterRepository.class);
        settingsService = mock(SettingsService.class);
        abuseFilterService = mock(AbuseFilterService.class);
        wikiStreamService = mock(WikiStreamService.class);
        filterService = new FilterService(filterRepository, settingsService, abuseFilterService, wikiStreamService);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("alice");
    }

    @Test
    void testCreateFilter_Success() {
        when(settingsService.getMaxActiveFiltersPerUser()).thenReturn(3);
        when(filterRepository.countByUserAndIsActiveTrue(testUser)).thenReturn(0);

        filterService.createFilter(testUser, "My Filter", "title == 'Test'");

        ArgumentCaptor<Filter> captor = ArgumentCaptor.forClass(Filter.class);
        verify(filterRepository).save(captor.capture());

        Filter saved = captor.getValue();
        assertEquals("My Filter", saved.getName());
        assertEquals("title == 'Test'", saved.getFilterCode());
        assertTrue(saved.isActive());
        assertEquals(testUser, saved.getUser());
    }

    @Test
    void testCreateFilter_ExceedsLimit() {
        when(settingsService.getMaxActiveFiltersPerUser()).thenReturn(3);
        when(filterRepository.countByUserAndIsActiveTrue(testUser)).thenReturn(3);

        assertThrows(MaxActiveFiltersExceededException.class, () -> {
            filterService.createFilter(testUser, "My Filter", "title == 'Test'");
        });

        verify(filterRepository, never()).save(any());
    }

    @Test
    void testToggleFilter_Activate_Success() {
        Filter f = new Filter();
        f.setId(10L);
        f.setUser(testUser);
        f.setActive(false);

        when(filterRepository.findById(10L)).thenReturn(Optional.of(f));
        when(settingsService.getMaxActiveFiltersPerUser()).thenReturn(3);
        when(filterRepository.countByUserAndIsActiveTrue(testUser)).thenReturn(2);

        filterService.toggleFilterStatus(testUser, 10L, true);

        assertTrue(f.isActive());
        verify(filterRepository).save(f);
    }

    @Test
    void testToggleFilter_Activate_ExceedsLimit() {
        Filter f = new Filter();
        f.setId(10L);
        f.setUser(testUser);
        f.setActive(false);

        when(filterRepository.findById(10L)).thenReturn(Optional.of(f));
        when(settingsService.getMaxActiveFiltersPerUser()).thenReturn(3);
        when(filterRepository.countByUserAndIsActiveTrue(testUser)).thenReturn(3);

        assertThrows(MaxActiveFiltersExceededException.class, () -> {
            filterService.toggleFilterStatus(testUser, 10L, true);
        });

        assertFalse(f.isActive());
        verify(filterRepository, never()).save(any());
    }

    @Test
    void testToggleFilter_Deactivate_AlwaysSucceeds() {
        Filter f = new Filter();
        f.setId(10L);
        f.setUser(testUser);
        f.setActive(true);

        when(filterRepository.findById(10L)).thenReturn(Optional.of(f));
        // Even if count is high, deactivating should always work

        filterService.toggleFilterStatus(testUser, 10L, false);

        assertFalse(f.isActive());
        verify(filterRepository).save(f);
    }

    @Test
    void testDeleteFilter_Success() {
        Filter f = new Filter();
        f.setId(10L);
        f.setUser(testUser);

        when(filterRepository.findById(10L)).thenReturn(Optional.of(f));

        filterService.deleteFilter(testUser, 10L);

        verify(filterRepository).delete(f);
    }

}
