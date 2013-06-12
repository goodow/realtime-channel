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

import com.google.gwt.core.client.JavaScriptObject;

public class JsXDomainRequest extends JavaScriptObject {
  interface JsXDomainRequestHandler {
    void onError(JsXDomainRequest req);

    void onLoad(JsXDomainRequest req);

    void onProgress(JsXDomainRequest req);

    void onTimeout(JsXDomainRequest req);
  }

  // @formatter:off
  public static native JsXDomainRequest create() /*-{
    if ($wnd.XDomainRequest) {
      return new XDomainRequest();
    } else {
      $wnd.alert("Cross site requests not supported in this browser");
      return null;
    }
  }-*/;

  protected JsXDomainRequest() {
  }

  public final native void abort() /*-{
    this.abort();
  }-*/;

  public final native void clear() /*-{
    var self = this;
    $wnd.setTimeout(function() {
      self.onload= new Function();
      self.onprogress= new Function();
      self.onerror= new Function();
      self.ontimeout= new Function();
    }, 0);
  }-*/;

  public final native String getContentType() /*-{
    return this.contentType;
  }-*/;

  public final native String getResponseText() /*-{
    return this.responseText;
  }-*/;

  public final native int getTimeout() /*-{
    return this.timeout;
  }-*/;

  public final native void open(String httpMethod, String url) /*-{
    this.open(httpMethod, url);
  }-*/;

  public final native void send(String requestData) /*-{
    this.send(requestData);
  }-*/;

  public final native void setHandler(JsXDomainRequestHandler handler) /*-{
    var _this = this;
    this.onload = $entry(function() {
      handler.@com.goodow.realtime.channel.http.js.JsXDomainRequest.JsXDomainRequestHandler::onLoad(Lcom/goodow/realtime/channel/http/js/JsXDomainRequest;)(_this);
    });
    this.onerror = $entry(function() {
      handler.@com.goodow.realtime.channel.http.js.JsXDomainRequest.JsXDomainRequestHandler::onError(Lcom/goodow/realtime/channel/http/js/JsXDomainRequest;)(_this);
    });
    this.ontimeout = $entry(function() {
      handler.@com.goodow.realtime.channel.http.js.JsXDomainRequest.JsXDomainRequestHandler::onTimeout(Lcom/goodow/realtime/channel/http/js/JsXDomainRequest;)(_this);
    });
    this.onprogress = $entry(function() {
      handler.@com.goodow.realtime.channel.http.js.JsXDomainRequest.JsXDomainRequestHandler::onProgress(Lcom/goodow/realtime/channel/http/js/JsXDomainRequest;)(_this);
    });
  }-*/;

  public final native void setTimeout(int value) /*-{
    this.tmeout=value;
  }-*/;
}