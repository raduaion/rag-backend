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

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    public static final String REDIRECT_PARAM = "redirect_param";

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

    public CustomAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository,
            OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
        );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        // printData(request);
        OAuth2AuthorizationRequest originalRequest = defaultResolver.resolve(request);
        if (originalRequest != null) {
            return customizeAuthorizationRequest(request, originalRequest);
        }
        return null;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        // printData(request);
        OAuth2AuthorizationRequest originalRequest = defaultResolver.resolve(request, clientRegistrationId);
        if (originalRequest != null) {
            return customizeAuthorizationRequest(request, originalRequest);
        }
        return null;
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(HttpServletRequest request, OAuth2AuthorizationRequest authorizationRequest) {
        // Extract redirect_uri from the request parameters
        String customRedirectUri = request.getParameter("requester");

        if (customRedirectUri != null && !customRedirectUri.isEmpty()) {
            return OAuth2AuthorizationRequest.from(authorizationRequest)
                .state(authorizationRequest.getState() + "&" + REDIRECT_PARAM + "=" + customRedirectUri) // Append to state
                .build();
        }

        return authorizationRequest;
    }

    public String getFullURL(HttpServletRequest request) {
        StringBuilder requestURL = new StringBuilder(request.getRequestURL().toString());
        String queryString = request.getQueryString();

        if (queryString != null) {
            requestURL.append("?").append(queryString);
        }

        return requestURL.toString();
    }

    public void printData(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String sessionStr = session == null ? "No session" : "Session ID: " + session.getId();
        System.out.println(request.getMethod() + " " + getFullURL(request) + " " + sessionStr);
    }
}

