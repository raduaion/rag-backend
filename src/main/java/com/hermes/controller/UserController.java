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
package com.hermes.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import com.hermes.data.ApprovalStatus;
import com.hermes.exceptions.NotAuthorizedException;
import com.hermes.model.UserApproval;
import com.hermes.repository.UserApprovalRepository;
import com.hermes.service.AuthenticationFacade;
import com.hermes.service.EmailNotificationService;
import com.hermes.service.impl.Utils;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
class UserController {

  @Autowired
  private UserApprovalRepository userApprovalRepository;

  @Autowired
  private AuthenticationFacade authFacade;

  @Autowired
  private EmailNotificationService emailNotificationService;

  private static final Logger log = LoggerFactory.getLogger(UserController.class);

  @GetMapping("/info")
  public Map<String, Object> getUserInfo(final Principal principal) throws NotAuthorizedException{

    if(principal != null) {
        // System.out.println("Principal type: " + principal.getClass().getName());
        // System.out.println("Attributes: " + ((OAuth2AuthenticationToken) principal).getPrincipal().getAttributes());
    }

    if (principal instanceof OAuth2AuthenticationToken authToken) {

      final Map<String, Object> details = new HashMap<>();
      final OAuth2User user = authToken.getPrincipal();

      details.put("email", user.getAttribute("email"));
      details.put("name", user.getAttribute("name"));
      details.put("picture", user.getAttribute("picture"));
      details.put("link", user.getAttribute("link"));

      if (details.get("email") != null) {
        details.put("status", getUserStatus((String) details.get("email"), (String) details.get("name")));
        return details;
      }
      throw new NotAuthorizedException("Email not found in authentication token");
    }

    throw new NotAuthorizedException("User not logged in");
  }

  private ApprovalStatus getUserStatus(@NonNull final String email, final String username) {

    if (authFacade.isAdmin()) {
      return ApprovalStatus.APPROVED;
    }

    try {

      UserApproval userApproval = userApprovalRepository.findByEmail(email).block();
      if (userApproval == null) {

        userApproval = new UserApproval()
          .setEmail(email)
          .setStatus(ApprovalStatus.PENDING)
          .setComment("I am trying to log in")
          .setCreatedOn(Utils.getTimestamp());

        final Mono<UserApproval> result = userApprovalRepository
          .save(userApproval)
          .doOnNext(user -> emailNotificationService.sendCreationNotification(Locale.ENGLISH, username, email));

        result.subscribe(
          response -> log.info("UserApprovalRepository Response: {}", response),
          error -> log.error("UserApprovalRepository Error: {}", error.getMessage())
        );
      }

      return userApproval.getStatus();
    }
    catch (Exception e) {
      log.error("getUserStatus: {}", e.getMessage());
      return ApprovalStatus.PENDING;
    }
  }
}
