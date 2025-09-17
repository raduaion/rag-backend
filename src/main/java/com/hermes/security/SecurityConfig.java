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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

  @Autowired
  private CustomOidcUserService customOidcUserService;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository)
  throws Exception {

    http
    .csrf( csrf -> csrf.disable()) // Disable CSRF for simplicity when using CORS (enable if properly configured)
    .cors(Customizer.withDefaults()) // Enable CORS with default settings (reads from WebMvcConfigurer)

    .authorizeHttpRequests(authorize -> authorize
      .requestMatchers("/api/auth/info", "/error/**").permitAll()
      .requestMatchers("/api/users/**").hasRole("ADMIN")
      .anyRequest().authenticated())

    .logout(logout -> logout
      .logoutUrl("/api/auth/logout") // Endpoint to trigger logout
      .logoutSuccessHandler(new CustomLogoutSuccessHandler())
      .invalidateHttpSession(true) // Invalidate session
      .clearAuthentication(true) // Clear authentication
      .deleteCookies("JSESSIONID") // Delete cookies
    )

    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

    .oauth2Login(oauth2 -> oauth2
      .authorizationEndpoint(authEndpoint ->
        authEndpoint.authorizationRequestResolver(
          new CustomAuthorizationRequestResolver(clientRegistrationRepository)
        )
      )
      .userInfoEndpoint(userInfo -> userInfo
        // .userService(customOAuth2UserService) // For standard OAuth2 providers (GitHub, Facebook)
        .oidcUserService(customOidcUserService) // For OIDC providers (Google)
      )
      .successHandler(new CustomAuthenticationSuccessHandler())
    );

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }
}
