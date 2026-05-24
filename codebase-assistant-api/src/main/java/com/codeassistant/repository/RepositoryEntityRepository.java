package com.codeassistant.repository;

import com.codeassistant.domain.RepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositoryEntityRepository extends JpaRepository<RepositoryEntity, UUID> {
    List<RepositoryEntity> findByWorkspace_Id(UUID workspaceId);
    List<RepositoryEntity> findByUserIdOrderByUpdatedAtDesc(UUID userId);
    List<RepositoryEntity> findByWorkspace_IdAndUserId(UUID workspaceId, UUID userId);
    Optional<RepositoryEntity> findByRepoUrlAndBranch(String repoUrl, String branch);
    Optional<RepositoryEntity> findByRepoUrlAndBranchAndUserId(String repoUrl, String branch, UUID userId);
}
