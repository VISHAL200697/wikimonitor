package org.qrdlife.wikiconnect.wikimonitor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long centralId;

    @Column(nullable = false)
    private String username;

    @Deprecated // deprecated: use filters table instead
    @Column(columnDefinition = "TEXT")
    private String filterCode;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Filter> filters = new ArrayList<>();

    @Column(nullable = false)
    private boolean approved = false;

    @Column(nullable = false)
    private String role = "USER"; // Default role

    public User(Long centralId, String username) {
        this.centralId = centralId;
        this.username = username;
    }

    @Override
    public String getPassword() {
        return null; // No password, using OAuth
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (!approved) {
            return Collections.emptyList(); // Unapproved users have no access
        }

        List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"));

        if ("ADMIN".equalsIgnoreCase(role)) {
            authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
