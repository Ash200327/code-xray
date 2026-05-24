package com.codeassistant.repository;

import com.codeassistant.domain.WorkspaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<WorkspaceEntity, UUID> {
    List<WorkspaceEntity> findByUserIdOrderByUpdatedAtDesc(UUID userId);
}
