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

import com.goodow.realtime.channel.http.HttpResponse;

import com.google.gwt.http.client.Header;
import com.google.gwt.http.client.Response;

import java.util.ArrayList;

final class JsHttpResponse extends HttpResponse {

  private final ArrayList<String> headerNames = new ArrayList<String>();
  private final ArrayList<String> headerValues = new ArrayList<String>();
  private final Response fetchResponse;
  private String contentEncoding;
  private String contentType;
  private long contentLength;

  JsHttpResponse(Response fetchResponse) {
    this.fetchResponse = fetchResponse;
    for (Header header : fetchResponse.getHeaders()) {
      String name = header.getName();
      String value = header.getValue();
      if (name != null && value != null) {
        headerNames.add(name);
        headerValues.add(value);
        if ("content-type".equalsIgnoreCase(name)) {
          contentType = value;
        } else if ("content-encoding".equalsIgnoreCase(name)) {
          contentEncoding = value;
        } else if ("content-length".equalsIgnoreCase(name)) {
          try {
            contentLength = Long.parseLong(value);
          } catch (NumberFormatException e) {
            // ignore
          }
        }
      }
    }
  }

  @Override
  public String getContent() {
    return fetchResponse.getText();
  }

  @Override
  public String getContentEncoding() {
    return contentEncoding;
  }

  @Override
  public long getContentLength() {
    return contentLength;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public int getHeaderCount() {
    return headerNames.size();
  }

  @Override
  public String getHeaderName(int index) {
    return headerNames.get(index);
  }

  @Override
  public String getHeaderValue(int index) {
    return headerValues.get(index);
  }

  @Override
  public String getReasonPhrase() {
    // unfortunately not available
    return null;
  }

  @Override
  public int getStatusCode() {
    return fetchResponse.getStatusCode();
  }

  @Override
  public String getStatusLine() {
    // unfortunately not available
    return null;
  }
}
