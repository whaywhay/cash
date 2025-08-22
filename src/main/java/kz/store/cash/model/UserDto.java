package kz.store.cash.model;

import kz.store.cash.model.enums.UserRole;

public record UserDto(
    Long id,
    String username,
    String password,
    String displayName,
    UserRole role,
    Boolean active
) {

}
