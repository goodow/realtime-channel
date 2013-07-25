/*
 * Copyright 2012 Goodow.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.goodow.realtime.channel.constant;

public enum Platform {
  WEB('w'), ANDROID('a'), IOS('i');
  public static Platform fromPrefix(char prefix) {
    for (Platform platform : Platform.values()) {
      if (platform.prefix == prefix) {
        return platform;
      }
    }
    return null;
  }

  public static Platform fromUserAgent(String userAgent) {
    if (userAgent == null) {
      return WEB;
    }
    userAgent = userAgent.toLowerCase();
    if (userAgent.contains("android")) {
      return ANDROID;
    } else if (userAgent.contains("iphone")) {
      return IOS;
    } else {
      return WEB;
    }
  }

  private final char prefix;

  private Platform(char prefix) {
    this.prefix = prefix;
  }

  public char prefix() {
    return prefix;
  }
}