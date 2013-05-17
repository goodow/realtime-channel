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
package com.goodow.realtime.channel;

import com.goodow.realtime.channel.util.FuzzingBackOffGenerator;
import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.util.NativeInterface;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;

import java.util.logging.Level;
import java.util.logging.Logger;

import elemental.util.ArrayOf;
import elemental.util.ArrayOfInt;
import elemental.util.Collections;

/**
 * Service that handles transportation and transforming of client and server operations.
 * 
 * @param <O> Mutation type.
 */
public class GenericOperationChannel<O extends Operation<?>> {

  /**
   * Notifies when operations and acknowledgments come in. The values passed to the methods can be
   * used to reconstruct the exact server history.
   * 
   * <p>
   * WARNING: The server history ops cannot be applied to local client state, because they have not
   * been transformed properly. Server history ops are for other uses. To get the server ops to
   * apply locally, use {@link GenericOperationChannel#receive()}
   */
  public interface Listener<O> {
    /**
     * A local op is acknowledged as applied at this point in the server history op stream.
     * 
     * @param serverHistoryOp the operation as it appears in the server history, not necessarily as
     *          it was when passed into the channel.
     * @param clean true if the channel is now clean.
     */
    void onAck(O serverHistoryOp, boolean clean);

    /**
     * Called when some unrecoverable problem occurs.
     */
    void onError(Throwable e);

    /**
     * A remote op has been received. Do not use the parameter to apply to local state, instead use
     * {@link GenericOperationChannel#receive()}.
     * 
     * @param serverHistoryOp the operation as it appears in the server history (do not apply this
     *          to local state).
     */
    void onRemoteOp(O serverHistoryOp);

    void onSaveStateChanged(boolean isSaving, boolean isPending);
  }

  /**
   * Provides a channel for incoming operations.
   */
  public interface ReceiveOpChannel<O> {
    public interface Listener<O> {
      void onError(Throwable e);

      void onMessage(int resultingRevision, String sid, O op);
    }

    void connect(int revision, Listener<O> listener);

    void disconnect();

    void onKnownHeadVersion(int headVersion);
  }

  /**
   * Provides a service to send outgoing operations and synchronize the concurrent object with the
   * server.
   */
  public interface SendOpService<O> {
    public interface Callback {
      void onConnectionError(Throwable e);

      void onFatalError(Throwable e);

      void onSuccess(int appliedRevision);
    }

    /**
     * Called to indicate that the channel is no longer interested in being notified via the given
     * callback object, so implementations may optionally discard the associated request state and
     * callback. It is safe to do nothing with this method.
     * 
     * @param callback
     */
    void callbackNotNeeded(Callback callback);

    /**
     * Lightweight request to get the current revision of the object without submitting operations
     * (somewhat equivalent to applying no operations).
     * 
     * <p>
     * Useful for synchronizing with the channel for retrying/reconnection.
     */
    void requestRevision(String sessionId, Callback callback);

    /**
     * Submit operations at the given revision.
     * 
     * <p>
     * Will be called back with the revision at which the ops were applied.
     */
    void submitOperations(String sessionId, int revision, ArrayOf<O> operations, Callback callback);
  }

  /**
   * To reduce the risk of the channel behaving unpredictably due to poor external implementations,
   * we handle discarding callbacks internally and merely hint to the service that it may be
   * discarded.
   */
  abstract class DiscardableCallback implements SendOpService.Callback {
    private boolean discarded = false;

    @Override
    public void onConnectionError(Throwable e) {
      if (!isConnected()) {
        return;
      }

      if (discarded) {
        logger.log(Level.WARNING, "Ignoring failure, ", e);
        return;
      }
      discarded = true;

      logger.log(Level.WARNING, "Retryable failure, will resync.", e);
      delayResync();
    }

    @Override
    public final void onFatalError(Throwable e) {
      fail(e);
    }

    @Override
    public void onSuccess(int appliedRevision) {
      if (!isConnected()) {
        return;
      }

      if (discarded) {
        logger.log(Level.INFO, "Ignoring success @" + appliedRevision);
        return;
      }
      discarded = true;

      success(appliedRevision);
    }

    void discard() {
      if (discarded) {
        return;
      }
      discarded = true;
      submitService.callbackNotNeeded(this);
    }

