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

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.hermes.security.CustomAuthorizationRequestResolver.REDIRECT_PARAM;

public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
    Authentication authentication) throws IOException, ServletException {

        String state = request.getParameter("state");
        if (state != null && state.contains(REDIRECT_PARAM)) {
            String redirectUri = extractRedirectUriFromState(state);
            if (redirectUri != null) {
                getRedirectStrategy().sendRedirect(request, response, redirectUri);
                return;
            }
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }

    private String extractRedirectUriFromState(String state) {
        final String redirectStr = REDIRECT_PARAM + "=";
        for (String param : state.split("&")) {
            if (param.startsWith(redirectStr)) {
                return param.substring(redirectStr.length());
            }
        }
        return null;
    }
}