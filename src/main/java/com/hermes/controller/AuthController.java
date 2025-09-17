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

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import com.hermes.data.AuthenticatedUser;
import com.hermes.data.CustomOidcUser;
import com.hermes.exceptions.NotAuthorizedException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@Slf4j
class AuthController {

  @GetMapping("/info")
  public Mono<AuthenticatedUser> getUserInfo(@AuthenticationPrincipal final OidcUser principal) throws NotAuthorizedException {

    if (principal instanceof CustomOidcUser customOidcUser) {
      return Mono.just(customOidcUser.getAuthenticatedUser());
    }

    return Mono.error(new  NotAuthorizedException("User not authenticated!"));
  }
}
