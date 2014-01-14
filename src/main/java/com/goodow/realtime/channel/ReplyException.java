/*
 * Copyright 2014 Goodow.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.goodow.realtime.channel;

public class ReplyException extends RuntimeException {
  private static final long serialVersionUID = -4441153344646081242L;
  private final ReplyFailure failureType;
  private final int failureCode;

  public ReplyException(ReplyFailure failureType) {
    super((String) null);
    this.failureType = failureType;
    this.failureCode = -1;
  }

  public ReplyException(ReplyFailure failureType, int failureCode, String message) {
    super(message);
    this.failureType = failureType;
    this.failureCode = failureCode;
  }

  public ReplyException(ReplyFailure failureType, String message) {
    super(message);
    this.failureType = failureType;
    this.failureCode = -1;
  }

  public int failureCode() {
    return failureCode;
  }

  public ReplyFailure failureType() {
    return failureType;
  }
}