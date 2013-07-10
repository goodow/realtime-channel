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
package com.goodow.realtime.channel.rpc;

/**
 * Shared constants between server and client.
 */
public class Constants {

  /**
   * Request parameter keys for referencing various values.
   */
  public static class Params {
    public static final String ID = "id";
    public static final String IDS = "ids";
    public static final String SNAPSHOT = "snapshot";
    public static final String CHANGES = "changes";
    public static final String DELTAS = "mu";
    public static final String SESSION_ID = "sid";

    public static final String ACCESS_TOKEN = "access_token";
    public static final String AUTO_CREATE = "auto_create";
    public static final String TOKEN = "token";
    public static final String HAS_MORE = "more";
    public static final String REVISION = "revision";
    public static final String START_REVISION = "startRev";
    public static final String END_REVISION = "endRev";

    private Params() {
    }
  }

  /** Service names. */
  public static class Services {
    public static final String SERVICE = "otservice/";
    public static final String SNAPSHOT = SERVICE + "gs";
    public static final String DELTA = SERVICE + "delta";
    public static final String REVISION = SERVICE + "revision";
    public static final String SAVE = SERVICE + "save";
    public static final String POLL = SERVICE + "poll";
    private static final String PRESENCE = SERVICE + "presence/";
    public static final String PRESENCE_CONNECT = PRESENCE + "connect/";
    public static final String PRESENCE_DISCONNECT = PRESENCE + "disconnect/";

    private Services() {
    }
  }

  /**
   * Prefix added to data returned from XHRs to guard against XSSI attacks.
   * 
   * See http://google-gruyere.appspot.com/part4
   */
  public static final String XSSI_PREFIX = ")]}'\n";
  public static final char WEB = 'w';
  public static final char ANDROID = 'a';
  public static final char IOS = 'i';
  public static final int SESSION_LENGTH = 15;

  private Constants() {
  }
}
