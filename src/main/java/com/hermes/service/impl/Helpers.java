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
package com.hermes.service.impl;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Helpers class
 */
@Service
@Slf4j
public class Helpers {

  @Value("${email-service.url.base}")
  private String emailServiceBaseURL;

  @Value("${email-service.client.id}")
  private String emailServiceClientId;

  @Value("${email-service.client.secret}")
  private String emailServiceClientSecret;

  @Value("${auth-server.url.base}")
  private String authServerUrl;

  private static String accessToken;

  private static long tokenExpirationTime;

  /**
   * Send Emails gateway
   * @Async
   */
  public void sendEmailMessage(@NonNull final String sender, final String senderName,
  @NonNull final List<String> recipients, @NonNull final String subject,
  @NonNull final String content, final List<MultipartFile> attachments) {

    final MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
    map.add("sender", sender);
    map.add("senderName", senderName);
    recipients.stream().forEach(recipient -> map.add("recipients", recipient));
    map.add("subject", subject);
    map.add("content", content);
    map.add("attachments", attachments);

    final String auth = emailServiceClientId + ":" + emailServiceClientSecret,
    encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes()),
    authHeader = "Basic " + encodedAuth;

    final WebClient tokenClient = WebClient.builder()
      .baseUrl(authServerUrl + "/oauth/token")
      .defaultHeader(HttpHeaders.AUTHORIZATION, authHeader)
      .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
      .build(),

    emailClient = WebClient.builder()
      .baseUrl(String.format("%s/%s", emailServiceBaseURL, "send-immediately"))
      .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
      .build();

    final Mono<String> getEmailServiceResponse = getTargetServiceData(emailClient, tokenClient, map);

    getEmailServiceResponse.subscribe(
      response -> log.info("Email Service Response: {}", response),
      error -> log.error("Email Service error: {}", error.getMessage())
    );
  }

  /**
   * Check/Get Access Token before query Email-Service
   */
  private Mono<String> getTargetServiceData(final WebClient targetClient, final WebClient tokenClient, final MultiValueMap<String, Object> body) {

    if (accessToken == null || System.currentTimeMillis() > tokenExpirationTime) {
      return refreshAccessToken(tokenClient)
        .flatMap(token -> callEmailService(targetClient, token, body));
    }
    else {
      return callEmailService(targetClient, accessToken, body)
        .onErrorResume(e -> {
          if (e instanceof WebClientResponseException && ((WebClientResponseException) e).getStatusCode().value() == 401) {
            return refreshAccessToken(tokenClient)
              .flatMap(token -> callEmailService(targetClient, token, body));
          }
          return Mono.error(e);
        });
    }
  }

  /**
   * Get a new Token from auth-server
   */
  private Mono<String> refreshAccessToken(final WebClient tokenClient) {
    return tokenClient
      .post()
      .body(BodyInserters
        .fromFormData("grant_type", "client_credentials")
        .with("scope", "read")
      )
      .retrieve()
      .bodyToMono(Map.class)
      .map(response -> {

        accessToken = (String) response.get("access_token");
        final long expiresIn = ((Number) response.get("expires_in")).longValue();
        tokenExpirationTime = System.currentTimeMillis() + ((expiresIn - 15) * 1000);

        return accessToken;
      });
  }


  /**
   * Send email to Email-Service
   */
  private Mono<String> callEmailService(final WebClient targetClient, final String token, final MultiValueMap<String, Object> body) {
    return targetClient
      .post()
      .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
      .bodyValue(body)
      .retrieve()
      .bodyToMono(String.class);
  }
}
