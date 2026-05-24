package com.codeassistant.service;

import com.codeassistant.api.ResourceNotFoundException;
import com.codeassistant.domain.RepositoryEntity;
import com.codeassistant.domain.UserEntity;
import com.codeassistant.domain.WorkspaceEntity;
import com.codeassistant.model.CreateRepositoryRequest;
import com.codeassistant.model.RepositoryView;
import com.codeassistant.repository.RepositoryEntityRepository;
import com.codeassistant.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepositoryService {

    private final RepositoryEntityRepository repositoryRepository;
    private final WorkspaceRepository workspaceRepository;

    public RepositoryView create(CreateRepositoryRequest request, UserEntity user) {
        WorkspaceEntity workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + request.getWorkspaceId()));
        assertOwnership(workspace.getUser(), user);

        RepositoryEntity entity = new RepositoryEntity();
        entity.setId(UUID.randomUUID());
        entity.setWorkspace(workspace);
        entity.setName(request.getName().trim());
        entity.setRepoUrl(normalizeRepoUrl(request.getRepoUrl()));
        entity.setBranch(defaultBranch(request.getBranch()));
        entity.setUser(user);

        return toView(repositoryRepository.save(entity));
    }

    public RepositoryView get(UUID id, UserEntity user) {
        RepositoryEntity entity = repositoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + id));
        assertOwnership(entity.getUser(), user);
        return toView(entity);
    }

    public List<RepositoryView> list(UUID workspaceId, UserEntity user) {
        if (workspaceId == null) {
            if (user != null) {
                return repositoryRepository.findByUserIdOrderByUpdatedAtDesc(user.getId()).stream().map(this::toView).toList();
            }
            return repositoryRepository.findAll().stream().map(this::toView).toList();
        }
        if (user != null) {
            return repositoryRepository.findByWorkspace_IdAndUserId(workspaceId, user.getId()).stream().map(this::toView).toList();
        }
        return repositoryRepository.findByWorkspace_Id(workspaceId).stream().map(this::toView).toList();
    }

    public RepositoryView update(UUID id, CreateRepositoryRequest request, UserEntity user) {
        WorkspaceEntity workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + request.getWorkspaceId()));
        assertOwnership(workspace.getUser(), user);

        RepositoryEntity entity = repositoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + id));
        assertOwnership(entity.getUser(), user);

        entity.setWorkspace(workspace);
        entity.setName(request.getName().trim());
        entity.setRepoUrl(normalizeRepoUrl(request.getRepoUrl()));
        entity.setBranch(defaultBranch(request.getBranch()));
        return toView(repositoryRepository.save(entity));
    }

    public void delete(UUID id, UserEntity user) {
        RepositoryEntity entity = repositoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + id));
        assertOwnership(entity.getUser(), user);
        repositoryRepository.deleteById(id);
    }

    private RepositoryView toView(RepositoryEntity entity) {
        return RepositoryView.builder()
                .id(entity.getId())
                .workspaceId(entity.getWorkspace() != null ? entity.getWorkspace().getId() : null)
                .name(entity.getName())
                .repoUrl(entity.getRepoUrl())
                .branch(entity.getBranch())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .lastIngestedAt(entity.getLastIngestedAt())
                .build();
    }

    private String normalizeRepoUrl(String repoUrl) {
        String normalized = repoUrl.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith(".git")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized;
    }

    private String defaultBranch(String branch) {
        if (branch == null || branch.isBlank()) {
            return "main";
        }
        return branch.trim();
    }

    private void assertOwnership(UserEntity owner, UserEntity currentUser) {
        if (owner == null || currentUser == null) {
            return;
        }
        if (!owner.getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Repository not found");
        }
    }
}
