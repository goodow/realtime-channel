/*
 * Copyright 2013 Goodow.com
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
package com.goodow.realtime.channel.http.js;

import com.goodow.realtime.channel.http.HttpResponse;

import com.google.gwt.http.client.Response;

final class JsHttpResponse implements HttpResponse {

  private final Response fetchResponse;

  JsHttpResponse(Response fetchResponse) {
    this.fetchResponse = fetchResponse;
  }

  @Override
  public String getContent() {
    return fetchResponse.getText();
  }

  @Override
  public int getStatusCode() {
    return fetchResponse.getStatusCode();
  }
}
