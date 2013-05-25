/*
 * Copyright 2012 Goodow.com
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
package com.goodow.realtime.channel.http.objc;

import com.goodow.realtime.channel.http.HttpRequest;
import com.goodow.realtime.channel.http.HttpRequestCallback;

import java.io.IOException;

/**
 * Stub implementation.
 */
class ObjCHttpRequest extends HttpRequest {

  @SuppressWarnings("unused")
  ObjCHttpRequest(String method, String url) {
    throw new RuntimeException("Stub!");
  }

  @Override
  public void addHeader(String name, String value) {
    throw new RuntimeException("Stub!");
  }

  @Override
  public void executeAsync(HttpRequestCallback callback) throws IOException {
    throw new RuntimeException("Stub!");
  }

}
