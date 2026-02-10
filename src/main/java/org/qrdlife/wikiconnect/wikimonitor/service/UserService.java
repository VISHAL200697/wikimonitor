package org.qrdlife.wikiconnect.wikimonitor.service;

import lombok.RequiredArgsConstructor;
import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.qrdlife.wikiconnect.wikimonitor.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final SettingsService settingsService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public User findOrCreateUser(Long centralId, String username) {
        return userRepository.findByCentralId(centralId)
                .map(user -> {
                    if (!user.getUsername().equals(username)) {
                        log.info("Updating username for user ID {}: {} -> {}", user.getId(), user.getUsername(),
                                username);
                        user.setUsername(username);
                        return userRepository.save(user); // Update username if changed
                    }
                    return user;
                })
                .orElseGet(() -> {
                    log.info("Creating new user: {} (Central ID: {})", username, centralId);

                    // Check if this is the first user
                    long userCount = userRepository.count();
                    boolean isFirstUser = userCount == 0;
                    boolean autoApproveEnabled = settingsService.isAutoApproveEnabled();

                    User newUser = new User(centralId, username);
                    if (isFirstUser) {
                        newUser.setApproved(true);
                        newUser.setRole("ADMIN");
                        log.info("First user created as ADMIN and APPROVED");
                    } else {
                        newUser.setApproved(autoApproveEnabled);
                        newUser.setRole("USER");
                        log.info("New user created as USER with approval status: {}", autoApproveEnabled);
                    }

                    return userRepository.save(newUser);
                });
    }

    public java.util.List<User> findAll() {
        log.debug("Fetching all users");
        return userRepository.findAll();
    }

    public org.springframework.data.domain.Page<User> getAllUsers(org.springframework.data.domain.Pageable pageable) {
        log.debug("Fetching all users with pagination");
        return userRepository.findAll(pageable);
    }

    public org.springframework.data.domain.Page<User> searchUsers(String query,
                                                                  org.springframework.data.domain.Pageable pageable) {
        log.debug("Searching users with query: {}", query);
        return userRepository.findByUsernameContainingIgnoreCase(query, pageable);
    }

    public void updateUserStatus(Long id, boolean approved, String role) {
        log.debug("Request to update user status: id={}, approved={}, role={}", id, approved, role);
        if (id == null)
            return;
        userRepository.findById(id).ifPresent(user -> {
            user.setApproved(approved);
            user.setRole(role);
            userRepository.save(user);
            log.info("Updated user {} status: approved={}, role={}", user.getUsername(), approved, role);
        });
    }
}