    abstract void success(int appliedRevision);
  }
  enum State {
    /**
     * Cannot send ops in this state. All states can transition here if either explicitly requested,
     * or if there is a permanent failure.
     */
    UNINITIALISED,

    /**
     * No unacked ops. There may be queued ops though.
     */
    ALL_ACKED,

    /**
     * Waiting for an ack for sent ops. Will transition back to ALL_ACKED if successful, or to
     * DELAY_RESYNC if there is a retryable failure.
     */
    WAITING_ACK,

    /**
     * Waiting to attempt a resync/reconnect (delay can be large due to exponential backoff). Will
     * transition to WAITING_SYNC when the delay is up and we send off the version request, or to
     * ALL_ACKED if all ops get acked while waiting.
     */
    DELAY_RESYNC,

    /**
     * Waiting for our version sync. If it turns out that all ops get acked down the channel in the
     * meantime, we can return to ALL_ACKED. Otherwise, we can resend and go to WAITING_ACK. If
     * there is a retryable failure, we will go to DELAY_RESYNC
     */
    WAITING_SYNC;

    private ArrayOfInt to;
    static {
      UNINITIALISED.transitionsTo(ALL_ACKED);
      ALL_ACKED.transitionsTo(WAITING_ACK);
      WAITING_ACK.transitionsTo(ALL_ACKED, DELAY_RESYNC);
      DELAY_RESYNC.transitionsTo(ALL_ACKED, WAITING_SYNC);
      WAITING_SYNC.transitionsTo(ALL_ACKED, WAITING_ACK, DELAY_RESYNC);
    }

    private void transitionsTo(State... validTransitionStates) {
      // Also, everything may transition to UNINITIALISED
      to = Collections.arrayOfInt();
      to.push(UNINITIALISED.ordinal());
      for (State state : validTransitionStates) {
        to.push(state.ordinal());
      }
    }
  }

  private static final Logger logger = Logger.getLogger(GenericOperationChannel.class.getName());
  private final FuzzingBackOffGenerator backoffGenerator = new FuzzingBackOffGenerator(1500,
      1800 * 1000, 0.5);
  private boolean isMaybeSendTaskScheduled;
  private final Runnable maybeSendTask = new Runnable() {
    @Override
    public void run() {
      isMaybeSendTaskScheduled = false;
      maybeSend();
    }
  };
  private boolean isResyncTaskScheduled;
  private final RepeatingCommand delayedResyncTask = new RepeatingCommand() {
    @Override
    public boolean execute() {
      if (!isResyncTaskScheduled) {
        return false;
      }
      isResyncTaskScheduled = false;
      doResync();
      return false;
    }
  };

  private final ReceiveOpChannel.Listener<O> receiveListener = new ReceiveOpChannel.Listener<O>() {
    @Override
    public void onError(Throwable e) {
      if (!isConnected()) {
        return;
      }
      listener.onError(e);
    }

    @Override
    public void onMessage(int resultingRevision, String sid, O operation) {
      if (!isConnected()) {
        return;
      }

      logger.log(Level.FINE, "my sid=" + sessionId + ", incoming sid=" + sid);
      if (sessionId.equals(sid)) {
        onAckOwnOperation(resultingRevision, operation);
      } else {
        onIncomingOperation(resultingRevision, operation);
      }

      maybeSynced();
    }
  };

  private final ReceiveOpChannel<O> channel;
  private final SendOpService<O> submitService;
  private final Listener<O> listener;

  // State variables
  private State state = State.UNINITIALISED;
  private final TransformQueue<O> queue;
  private String sessionId;
  private int retryVersion = -1;
  private DiscardableCallback submitCallback; // mutable to discard out of date ones
  private DiscardableCallback versionCallback;

  public GenericOperationChannel(TransformQueue<O> queue, ReceiveOpChannel<O> channel,
      SendOpService<O> submitService, Listener<O> listener) {
    this.queue = queue;
    this.channel = channel;
    this.submitService = submitService;
    this.listener = listener;
  }

  public void connect(int revision, String sessionId) {
    assert !isConnected() : "Already connected";
    assert sessionId != null : "Null sessionId";
    assert revision >= 0 : "Invalid revision, " + revision;
    this.sessionId = sessionId;
    channel.connect(revision, receiveListener);
    queue.init(revision);
    setState(State.ALL_ACKED);
  }

