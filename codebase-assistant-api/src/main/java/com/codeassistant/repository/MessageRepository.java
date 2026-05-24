package com.codeassistant.repository;

import com.codeassistant.domain.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {
    List<MessageEntity> findByConversation_IdOrderByCreatedAtAsc(UUID conversationId);
}
