package com.codeassistant.ingestion;

import com.codeassistant.api.ApiResponse;
import com.codeassistant.api.UnauthorizedException;
import com.codeassistant.domain.UserEntity;
import com.codeassistant.model.IngestRequest;
import com.codeassistant.model.IngestionJobView;
import com.codeassistant.security.CurrentUserResolver;
import com.codeassistant.security.SecurityMode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ingestion/jobs")
@RequiredArgsConstructor
public class IngestionJobController {

    private final IngestionJobService ingestionJobService;
    private final CurrentUserResolver currentUserResolver;
    private final SecurityMode securityMode;

    @PostMapping
    public ResponseEntity<ApiResponse<IngestionJobView>> createJob(@Valid @RequestBody IngestRequest request,
                                                                   @RequestHeader(value = "Authorization", required = false) String authHeader) {
        final UserEntity currentUser = requireUserIfNeeded(currentUserResolver.resolve(authHeader).orElse(null));
        IngestionJobView job = ingestionJobService.createJob(request, currentUser);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(job));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<IngestionJobView>> getJob(@PathVariable UUID jobId,
                                                                @RequestHeader(value = "Authorization", required = false) String authHeader) {
        final UserEntity currentUser = requireUserIfNeeded(currentUserResolver.resolve(authHeader).orElse(null));
        return ResponseEntity.ok(ApiResponse.ok(ingestionJobService.getJob(jobId, currentUser)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<IngestionJobView>>> listJobs(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        final UserEntity currentUser = requireUserIfNeeded(currentUserResolver.resolve(authHeader).orElse(null));
        return ResponseEntity.ok(ApiResponse.ok(ingestionJobService.listJobs(currentUser)));
    }

    @PostMapping("/{jobId}/retry")
    public ResponseEntity<ApiResponse<IngestionJobView>> retryJob(@PathVariable UUID jobId,
                                                                  @RequestHeader(value = "Authorization", required = false) String authHeader) {
        final UserEntity currentUser = requireUserIfNeeded(currentUserResolver.resolve(authHeader).orElse(null));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(ingestionJobService.retry(jobId, currentUser)));
    }

    private UserEntity requireUserIfNeeded(UserEntity user) {
        if (securityMode.isEnabled() && user == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }
}
