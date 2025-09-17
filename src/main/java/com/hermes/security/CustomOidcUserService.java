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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.hermes.data.AuthenticatedUser;
import com.hermes.data.CustomOidcUser;
import com.hermes.model.User;
import com.hermes.repository.UserRepository;
import com.hermes.service.EmailNotificationService;

import lombok.extern.slf4j.Slf4j;

/**
 * This class will be responsible for fetching the user's data from the social
 * provider (Google) and saving it in UserRepository 
 */
@Service
@Slf4j
public class CustomOidcUserService extends OidcUserService {

  @Autowired
  private EmailNotificationService emailNotificationService;

  @Autowired
  private UserRepository userRepository;

  @Value("${hermes.users.admin.email}")
  private String adminEmail;

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

    final OidcUser oidcUser = super.loadUser(userRequest);

    final String registrationId = userRequest.getClientRegistration().getRegistrationId(),

    authId = oidcUser.getName(),
    email = oidcUser.getEmail(),
    name = oidcUser.getFullName(),
    picture = oidcUser.getPicture(),
    link = oidcUser.getProfile();

    final User user = SecurityHelper.saveOauth2User(
      emailNotificationService, userRepository,
      registrationId, authId, email, name, picture, link, adminEmail
    );

    return new CustomOidcUser(
      oidcUser,
      new AuthenticatedUser(authId, user)
    );
  }
}
