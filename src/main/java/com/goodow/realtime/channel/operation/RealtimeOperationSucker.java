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
package com.goodow.realtime.channel.operation;

import com.goodow.realtime.channel.PollChannel;
import com.goodow.realtime.channel.RealtimeChannelDemuxer;
import com.goodow.realtime.channel.operation.GenericOperationChannel.ReceiveOpChannel;
import com.goodow.realtime.channel.rpc.Rpc;
import com.goodow.realtime.channel.rpc.SaveService;
import com.goodow.realtime.channel.rpc.SnapshotService;
import com.goodow.realtime.operation.OperationSink;
import com.goodow.realtime.operation.RealtimeOperation;
import com.goodow.realtime.operation.RealtimeTransformer;

import java.util.logging.Level;
import java.util.logging.Logger;

import elemental.json.JsonValue;

public class RealtimeOperationSucker implements
    GenericOperationChannel.Listener<RealtimeOperation<?>> {
  public interface Listener extends OperationSink<RealtimeOperation<?>> {
    void onSaveStateChanged(boolean isSaving, boolean isPending);
  }

  private static final Logger logger = Logger.getLogger(RealtimeOperationSucker.class.getName());

  private static final RealtimeChannelDemuxer demuxer = RealtimeChannelDemuxer.get();
  private final String id;
  private final GenericOperationChannel<RealtimeOperation<?>> channel;
  private final Rpc rpc;
  private final RealtimeTransformer transformer;
  private final OperationSink<RealtimeOperation<?>> outputSink;
  private final ReceiveOpChannel<RealtimeOperation<?>> receiveChannel;
  private Listener bridge;

  public RealtimeOperationSucker(final String id) {
    this.id = id;
    this.rpc = demuxer.getRpc();
    transformer = new RealtimeTransformer();
    receiveChannel = new ReceiveOpChannelImpl<RealtimeOperation<?>>(id, rpc, transformer);
    TransformQueue<RealtimeOperation<?>> queue =
        new TransformQueue<RealtimeOperation<?>>(transformer);
    SaveService<RealtimeOperation<?>> saveService =
        new SaveService<RealtimeOperation<?>>(rpc, id, demuxer.getAccessToken());
    channel =
        new GenericOperationChannel<RealtimeOperation<?>>(queue, receiveChannel, saveService, this);
    outputSink = new OperationSink<RealtimeOperation<?>>() {
      @Override
      public void consume(RealtimeOperation<?> op) {
        channel.send(op);
      }
    };
  }

  public OperationSink<RealtimeOperation<?>> getOutputSink() {
    return outputSink;
  }

  public void load(final Listener listener, final SnapshotService.Callback callback) {
    SnapshotService snapshotService = new SnapshotService(rpc);
    snapshotService.load(id, demuxer.getAccessToken(), true, new SnapshotService.Callback() {
      @Override
      public void onSuccess(JsonValue snapshot, String sessionId, int revision) {
        bridge = listener;
        callback.onSuccess(snapshot, sessionId, revision);
        demuxer.register(id, bridge, (ReceiveOpChannelImpl<?>) receiveChannel);
        channel.connect(revision, sessionId);
        PollChannel.get().connect(sessionId);
      }
    });
  }

  @Override
  public void onAck(RealtimeOperation<?> serverHistoryOp, boolean clean) {
  }

  @Override
  public void onError(Throwable e) {
    logger.log(Level.WARNING, "Unable to save", e);
  }

  @Override
  public void onRemoteOp(RealtimeOperation<?> serverHistoryOp) {
    while (channel.peek() != null) {
      bridge.consume(channel.receive());
    }
  }

  @Override
  public void onSaveStateChanged(boolean isSaving, boolean isPending) {
    bridge.onSaveStateChanged(isSaving, isPending);
  }
}