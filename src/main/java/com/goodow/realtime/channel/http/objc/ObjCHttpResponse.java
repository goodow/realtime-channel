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

import com.goodow.realtime.channel.http.HttpResponse;

import java.io.IOException;

/**
 * Stub implementation.
 */
class ObjCHttpResponse extends HttpResponse {

  @Override
  public String getContent() {
    return null;
  }

  @Override
  public String getContentEncoding() throws IOException {
    return null;
  }

  @Override
  public long getContentLength() throws IOException {
    return 0;
  }

  @Override
  public String getContentType() throws IOException {
    return null;
  }

  @Override
  public int getHeaderCount() throws IOException {
    return 0;
  }

  @Override
  public String getHeaderName(int index) throws IOException {
    return null;
  }

  @Override
  public String getHeaderValue(int index) throws IOException {
    return null;
  }

  @Override
  public String getReasonPhrase() throws IOException {
    return null;
  }

  @Override
  public int getStatusCode() {
    return 0;
  }

  @Override
  public String getStatusLine() throws IOException {
    return null;
  }
}