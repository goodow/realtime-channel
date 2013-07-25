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

import com.goodow.realtime.channel.ChannelDemuxer;
import com.goodow.realtime.channel.constant.Constants;
import com.goodow.realtime.channel.constant.Constants.Params;

import java.util.logging.Level;
import java.util.logging.Logger;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.util.Collections;
import elemental.util.MapFromStringToString;

public class PollService {
  private static final Logger log = Logger.getLogger(DeltaService.class.getName());
  private static final ChannelDemuxer demuxer = ChannelDemuxer.get();

  public void poll(JsonArray ids, String sessionId) {
    MapFromStringToString params = Collections.mapFromStringToString();
    params.put(Params.ACCESS_TOKEN, demuxer.getAccessToken());
    params.put(Params.SESSION_ID, sessionId);
    JsonObject obj = Json.createObject();
    obj.put(Params.IDS, ids);
    demuxer.getRpc().post(Constants.Services.POLL, params, obj.toJson(), new Rpc.RpcCallback() {
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
        JsonArray msgs = RpcUtil.evalPrefixed(data);
        for (int i = 0, len = msgs.length(); i < len; i++) {
          JsonObject msg = msgs.getObject(i);
          if (Params.TOKEN.equals(msg.getString(Params.ID))) {
            demuxer.connect(msg.getString(Params.TOKEN));
            continue;
          }
          demuxer.publishMessage(msg);
        }
      }
    });
  }
}
