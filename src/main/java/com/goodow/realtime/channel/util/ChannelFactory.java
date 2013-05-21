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
package com.goodow.realtime.channel.util;

import com.goodow.realtime.channel.Channel;
import com.goodow.realtime.channel.http.HttpTransport;

public interface ChannelFactory {
  /**
   * Creates a new {@code Channel} object with the given token. The token must be a valid Channel
   * API token created by App Engine's channel service.
   * 
   * @param token
   * @return a new {@code Channel}
   */
  Channel createChannel(String token);

  String escapeUriQuery(String value);

  HttpTransport getHttpTransport();
}
