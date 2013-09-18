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

import com.goodow.realtime.channel.http.HttpRequest;
import com.goodow.realtime.channel.http.HttpRequestCallback;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestBuilder.Method;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;

import elemental.client.Browser;

final class JsHttpRequest implements HttpRequest {

  private final RequestBuilder request;

  JsHttpRequest(Method method, String url) {
    String userAgent = Browser.getWindow().getNavigator().getUserAgent().toLowerCase();
    if (userAgent.indexOf("msie") != -1) {
      request = new JsXDomainRequestBuilder(method, url);
    } else {
      request = new RequestBuilder(method, url);
    }
  }

  @Override
  public void executeAsync(final HttpRequestCallback callback, String content) {
    // write content
    if (content != null) {
      request.setRequestData(content);
    }
    // connect
    request.setCallback(new RequestCallback() {
      @Override
      public void onError(Request request, Throwable exception) {
        callback.onFailure(exception);
      }

      @Override
      public void onResponseReceived(Request request, Response response) {
        callback.onResponse(new JsHttpResponse(response));
      }
    });
    try {
      request.send();
    } catch (RequestException e) {
      callback.onFailure(e);
    }
  }
}
