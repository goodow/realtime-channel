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

import com.goodow.realtime.Realtime;
import com.goodow.realtime.channel.rpc.Constants.Params;

import java.util.logging.Level;
import java.util.logging.Logger;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.util.Collections;
import elemental.util.MapFromStringToString;

/**
 * Handles getting a channel token for connecting a browser channel, and fetching missing messages.
 */
public class DeltaService {
  public interface Callback {
    void onConnectionError(Throwable e);

    void onFatalError(Throwable e);

    void onMessage(JsonObject msg);
  }

  private static final Logger log = Logger.getLogger(DeltaService.class.getName());
  private final Rpc rpc;

  public DeltaService(Rpc rpc) {
    this.rpc = rpc;
  }

  public void fetchHistories(final JsonArray ids) {
    MapFromStringToString params = Collections.mapFromStringToString();
    params.put(Constants.Params.ACCESS_TOKEN, Realtime.getToken());
    rpc.post(Constants.Services.DELTA, params, ids.toJson(), new Rpc.RpcCallback() {
      @Override
      public void onConnectionError(Throwable e) {
        log.log(Level.WARNING, "onConnectionError ", e);
      }

      @Override
      public void onFatalError(Throwable e) {
        log.log(Level.WARNING, "onFatalError ", e);
      }

      @Override
      public void onSuccess(String data) {
        JsonObject msgs = RpcUtil.evalPrefixed(data);
        // registry.getChannel().connect(msgs.getString(Params.TOKEN));
        JsonArray array = msgs.getArray(Params.CHANGES);
        for (int i = 0, len = array.length(); i < len; i++) {
          JsonObject msg = array.getObject(i);
          assert msg.hasKey(Params.REVISION) && msg.hasKey(Params.ID);
          // registry.getChannel().publishMessage(msg);
        }
      }
    });
  }

  public void fetchHistory(String key, final int startRev, final Callback callback) {
    MapFromStringToString params = Collections.mapFromStringToString();
    params.put(Constants.Params.ID, key);
    params.put(Constants.Params.ACCESS_TOKEN, Realtime.getToken());
    params.put(Constants.Params.START_REVISION, startRev + "");
    rpc.get(Constants.Services.DELTA, params, new Rpc.RpcCallback() {
      @Override
      public void onConnectionError(Throwable e) {
        callback.onConnectionError(e);
      }

      @Override
      public void onFatalError(Throwable e) {
        callback.onFatalError(e);
      }

      @Override
      public void onSuccess(String data) {
        JsonObject msg = RpcUtil.evalPrefixed(data);
        assert msg.hasKey(Params.HAS_MORE) && msg.getArray(Params.DELTAS).length() > 0;
        callback.onMessage(msg);
      }
    });
  }
}