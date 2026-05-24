package com.codeassistant.service;

import com.codeassistant.api.ResourceNotFoundException;
import com.codeassistant.domain.ConversationEntity;
import com.codeassistant.domain.MessageEntity;
import com.codeassistant.domain.RepositoryEntity;
import com.codeassistant.domain.UserEntity;
import com.codeassistant.model.*;
import com.codeassistant.repository.ConversationRepository;
import com.codeassistant.repository.MessageRepository;
import com.codeassistant.repository.RepositoryEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final RepositoryEntityRepository repositoryRepository;

    public ConversationView createConversation(CreateConversationRequest request, UserEntity user) {
        RepositoryEntity repository = repositoryRepository.findById(request.getRepositoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + request.getRepositoryId()));
        assertOwnership(repository.getUser(), user, "Repository not found");

        ConversationEntity entity = new ConversationEntity();
        entity.setId(UUID.randomUUID());
        entity.setRepository(repository);
        entity.setTitle(request.getTitle().trim());
        entity.setUser(user);
        return toView(conversationRepository.save(entity));
    }

    public List<ConversationView> listConversations(UUID repositoryId, UserEntity user) {
        if (repositoryId == null) {
            if (user != null) {
                return conversationRepository.findByUserIdOrderByUpdatedAtDesc(user.getId()).stream().map(this::toView).toList();
            }
            return conversationRepository.findAll().stream().map(this::toView).toList();
        }
        if (user != null) {
            return conversationRepository.findByRepository_IdAndUserIdOrderByUpdatedAtDesc(repositoryId, user.getId()).stream()
                    .map(this::toView)
                    .toList();
        }
        return conversationRepository.findByRepository_IdOrderByUpdatedAtDesc(repositoryId).stream()
                .map(this::toView)
                .toList();
    }

    public ConversationView renameConversation(UUID id, CreateConversationRequest request, UserEntity user) {
        ConversationEntity entity = conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + id));
        assertOwnership(entity.getUser(), user, "Conversation not found");
        entity.setTitle(request.getTitle().trim());
        return toView(conversationRepository.save(entity));
    }

    public void deleteConversation(UUID id, UserEntity user) {
        ConversationEntity entity = conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + id));
        assertOwnership(entity.getUser(), user, "Conversation not found");
        conversationRepository.deleteById(id);
    }

    public MessageView createMessage(CreateMessageRequest request, UserEntity user) {
        ConversationEntity conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + request.getConversationId()));
        assertOwnership(conversation.getUser(), user, "Conversation not found");

        MessageEntity message = new MessageEntity();
        message.setId(UUID.randomUUID());
        message.setConversation(conversation);
        message.setRole(request.getRole());
        message.setContent(request.getContent().trim());
        message.setCitationsJson(request.getCitationsJson());
        return toView(messageRepository.save(message));
    }

    public List<MessageView> listMessages(UUID conversationId, UserEntity user) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        assertOwnership(conversation.getUser(), user, "Conversation not found");
        return messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(this::toView)
                .toList();
    }

    public List<MessageView> getRecentMessages(UUID conversationId, int limit, UserEntity user) {
        List<MessageView> all = listMessages(conversationId, user);
        if (all.size() <= limit) {
            return all;
        }
        return all.subList(all.size() - limit, all.size());
    }

    public ConversationView getConversation(UUID conversationId, UserEntity user) {
        ConversationEntity entity = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        assertOwnership(entity.getUser(), user, "Conversation not found");
        return toView(entity);
    }

    public Optional<ConversationView> findConversation(UUID conversationId, UserEntity user) {
        return conversationRepository.findById(conversationId)
                .filter(entity -> user == null || entity.getUser() == null || entity.getUser().getId().equals(user.getId()))
                .map(this::toView);
    }

    private ConversationView toView(ConversationEntity entity) {
        return ConversationView.builder()
                .id(entity.getId())
                .repositoryId(entity.getRepository().getId())
                .title(entity.getTitle())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private MessageView toView(MessageEntity entity) {
        return MessageView.builder()
                .id(entity.getId())
                .conversationId(entity.getConversation().getId())
                .role(entity.getRole())
                .content(entity.getContent())
                .citationsJson(entity.getCitationsJson())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private void assertOwnership(UserEntity owner, UserEntity currentUser, String message) {
        if (owner == null || currentUser == null) {
            return;
        }
        if (!owner.getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException(message);
        }
    }
}
