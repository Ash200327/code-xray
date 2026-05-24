package com.codeassistant.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "code_repositories")
public class RepositoryEntity extends BaseAuditEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private WorkspaceEntity workspace;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "repo_url", nullable = false, length = 500)
    private String repoUrl;

    @Column(nullable = false, length = 120)
    private String branch;

    @Column(name = "last_ingested_at")
    private Instant lastIngestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;
}
