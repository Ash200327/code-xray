package com.codeassistant.repository;

import com.codeassistant.domain.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {
    List<ConversationEntity> findByRepository_IdOrderByUpdatedAtDesc(UUID repositoryId);
    List<ConversationEntity> findByRepository_IdAndUserIdOrderByUpdatedAtDesc(UUID repositoryId, UUID userId);
    List<ConversationEntity> findByUserIdOrderByUpdatedAtDesc(UUID userId);
}
