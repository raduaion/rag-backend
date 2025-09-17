/*
 * Copyright 2025 Aion Sigma Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hermes.data;

import com.hermes.model.User;

/**
 * Authenticated User data
 */
public record AuthenticatedUser (
  String authId,
  String id,
  String email,
  ApprovalStatus status,
  UserRole role,
  String comment,
  String fullName,
  String link,
  String picture,
  String provider,
  long createdOn,
  long updatedOn
) {

  public AuthenticatedUser(final String authID, final User user) {

    this(authID, user.getId(), user.getEmail(), user.getStatus(),
      user.getRole(), user.getComment(), user.getFullName(),
      user.getLink(), user.getPicture(), user.getProvider(),
      user.getCreatedOn(), user.getUpdatedOn()
    );
  }
}
