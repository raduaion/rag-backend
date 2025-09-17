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
package com.hermes.model;

import org.springframework.data.annotation.CreatedDate;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import com.hermes.data.ApprovalStatus;
import com.hermes.data.UserRole;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Document(collectionName = "userapproval")
public class User {

  @DocumentId
  private String id;

  private String authId;

  private String email;

  private ApprovalStatus status;

  private UserRole role;

  private String comment;

  private String fullName;

  private String link;

  private String picture;

  private String provider;

  @CreatedDate
  private long createdOn;

  private long updatedOn;
}
