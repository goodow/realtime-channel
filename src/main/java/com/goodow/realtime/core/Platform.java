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
 * Generic platform interface. New platforms are defined as implementations of this interface.
 */
public abstract class Platform {
  public enum Type {
    JAVA, HTML, ANDROID, IOS, FLASH, STUB
  }

  private static Platform platform;

  public static Platform get() {
    assert platform != null : "You must register a platform first by invoke {Java|Android}Platform.register()";
    return platform;
  }

  /**
   * Configures the current {@link Platform}. Do not call this directly unless you're implementing a
   * new platform.
   */
  public static void setPlatform(Platform platform) {
    Platform.platform = platform;
  }

  // Non-instantiable
  protected Platform() {
  }

  /**
   * Cancel the timer with the specified {@code id}. Returns {@code} true if the timer was
   * successfully cancelled, or {@code false} if the timer does not exist.
   */
  public abstract boolean cancelTimer(int id);

  public abstract Net net();

  /**
   * A deferred command is executed after the event loop returns.
   */
  public abstract void scheduleDeferred(VoidHandler handler);

  /**
   * Set a periodic timer to fire every {@code delayMs} milliseconds, at which point {@code handler}
   * will be called with the id of the timer.
   * 
   * @return the unique ID of the timer
   */
  public abstract int setPeriodic(int delayMs, VoidHandler handler);

  public abstract Platform.Type type();
}
