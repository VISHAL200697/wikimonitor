package org.qrdlife.wikiconnect.wikimonitor.repository;

import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByCentralId(Long centralId);

    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
}
