package org.qrdlife.wikiconnect.wikimonitor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.qrdlife.wikiconnect.wikimonitor.exception.MaxActiveFiltersExceededException;
import org.qrdlife.wikiconnect.wikimonitor.model.Filter;
import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.qrdlife.wikiconnect.wikimonitor.repository.FilterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilterService {

    private final FilterRepository filterRepository;
    private final SettingsService settingsService;
    private final AbuseFilterService abuseFilterService;
    private final WikiStreamService wikiStreamService;

    public List<Filter> getUserFilters(User user) {
        return filterRepository.findByUser(user);
    }

    @Transactional
    public Filter createFilter(User user, String name, String filterCode) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Filter name cannot be empty");
        }
        
        // When creating a new filter, it defaults to active.
        // We must check if limit exceeded.
        int activeCount = filterRepository.countByUserAndIsActiveTrue(user);
        int maxFilters = settingsService.getMaxActiveFiltersPerUser();
        
        boolean activate = true;
        if (activeCount >= maxFilters) {
            log.warn("User {} reached maximum active filters.", user.getUsername());
            // If limit exceeded, create as inactive, or throw exception?
            // "Before activating a filter... throw MaxActiveFiltersExceededException"
            // The requirement says "Before activating a filter... throw Max...". Let's create it as inactive.
            // Or wait, "Test activation exceeding limit → throws exception" and "create filter with name".
            // Let's create as inactive if it would exceed. Or throw exception on create?
            // The prompt says "Handle MaxActiveFiltersExceededException Return user-friendly error".
            // Since it defaults to active = true, if we throw, they can't even save it.
            // It's better to throw so the UI can prompt them.
            throw new MaxActiveFiltersExceededException("Maximum active filters limit (" + maxFilters + ") reached. Please deactivate another filter first.");
        }

        Filter filter = new Filter(user, name, filterCode);
        filter.setActive(activate);
        filterRepository.save(filter);
        
        // Refresh active filters for user
        refreshServices(user);
        
        return filter;
    }

    @Transactional
    public void deleteFilter(User user, Long filterId) {
        Filter filter = filterRepository.findById(filterId)
            .orElseThrow(() -> new IllegalArgumentException("Filter not found"));
            
        if (!filter.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized");
        }
        
        filterRepository.delete(filter);
        refreshServices(user);
    }

    @Transactional
    public void toggleFilterStatus(User user, Long filterId, boolean status) {
        Filter filter = filterRepository.findById(filterId)
            .orElseThrow(() -> new IllegalArgumentException("Filter not found"));
            
        if (!filter.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized");
        }
        
        if (status && !filter.isActive()) {
            int activeCount = filterRepository.countByUserAndIsActiveTrue(user);
            int maxFilters = settingsService.getMaxActiveFiltersPerUser();
            if (activeCount >= maxFilters) {
                throw new MaxActiveFiltersExceededException("Maximum active filters limit (" + maxFilters + ") reached.");
            }
        }
        
        filter.setActive(status);
        filterRepository.save(filter);
        refreshServices(user);
    }

    @Transactional
    public void updateFilterName(User user, Long filterId, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Filter name cannot be empty");
        }

        Filter filter = filterRepository.findById(filterId)
            .orElseThrow(() -> new IllegalArgumentException("Filter not found"));
            
        if (!filter.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized");
        }
        
        filter.setName(name);
        filterRepository.save(filter);
    }
    
    @Transactional
    public void updateFilterCode(User user, Long filterId, String code) {
        Filter filter = filterRepository.findById(filterId)
            .orElseThrow(() -> new IllegalArgumentException("Filter not found"));
            
        if (!filter.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized");
        }
        
        filter.setFilterCode(code);
        filterRepository.save(filter);
        refreshServices(user);
    }

    @Transactional
    public void updateFilter(User user, Long filterId, String name, String code) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Filter name cannot be empty");
        }

        Filter filter = filterRepository.findById(filterId)
            .orElseThrow(() -> new IllegalArgumentException("Filter not found"));

        if (!filter.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized");
        }

        filter.setName(name.trim());
        filter.setFilterCode(code);
        filterRepository.save(filter);
        refreshServices(user);
    }

    private void refreshServices(User user) {
        abuseFilterService.refreshRules(user);
        wikiStreamService.updateUser(user);
    }
}
