package com.textile.manufacturing.common.security;

import java.util.Optional;
import java.util.UUID;

public interface AuthenticatedUserProvider {

    AuthenticatedUser loadActiveUser(UUID userId);
}
