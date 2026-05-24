package com.codeassistant.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecurityMode {

    @Value("${codeassistant.security.enabled:false}")
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }
}

