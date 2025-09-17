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
package com.hermes.security;

import java.util.Locale;

import com.hermes.data.ApprovalStatus;
import com.hermes.data.UserRole;
import com.hermes.model.User;
import com.hermes.repository.UserRepository;
import com.hermes.service.EmailNotificationService;
import com.hermes.service.impl.Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecurityHelper {

  /**
   * Save Oauth2User to the DB
   */
  public static User saveOauth2User(final EmailNotificationService emailNotificationService, 
  final UserRepository userRepository, final String registrationId, final String authId, final String email,
  final String name, final String picture, final String link, final String adminEmail) {

    User user = userRepository.findByEmail(email).block();

    if (user == null) {

      final boolean isAdmin = adminEmail.equalsIgnoreCase(email);
      user = new User()
        .setAuthId(authId)
        .setEmail(email)
        .setFullName(name)
        .setRole(isAdmin ? UserRole.ADMIN : UserRole.USER)
        .setStatus(isAdmin ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING)
        .setComment(isAdmin ? null : "I am trying to log in")
        .setPicture(picture)
        .setProvider(registrationId)
        .setCreatedOn(Utils.getTimestamp());

      user = userRepository.save(user).block();

      if (!isAdmin) { // Do not send notification email to ADMIN
        emailNotificationService.sendCreationNotification(Locale.ENGLISH, name, email);
      }
    }

    return user;
  }
}
