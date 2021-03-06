package com.ft.methodearticlemapper.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
  private static final long serialVersionUID = -8165313320588116117L;
  private final UUID uuid;

  public ResourceNotFoundException(UUID uuid) {
    super("methode file not found for uuid " + uuid);
    this.uuid = uuid;
  }

  public UUID getUuid() {
    return uuid;
  }
}
