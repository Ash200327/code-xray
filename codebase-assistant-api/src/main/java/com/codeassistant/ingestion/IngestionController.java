package com.codeassistant.ingestion;

import com.codeassistant.model.IngestRequest;
import com.codeassistant.model.IngestResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    @PostMapping("/ingest")
    public Mono<ResponseEntity<IngestResult>> ingest(@Valid @RequestBody IngestRequest request) {
        log.info("Ingest request received: {}", request.getRepoUrl());
        return Mono.fromCallable(() -> ingestionService.ingest(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(result -> {
                    if ("SUCCESS".equals(result.getStatus())) {
                        return ResponseEntity.ok(result);
                    } else {
                        return ResponseEntity.internalServerError().body(result);
                    }
                });
    }
}
