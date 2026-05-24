package com.codeassistant.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserView {
    private UUID id;
    private String email;
    private String displayName;
}

