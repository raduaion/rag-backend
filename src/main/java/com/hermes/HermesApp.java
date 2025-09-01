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
package com.hermes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.SecurityFilterChain;

import com.hermes.security.CustomAuthorizationRequestResolver;
import com.hermes.security.CustomLogoutSuccessHandler;
import com.hermes.security.CustomAuthenticationSuccessHandler;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@SpringBootApplication
public class HermesApp {
    public static void main(String[] args) {
        SpringApplication.run(HermesApp.class, args);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
        .csrf( csrf -> csrf.disable())        // Disable CSRF for simplicity when using CORS (enable if properly configured)
        .cors(Customizer.withDefaults()) // Enable CORS with default settings (reads from WebMvcConfigurer)
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/api/users/**", "/error/**").permitAll()
            .anyRequest().authenticated())
        .logout(logout -> logout
            .logoutUrl("/api/users/logout") // Endpoint to trigger logout
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
            .successHandler(new CustomAuthenticationSuccessHandler())
        );

        return http.build();
    }
}