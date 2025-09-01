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

import com.google.cloud.spring.data.firestore.Document;

import lombok.Data;
import lombok.experimental.Accessors;

import com.google.cloud.firestore.annotation.DocumentId;
import java.util.Map;

@Data
@Accessors(chain = true)
@Document(collectionName = "indexes")
public class Index {

  @DocumentId
  private String id; // Firestore requires this annotation for the ID field.

  private String name;

  private String lowercaseName; // For case-insensitive check for duplicate names

  private Map<String, String> files; // Key: File ID, Value: Checksum

  private String createdBy;

  private Long createdAt;

  private Long updatedAt;

  private boolean shared;
}
