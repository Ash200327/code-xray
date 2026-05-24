package com.codeassistant.ingestion;

import com.codeassistant.model.IngestionProgressEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IngestionProgressPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(UUID jobId, IngestionProgressEvent event) {
        messagingTemplate.convertAndSend("/topic/ingestion/jobs/" + jobId, event);
    }
}

