package com.curleesoft.pickem.backend.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextAuditActorResolver implements AuditActorResolver {

  @Override
  public String currentActor() {
    Authentication authentication = SecurityContextHolder.getContext()
      .getAuthentication();
    if (
      authentication == null ||
      !authentication.isAuthenticated() ||
      authentication.getName() == null ||
      authentication.getName().isBlank() ||
      "anonymousUser".equals(authentication.getName())
    ) {
      return SYSTEM_ACTOR;
    }
    return authentication.getName();
  }
}
