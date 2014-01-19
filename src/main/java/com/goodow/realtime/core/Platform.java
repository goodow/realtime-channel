/*
 * Copyright 2013 Goodow.com
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
package com.goodow.realtime.core;

/**
 * The main Platform interface. The static methods in this class provide access to the various
 * available subsystems.
 * 
 * <p>
 * You must register a {@link Platform} before calling any of these methods. For example,
 * <code>JavaPlatform.register();</code>.
 * </p>
 */
public class Platform {
  public enum Type {
    JAVA, HTML, ANDROID, IOS, FLASH, STUB
  }

  private static PlatformFactory FACTORY;

  /**
   * Cancel the timer with the specified {@code id}. Returns {@code} true if the timer was
   * successfully cancelled, or {@code false} if the timer does not exist.
   */
  public static boolean cancelTimer(int id) {
    return get().cancelTimer(id);
  }

  public static void handle(Object handler, Object event) {
    get().handle(handler, event);
  }

  public static Net net() {
    return get().net();
  }

  /**
   * A deferred command is executed after the event loop returns.
   */
  public static void scheduleDeferred(Handler<Void> handler) {
    get().scheduleDeferred(handler);
  }

  /**
   * Configures the current {@link Platform}. Do not call this directly unless you're implementing a
   * new platform.
   */
  public static void setFactory(PlatformFactory factory) {
    FACTORY = factory;
  }

  /**
   * Schedules a repeating handler that is scheduled with a constant periodicity. That is, the
   * handler will be invoked every <code>delayMs</code> milliseconds, regardless of how long the
   * previous invocation took to complete.
   * 
   * @param delayMs the period with which the handler is executed
   * @param handler the handler to execute
   * 
   * @return the unique ID of the timer
   */
  public static int setPeriodic(int delayMs, Handler<Void> handler) {
    return get().setPeriodic(delayMs, handler);
  }

  public static Platform.Type type() {
    return get().type();
  }

  private static PlatformFactory get() {
    assert FACTORY != null : "You must register a platform first by invoke {Java|Android}Platform.register()";
    return FACTORY;
  }

  // Non-instantiable
  protected Platform() {
  }
}
