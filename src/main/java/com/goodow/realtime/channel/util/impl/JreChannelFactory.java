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
package com.goodow.realtime.channel.util.impl;

import com.goodow.realtime.channel.Channel;
import com.goodow.realtime.channel.Socket;
import com.goodow.realtime.channel.SocketListener;
import com.goodow.realtime.channel.http.HttpTransport;
import com.goodow.realtime.channel.util.ChannelFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class JreChannelFactory implements ChannelFactory {

  @Override
  public Channel createChannel(String token) {
    return new Channel() {
      @Override
      public Socket open(SocketListener listener) {
        return null;
      }
    };
  }

  @Override
  public String escapeUriQuery(String value) {
    try {
      return URLEncoder.encode(value, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public HttpTransport getHttpTransport() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void scheduleDeferred(Runnable cmd) {
    cmd.run();
  }

  @Override
  public void scheduleFixedDelay(Runnable cmd, int delayMs) {
    // cmd.run();
  }
}
