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

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.hermes.data.ApprovalStatus;
import com.hermes.service.EmailNotificationService;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailNotificationServiceImpl implements EmailNotificationService {
  
  @Autowired
  private MessageSource messageSource;

  @Value("${hermes.email.from}")
  private String emailFrom;

  @Value("${hermes.email.sender.name}")
  private String senderName;

  @Value("${hermes.frontend.url}")
  private String frontendUrl;

  @Autowired
  private Helpers helpers;

  public static final String NOTIFICATION_USER_CREATION_SUBJECT = "email.notification.user.creation.subject";
  public static final String NOTIFICATION_USER_CREATION_BODY = "email.notification.user.creation.body";

  public static final String NOTIFICATION_USER_APPROVED_SUBJECT = "email.notification.user.approved.subject";
  public static final String NOTIFICATION_USER_APPROVED_BODY = "email.notification.user.approved.body";

  public static final String NOTIFICATION_USER_REJECTED_SUBJECT = "email.notification.user.rejected.subject";
  public static final String NOTIFICATION_USER_REJECTED_BODY = "email.notification.user.rejected.body";

  @Override
  public void sendCreationNotification(final Locale locale, final String userName, final String email) {

    this.doSendMessage(
      emailFrom,
      List.of(email),
      locale,
      NOTIFICATION_USER_CREATION_SUBJECT,
      null,
      NOTIFICATION_USER_CREATION_BODY,
      new Object[] { userName, email, frontendUrl }
    );
  }

  @Override
  public void sendApprovalStatusNotification(final Locale locale, final String userName,
  final String email, final ApprovalStatus newStatus) {

    if (newStatus == ApprovalStatus.PENDING) {
      return;
    }

    this.doSendMessage(
      emailFrom,
      List.of(email),
      locale,
      newStatus == ApprovalStatus.REJECTED ? NOTIFICATION_USER_REJECTED_SUBJECT : NOTIFICATION_USER_APPROVED_SUBJECT,
      null,
      newStatus == ApprovalStatus.REJECTED ? NOTIFICATION_USER_REJECTED_BODY : NOTIFICATION_USER_APPROVED_BODY,
      new Object[] { userName, frontendUrl }
    );
  }

  private void doSendMessage(final String from, @NonNull final List<String> to, final Locale locale,
  final String subjectPattern, @Nullable Object[] subjectArgs, final String bodyPattern,
  @Nullable Object[] bodyArgs) {

    try {
      final String subject = messageSource.getMessage(subjectPattern, subjectArgs, locale),
      text = messageSource.getMessage(bodyPattern, bodyArgs, locale);

      helpers.sendEmailMessage(
        from,
        senderName,
        to,
        subject,
        text,
        null
      );
    }
    catch (Exception ex) {
      log.error(ex.getMessage(), ex);
      throw new IllegalArgumentException("An error occurred while sending the email. Please try again!");
    }
  }
}