  public void disconnect() {
    checkConnected();
    channel.disconnect();
    sessionId = null;
    setState(State.UNINITIALISED);
  }

  /**
   * @return true if there are no queued or unacknowledged ops
   */
  public boolean isClean() {
    checkConnected();
    boolean ret = !queue.hasQueuedClientOps() && !queue.hasUnacknowledgedClientOps();
    // isClean() implies ALL_ACKED
    assert !ret || state == State.ALL_ACKED;
    return ret;
  }

  public boolean isConnected() {
    // UNINITIALISED implies sessionId == null.
    assert state != State.UNINITIALISED || sessionId == null;
    return sessionId != null;
  }

  public O peek() {
    checkConnected();
    return queue.hasServerOp() ? queue.peekServerOp() : null;
  }

  public O receive() {
    checkConnected();
    return queue.hasServerOp() ? queue.removeServerOp() : null;
  }

  public int revision() {
    checkConnected();
    return queue.revision();
  }

  public void send(O operation) {
    if (operation.isNoOp()) {
      return;
    }
    checkConnected();
    queue.clientOp(operation);
    // Defer the send to allow multiple ops to batch up, and
    // to avoid waiting for the browser's network stack in case
    // we are in a time critical piece of code. Note, we could even
    // go further and avoid doing the transform inside the queue.
    if (!queue.hasUnacknowledgedClientOps()) {
      assert state == State.ALL_ACKED;
      isMaybeSendTaskScheduled = true;
      NativeInterface.get().scheduleDeferred(maybeSendTask);
    }
  }

  State getState() {
    return state;
  }

  private void allAcked() {

    // This also counts as an early sync
    synced();

    // No point waiting for the XHR to come back, we're already acked.
    submitCallback.discard();

    setState(State.ALL_ACKED);
    if (queue.hasQueuedClientOps()) {
      NativeInterface.get().scheduleDeferred(maybeSendTask);
    }
  }

  private void checkConnected() {
    assert isConnected() : "Not connected";
  }

  private void checkState(State newState) {

    switch (newState) {
      case UNINITIALISED:
        assert sessionId == null;
        break;
      case ALL_ACKED:
        assert sessionId != null;
        assert queue.revision() >= 0;
        assert isDiscarded(submitCallback);
        assert isDiscarded(versionCallback);
        assert retryVersion == -1;
        assert !queue.hasUnacknowledgedClientOps();
        assert !isResyncTaskScheduled;
        break;
      case WAITING_ACK:
        assert !isDiscarded(submitCallback);
        assert isDiscarded(versionCallback);
        assert retryVersion == -1;
        assert !isMaybeSendTaskScheduled;
        assert !isResyncTaskScheduled;
        break;
      case DELAY_RESYNC:
        assert isDiscarded(submitCallback);
        assert isDiscarded(versionCallback);
        assert retryVersion == -1;
        assert !isMaybeSendTaskScheduled;
        assert isResyncTaskScheduled;
        break;
      case WAITING_SYNC:
        assert isDiscarded(submitCallback);
        assert !isDiscarded(versionCallback);
        assert !isMaybeSendTaskScheduled;
        assert !isResyncTaskScheduled;
        break;
      default:
        throw new AssertionError("State " + state + " not implemented");
    }
  }

  private void delayResync() {
    isResyncTaskScheduled = true;
    Scheduler.get().scheduleFixedDelay(delayedResyncTask, 5 * 1000);
    setState(State.DELAY_RESYNC);
  }

  private void doResync() {
    versionCallback = new DiscardableCallback() {
      @Override
      public void success(int appliedRevision) {
        logger.log(Level.INFO, "version callback returned @" + appliedRevision);
        retryVersion = appliedRevision;
        maybeSynced();
      }
    };
    submitService.requestRevision(sessionId, versionCallback);
    setState(State.WAITING_SYNC);
  }

  private void fail(Throwable e) {
    logger.log(Level.WARNING, "channel.fail()");
    if (!isConnected()) {
      logger.log(Level.WARNING, "not connected");
      return;
    }

    logger.log(Level.WARNING, "Permanent failure");

    disconnect();

    listener.onError(e);
  }

  private boolean isDiscarded(DiscardableCallback c) {
    return c == null || c.discarded;
  }

