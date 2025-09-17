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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Custom OidcUser Class
 * 
 * <p> This custom class act as a wrapper, holding the 
 * original OIDC information plus any additional data 
 */
public class CustomOidcUser implements OidcUser {

  private final OidcUser oidcUser;
  private final AuthenticatedUser authenticatedUser; // Use the safe DTO

  public CustomOidcUser(OidcUser oidcUser, AuthenticatedUser authenticatedUser) {
    this.oidcUser = oidcUser;
    this.authenticatedUser = authenticatedUser;
  }

  // Inject the custom data
  public AuthenticatedUser getAuthenticatedUser() {
    return authenticatedUser;
  }

  @Override
  public Map<String, Object> getClaims() {
    return oidcUser.getClaims();
  }

  @Override
  public OidcUserInfo getUserInfo() {
    return oidcUser.getUserInfo();
  }

  @Override
  public OidcIdToken getIdToken() {
    return oidcUser.getIdToken();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return oidcUser.getAttributes();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {

    final Set<GrantedAuthority> customAuthorities = new HashSet<>(oidcUser.getAuthorities());
    customAuthorities.add(new SimpleGrantedAuthority("ROLE_" + authenticatedUser.role()));
    return customAuthorities;
  }

  @Override
  public String getName() {
    return oidcUser.getName();
  }
}
