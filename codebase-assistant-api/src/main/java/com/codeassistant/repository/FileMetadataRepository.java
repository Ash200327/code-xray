package com.codeassistant.repository;

import com.codeassistant.domain.FileMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepository extends JpaRepository<FileMetadataEntity, UUID> {
    Optional<FileMetadataEntity> findByRepositoryIdAndFilePath(UUID repositoryId, String filePath);
}

