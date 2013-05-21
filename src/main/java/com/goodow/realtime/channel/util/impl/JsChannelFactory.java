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
import com.goodow.realtime.channel.http.HttpTransport;
import com.goodow.realtime.channel.http.js.JsHttpTransport;
import com.goodow.realtime.channel.util.ChannelFactory;

import com.google.gwt.http.client.URL;

public class JsChannelFactory implements ChannelFactory {

  @Override
  // @formatter:off
  public final native Channel createChannel(String token) /*-{
    return new $wnd.goog.appengine.Channel(token);        
  }-*/;
  // @formatter:on

  @Override
  public String escapeUriQuery(String value) {
    return URL.encodeQueryString(value);
  }

  @Override
  public HttpTransport getHttpTransport() {
    return new JsHttpTransport();
  }
}
