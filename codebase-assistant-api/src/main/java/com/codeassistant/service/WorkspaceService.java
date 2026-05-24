package com.codeassistant.service;

import com.codeassistant.api.ResourceNotFoundException;
import com.codeassistant.domain.WorkspaceEntity;
import com.codeassistant.domain.UserEntity;
import com.codeassistant.model.CreateWorkspaceRequest;
import com.codeassistant.model.WorkspaceView;
import com.codeassistant.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceView create(CreateWorkspaceRequest request, UserEntity user) {
        WorkspaceEntity entity = new WorkspaceEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(request.getName().trim());
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setUser(user);
        return toView(workspaceRepository.save(entity));
    }

    public List<WorkspaceView> list(UserEntity user) {
        if (user == null) {
            return workspaceRepository.findAll().stream().map(this::toView).toList();
        }
        return workspaceRepository.findByUserIdOrderByUpdatedAtDesc(user.getId()).stream().map(this::toView).toList();
    }

    public WorkspaceView get(UUID id, UserEntity user) {
        WorkspaceEntity workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + id));
        assertOwnership(workspace.getUser(), user);
        return toView(workspace);
    }

    public WorkspaceView update(UUID id, CreateWorkspaceRequest request, UserEntity user) {
        WorkspaceEntity workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + id));
        assertOwnership(workspace.getUser(), user);
        workspace.setName(request.getName().trim());
        workspace.setDescription(trimToNull(request.getDescription()));
        return toView(workspaceRepository.save(workspace));
    }

    public void delete(UUID id, UserEntity user) {
        WorkspaceEntity workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + id));
        assertOwnership(workspace.getUser(), user);
        if (!workspaceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Workspace not found: " + id);
        }
        workspaceRepository.deleteById(id);
    }

    private WorkspaceView toView(WorkspaceEntity entity) {
        return WorkspaceView.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void assertOwnership(UserEntity owner, UserEntity currentUser) {
        if (owner == null || currentUser == null) {
            return;
        }
        if (!owner.getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Workspace not found");
        }
    }
}
