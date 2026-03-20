package org.qrdlife.wikiconnect.wikimonitor.repository;

import org.qrdlife.wikiconnect.wikimonitor.model.Filter;
import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FilterRepository extends JpaRepository<Filter, Long> {
    List<Filter> findByUser(User user);
    int countByUserAndIsActiveTrue(User user);
    List<Filter> findByUserAndIsActiveTrue(User user);
}
