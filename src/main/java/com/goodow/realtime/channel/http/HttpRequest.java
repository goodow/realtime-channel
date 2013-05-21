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
package com.goodow.realtime.channel.http;

import java.io.IOException;

/**
 * Low-level HTTP request.
 * 
 * <p>
 * This allows providing a different implementation of the HTTP request that is more compatible with
 * the Java environment used.
 * </p>
 * 
 * <p>
 * Implementation has no fields and therefore thread-safe, but sub-classes are not necessarily
 * thread-safe.
 * </p>
 * 
 */
public abstract class HttpRequest {

  /** Content length or less than zero if not known. */
  private long contentLength = -1;

  /** Content encoding (for example {@code "gzip"}) or {@code null} for none. */
  private String contentEncoding;

  /** Content type or {@code null} for none. */
  private String contentType;

  /** Streaming content or {@code null} for no content. */
  private String content;

  /**
   * Adds a header to the HTTP request.
   * 
   * <p>
   * Note that multiple headers of the same name need to be supported, in which case
   * {@link #addHeader} will be called for each instance of the header.
   * </p>
   * 
   * @param name header name
   * @param value header value
   */
  public abstract void addHeader(String name, String value);

  /** Executes the request and returns a low-level HTTP response object. */
  public abstract void executeAsync(HttpRequestCallback callback) throws IOException;

  /**
   * Returns the streaming content or {@code null} for no content.
   */
  public final String getContent() {
    return content;
  }

  /**
   * Returns the content encoding (for example {@code "gzip"}) or {@code null} for none.
   */
  public final String getContentEncoding() {
    return contentEncoding;
  }

  /**
   * Returns the content length or less than zero if not known.
   */
  public final long getContentLength() {
    return contentLength;
  }

  /**
   * Returns the content type or {@code null} for none.
   */
  public final String getContentType() {
    return contentType;
  }

  /**
   * Sets the streaming content or {@code null} for no content.
   * 
   * @throws IOException I/O exception
   */
  public final void setContent(String content) throws IOException {
    this.content = content;
  }

  /**
   * Sets the content encoding (for example {@code "gzip"}) or {@code null} for none.
   * 
   * @throws IOException I/O exception
   */
  public final void setContentEncoding(String contentEncoding) throws IOException {
    this.contentEncoding = contentEncoding;
  }

  /**
   * Sets the content length or less than zero if not known.
   * 
   * <p>
   * Default value is {@code -1}.
   * </p>
   * 
   * @throws IOException I/O exception
   */
  public final void setContentLength(long contentLength) throws IOException {
    this.contentLength = contentLength;
  }

  /**
   * Sets the content type or {@code null} for none.
   */
  public final void setContentType(String contentType) {
    this.contentType = contentType;
  }

  /**
   * Sets the connection and read timeouts.
   * 
   * <p>
   * Default implementation does nothing, but subclasses should normally override.
   * </p>
   * 
   * @param connectTimeout timeout in milliseconds to establish a connection or {@code 0} for an
   *          infinite timeout
   * @param readTimeout Timeout in milliseconds to read data from an established connection or
   *          {@code 0} for an infinite timeout
   * @throws IOException I/O exception
   */
  public void setTimeout(int connectTimeout, int readTimeout) throws IOException {
  }
}
