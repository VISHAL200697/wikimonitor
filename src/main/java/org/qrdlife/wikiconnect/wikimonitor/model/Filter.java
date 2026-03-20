package org.qrdlife.wikiconnect.wikimonitor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "filters")
@Data
@NoArgsConstructor
public class Filter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String filterCode;

    @Column(nullable = false, name = "is_active")
    private boolean isActive = true;

    public Filter(User user, String name, String filterCode) {
        this.user = user;
        this.name = name;
        this.filterCode = filterCode;
        this.isActive = true;
    }
}
