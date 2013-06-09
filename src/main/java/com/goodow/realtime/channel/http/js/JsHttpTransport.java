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
package com.goodow.realtime.channel.http.js;

import com.goodow.realtime.channel.http.HttpRequest;
import com.goodow.realtime.channel.http.HttpTransport;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestBuilder.Method;

public final class JsHttpTransport extends HttpTransport {

  @Override
  public HttpRequest buildRequest(String method, String url) {
    final Method httpMethod;
    if (method.equals(RequestBuilder.DELETE.toString())) {
      httpMethod = RequestBuilder.DELETE;
    } else if (method.equals(RequestBuilder.GET.toString())) {
      httpMethod = RequestBuilder.GET;
    } else if (method.equals(RequestBuilder.HEAD.toString())) {
      httpMethod = RequestBuilder.HEAD;
    } else if (method.equals(RequestBuilder.POST.toString())) {
      httpMethod = RequestBuilder.POST;
    } else {
      httpMethod = RequestBuilder.PUT;
    }
    return new JsHttpRequest(httpMethod, HttpTransport.ROOT + url);
  }
}
