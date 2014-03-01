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
package com.goodow.realtime.channel.server;

import com.alienos.guice.GuiceVerticleHelper;
import com.alienos.guice.GuiceVertxBinding;
import com.google.inject.Inject;

import org.vertx.java.core.http.RouteMatcher;
import org.vertx.mods.web.WebServer;

@GuiceVertxBinding(modules = {ChannelModule.class})
public class ChannelWebServer extends WebServer {
  @Inject protected RouteMatcher routeMatcher;

  @Override
  public void start() {
    GuiceVerticleHelper.inject(this, vertx, container);
    super.start();
  }

  @Override
  protected RouteMatcher routeMatcher() {
    routeMatcher.noMatch(staticHandler());
    return routeMatcher;
  }
}
