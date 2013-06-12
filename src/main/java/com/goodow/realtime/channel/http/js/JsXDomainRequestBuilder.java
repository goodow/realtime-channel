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
package com.goodow.realtime.channel.http.js;

import com.goodow.realtime.channel.http.js.JsXDomainRequest.JsXDomainRequestHandler;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.http.client.Header;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.RequestPermissionException;
import com.google.gwt.http.client.RequestTimeoutException;
import com.google.gwt.http.client.Response;

public class JsXDomainRequestBuilder extends RequestBuilder {
  public static class IECrossSiteRequest extends Request {
    static final int UNSENT = 0;
    static final int OPEN = 1;
    static final int SENT = 2;
    static final int DONE = 3;
    private int _status = UNSENT;
    private final JsXDomainRequest _xhr;

    public IECrossSiteRequest(JsXDomainRequest xhr) {
      if (xhr == null) {
        throw new NullPointerException();
      }
      _xhr = xhr;
    }

    @Override
    public void cancel() {
      if (isPending()) {
        _xhr.abort();
      }
    }

    @Override
    public boolean isPending() {
      return (_status == OPEN || _status == SENT);
    }

    void setStatus(int status) {
      _status = status;
    }
  }

  public static class IECrossSiteResponse extends Response {
    public static class IEHeader extends Header {
      private final String _name;
      private final String _value;

      public IEHeader(String name, String val) {
        _name = name;
        _value = val;
      }

      @Override
      public String getName() {
        return _name;
      }

      @Override
      public String getValue() {
        return _value;
      }
    }

    private final JsXDomainRequest _xhr;

    public IECrossSiteResponse(JsXDomainRequest xhr) {
      _xhr = xhr;
    }

    @Override
    public String getHeader(String header) {
      return header.equals("Content-Type") ? _xhr.getContentType() : null;
    }

    @Override
    public Header[] getHeaders() {
      if (_xhr.getContentType() != null) {
        Header ret[] = new Header[1];
        ret[0] = new IEHeader("Content-Type", _xhr.getContentType());
        return ret;
      } else {
        return null;
      }
    }

    @Override
    public String getHeadersAsString() {
      return (_xhr.getContentType() == null) ? "" : ("Content-Type : " + _xhr.getContentType());
    }

    @Override
    public int getStatusCode() {
      return (_xhr != null) ? Response.SC_OK : Response.SC_BAD_REQUEST;
    }

    @Override
    public String getStatusText() {
      return "OK";
    }

    @Override
    public String getText() {
      return _xhr.getResponseText();
    }
  }

  public JsXDomainRequestBuilder(RequestBuilder.Method httpMethod, String url) {
    super(httpMethod, url);
  }

  @Override
  public Request send() throws RequestException {
    return doSend(getRequestData(), getCallback());
  }

  @Override
  public Request sendRequest(String data, RequestCallback callback) throws RequestException {
    return doSend(data, callback);
  }

  private Request doSend(String data, final RequestCallback callback) throws RequestException {
    JsXDomainRequest xhr = JsXDomainRequest.create();
    try {
      xhr.open(getHTTPMethod(), getUrl());
    } catch (JavaScriptException e) {
      RequestPermissionException requestPermissionException =
          new RequestPermissionException(getUrl());
      requestPermissionException.initCause(new RequestException(e.getMessage()));
      throw requestPermissionException;
    }
    // Cannot set content type on IE
    final IECrossSiteRequest req = new IECrossSiteRequest(xhr);
    req.setStatus(IECrossSiteRequest.OPEN);
    final int timeout;
    if ((timeout = getTimeoutMillis()) > 0) {
      xhr.setTimeout(getTimeoutMillis());
    }
    // set handlers
    xhr.setHandler(new JsXDomainRequestHandler() {
      @Override
      public void onError(JsXDomainRequest r) {
        // Assume permission exception since XDomainRequest does not
        // return an error reason
        req.setStatus(IECrossSiteRequest.DONE);
        callback.onError(req, new RequestPermissionException(getUrl()));
      }

      @Override
      public void onLoad(JsXDomainRequest r) {
        req.setStatus(IECrossSiteRequest.DONE);
        callback.onResponseReceived(req, new IECrossSiteResponse(r));
      }

      @Override
      public void onProgress(JsXDomainRequest r) {
      }

      @Override
      public void onTimeout(JsXDomainRequest r) {
        req.setStatus(IECrossSiteRequest.DONE);
        callback.onError(req, new RequestTimeoutException(req, timeout));
      }
    });
    try {
      xhr.send(data);
      req.setStatus(IECrossSiteRequest.SENT);
    } catch (JavaScriptException e) {
      throw new RequestException(e.getMessage());
    }
    return req;
  }
}