  private void maybeEagerlyHandleAck(int appliedRevision) {
    ArrayOf<O> ownOps = queue.ackOpsIfVersionMatches(appliedRevision);
    if (ownOps == null) {
      channel.onKnownHeadVersion(appliedRevision);
      return;
    }
    logger.log(Level.INFO, "Eagerly acked @" + appliedRevision);

    // Special optimization: there were no concurrent ops on the server,
    // so we don't need to wait for them or even our own ops on the channel.
    // We just throw back our own ops to our listeners as if we had
    // received them from the server (we expect they should exactly
    // match the server history we will shortly receive on the channel).

    assert !queue.hasUnacknowledgedClientOps();
    allAcked();

    boolean isClean = isClean();
    for (int i = 0, len = ownOps.length(); i < len; i++) {
      boolean isLast = i == ownOps.length() - 1;
      listener.onAck(ownOps.get(i), isClean && isLast);
    }
  }

  private void maybeSend() {
    if (queue.hasUnacknowledgedClientOps()) {
      logger.log(Level.INFO, state + ", Has " + queue.unackedClientOpsCount() + " unacked...");
      return;
    }
    if (queue.hasQueuedClientOps()) {
      queue.pushQueuedOpsToUnacked();
      if (queue.hasUnacknowledgedClientOps()) {
        sendUnackedOps();
      }
    }
  }

  private void maybeSynced() {
    if (state == State.WAITING_SYNC && retryVersion != -1 && queue.revision() >= retryVersion) {

      // Our ping has returned.
      synced();

      if (queue.hasUnacknowledgedClientOps()) {
        // We've synced and didn't see our unacked ops, so they never made it (we
        // are not handling the case of ops that hang around on the network and
        // make it after a very long time, i.e. after a sync round trip. This
        // scenario most likely extremely rare).

        // Send the unacked ops again.
        sendUnackedOps();
      }
    }
  }

  private void onAckOwnOperation(int resultingRevision, O ackedOp) {
    boolean alreadyAckedByXhr = queue.expectedAck(resultingRevision);
    if (alreadyAckedByXhr) {
      // Nothing to do, just receiving expected operations that we've
      // already handled by the optimization in maybeEagerlyHandleAck()
      return;
    }

    boolean allAcked = queue.ackClientOp(resultingRevision);
    logger.log(Level.INFO, "Ack @" + resultingRevision + ", " + queue.unackedClientOpsCount()
        + " ops remaining");

    // If we have more ops to send and no unacknowledged ops,
    // then schedule a send.
    if (allAcked) {
      allAcked();
    }

    listener.onAck(ackedOp, isClean());
  }

  private void onIncomingOperation(int revision, O operation) {
    logger.log(Level.INFO, "Incoming " + revision + " " + state);
    queue.serverOp(revision, operation);
    listener.onRemoteOp(operation);
  }

  /**
   * Sends unacknowledged ops and transitions to the WAITING_ACK state
   */
  private void sendUnackedOps() {
    ArrayOf<O> ops = queue.unackedClientOps();
    assert ops.length() > 0;
    logger.log(Level.INFO, "Sending " + ops.length() + " ops @" + queue.revision());
    submitCallback = new DiscardableCallback() {
      @Override
      void success(int appliedRevision) {
        maybeEagerlyHandleAck(appliedRevision);
      }
    };

    submitService.submitOperations(sessionId, queue.revision(), ops, submitCallback);
    setState(State.WAITING_ACK);
  }

  /**
   * Brings the state variable to the given value.
   * 
   * <p>
   * Verifies that other member variables are are in the correct state.
   */
  private void setState(State newState) {
    // Check transitioning from valid old state
    assert state.to.contains(newState.ordinal()) : "Invalid state transition " + state + " -> "
        + newState;

    // Check consistency of variables with new state
    checkState(newState);

    state = newState;

    switch (newState) {
      case ALL_ACKED:
        listener.onSaveStateChanged(false, queue.hasQueuedClientOps());
        break;
      case WAITING_ACK:
        listener.onSaveStateChanged(true, queue.hasQueuedClientOps());
        break;
      default:
        break;
    }
  }

  /**
   * We have reached a state where we are confident we know whether any unacked ops made it to the
   * server.
   */
  private void synced() {
    logger.log(Level.INFO, "synced @" + queue.revision());

    retryVersion = -1;
    isResyncTaskScheduled = false;

    if (versionCallback != null) {
      versionCallback.discard();
    }
  }
}