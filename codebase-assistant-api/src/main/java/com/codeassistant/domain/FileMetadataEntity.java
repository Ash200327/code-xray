package com.codeassistant.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "file_metadata")
public class FileMetadataEntity extends BaseAuditEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private RepositoryEntity repository;

    @Column(name = "file_path", nullable = false, length = 700)
    private String filePath;

    @Column(length = 80)
    private String language;

    @Column(name = "file_size_bytes")
    private long fileSizeBytes;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "last_indexed_at")
    private Instant lastIndexedAt;
}

