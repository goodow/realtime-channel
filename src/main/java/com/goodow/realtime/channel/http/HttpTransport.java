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
package com.goodow.realtime.channel.http;


public abstract class HttpTransport {
  /**
   * Builds a low level HTTP request for the given HTTP method.
   * 
   * @param method HTTP method
   * @param url URL
   * @return new low level HTTP request
   * @throws IllegalArgumentException if HTTP method is not supported
   */
  public abstract HttpRequest buildRequest(String method, String url);

}
