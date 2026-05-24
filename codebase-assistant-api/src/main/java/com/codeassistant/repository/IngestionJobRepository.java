package com.codeassistant.repository;

import com.codeassistant.domain.IngestionJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IngestionJobRepository extends JpaRepository<IngestionJobEntity, UUID> {
    List<IngestionJobEntity> findAllByOrderByCreatedAtDesc();
    List<IngestionJobEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
