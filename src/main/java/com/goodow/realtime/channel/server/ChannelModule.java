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

import com.goodow.realtime.channel.Bus;

import com.alienos.guice.VertxModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Container;

public class ChannelModule extends AbstractModule implements VertxModule {
  private Vertx vertx;

  @Override
  public void setContainer(Container container) {
  }

  @Override
  public void setVertx(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  protected void configure() {
  }

  @Provides
  @Singleton
  Bus provideBus() {
    return new VertxBus(vertx.eventBus());
  }

  @Provides
  @Singleton
  RouteMatcher provideRouteMatcher() {
    return new RouteMatcher();
  }
}