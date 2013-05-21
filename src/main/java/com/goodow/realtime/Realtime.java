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
package com.goodow.realtime;

import com.goodow.realtime.Error.ErrorHandler;
import com.goodow.realtime.channel.RealtimeChannelDemuxer;
import com.goodow.realtime.channel.operation.RealtimeOperationSucker;
import com.goodow.realtime.util.ModelFactory;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.Exportable;

/**
 * The Goodow Realtime API.
 */
@ExportPackage("")
@Export(ModelFactory.PACKAGE_PREFIX_REALTIME)
public class Realtime implements Exportable {
  private static String TOKEN;
  private static String USERID;

  public static void authorize(String token, String userId) {
    TOKEN = token;
    USERID = userId;
  }

  /**
   * Returns an OAuth access token.
   * 
   * @return An OAuth 2.0 access token.
   */
  public static String getToken() {
    return TOKEN;
  }

  /**
   * Loads the realtime data model associated with {@code docId}. If no realtime data model is
   * associated with {@code docId}, a new realtime document will be created and
   * {@code opt_initializer} will be called (if it is provided).
   * 
   * @param docId The ID of the document to load.
   * @param onLoaded A callback that will be called when the realtime document is ready. The created
   *          or opened realtime document object will be passed to this function.
   * @param opt_initializer An optional initialization function that will be called before
   *          {@code onLoaded} only the first time that the document is loaded. The document's
   *          {@link com.goodow.realtime.Model} object will be passed to this function.
   * @param opt_error An optional error handling function that will be called if an error occurs
   *          while the document is being loaded or edited. A {@link com.goodow.realtime.Error}
   *          object describing the error will be passed to this function.
   */
  public static void load(String docId, DocumentLoadedHandler onLoaded,
      ModelInitializerHandler opt_initializer, ErrorHandler opt_error) {
    RealtimeChannelDemuxer demuxer = RealtimeChannelDemuxer.get();
    DocumentBridge snapshot = demuxer.getSnapshot(docId);
    if (snapshot != null) {
      onLoaded.onLoaded(snapshot.document);
      return;
    }
    new RealtimeOperationSucker(docId, USERID).load(onLoaded, opt_initializer, opt_error);
  }
}
