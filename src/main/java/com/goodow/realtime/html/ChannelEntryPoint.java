/*
 * Copyright 2014 Goodow.com
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
package com.goodow.realtime.html;

import com.goodow.realtime.html.ChannelOverlay.HandlerRegistrationOverlay;
import com.goodow.realtime.html.ChannelOverlay.MessageOverlay;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.shared.GWT;

class ChannelEntryPoint implements EntryPoint {

  @Override
  public void onModuleLoad() {
    HtmlPlatform.register();
    GWT.create(HandlerRegistrationOverlay.class);
    GWT.create(MessageOverlay.class);
    GWT.create(ChannelOverlay.class);
    // GWT.create(MessageHandlerOverlay.class);
    __jsniOnLoad__();
  }

  // @formatter:off
  private native void __jsniOnLoad__() /*-{
    $wnd.gdc = $wnd.gdc || $wnd.good.channel;
  }-*/;
  // @formatter:on
}
