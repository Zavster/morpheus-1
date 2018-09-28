/*
 * Copyright (C) 2014-2018 D3X Systems - All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zavtech.morpheus.util.http;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import com.zavtech.morpheus.util.IO;
import com.zavtech.morpheus.util.Initialiser;

/**
 * A basic API abstraction for making HTTP calls to allow other libraries to be used with additional customization
 *
 * @author Xavier Witdouck
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 */
public abstract class HttpClient {

    private static HttpClient defaultClient = new DefaultClient();

    /**
     * Constructor
     */
    public HttpClient() {
        super();
    }

    /**
     * Returns a reference to the default client
     * @return  the default client reference
     */
    public static HttpClient getDefault() {
        return defaultClient;
    }

    /**
     * Sets the default http client to use
     * @param defaultClient     the default http client to use
     */
    public static void setDefault(HttpClient defaultClient) {
        HttpClient.defaultClient = defaultClient;
    }


    /**
     * Executes an HTTP GET request using the configurator to setup the request descriptor
     * @param configurator  the HTTP request configurator
     * @param <T>           the type produced by the response handler bound to the request
     * @return              the optional result produced by the response handler
     */
    public abstract <T> Optional<T> doGet(Consumer<HttpRequest<T>> configurator);


    /**
     * Executes an HTTP POST request using the configurator to setup the request descriptor
     * @param configurator  the HTTP request configurator
     * @param <T>           the type produced by the response handler bound to the request
     * @return              the optional result produced by the response handler
     */
    public abstract <T> Optional<T> doPost(Consumer<HttpPost<T>> configurator);



    /**
     * A callback interface to handle the response to an HttpRequest
     */
    public interface ResponseHandler<T> {

        /**
         * Called after an http request has been invoked
         * @param response      the response object
         * @return              the optional result for this handler
         * @throws RuntimeException    if the handler fails to process request
         */
        Optional<T> onResponse(HttpResponse response) throws RuntimeException;
    }


    /**
     * The default Http client implementation that leverages the core JDK api
     */
    private static class DefaultClient extends HttpClient {


        @Override
        public <T> Optional<T> doGet(Consumer<HttpRequest<T>> configurator) {
            return execute(Initialiser.apply(new HttpRequest<>(HttpMethod.GET), configurator));
        }


        @Override
        public <T> Optional<T> doPost(Consumer<HttpPost<T>> configurator) {
            return execute(Initialiser.apply(new HttpPost<>(), configurator));
        }

        /**
         * Executes the Http request, returning the result produced by the response handler
         * @param request   the request descriptor
         * @param <T>       the type of response object
         * @return          the result produced by response handler
         */
        private <T> Optional<T> execute(HttpRequest<T> request) {
            final URL url = request.getUrl();
            final int retryCount = request.getRetryCount();
            for (int i = 0; i <= retryCount; ++i) {
                try {
                    final HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    conn.setRequestMethod(request.getMethod().name());
                    conn.setConnectTimeout(request.getConnectTimeout());
                    conn.setReadTimeout(request.getReadTimeout());
                    conn.setDoOutput(request.getContent().isPresent());
                    request.getHeaders().forEach(conn::setRequestProperty);
                    request.getCookies().forEach((key, value) -> {
                        conn.addRequestProperty("Cookie", String.format("%s=%s", key, value));
                    });
                    final int statusCode = conn.getResponseCode();
                    if (hasMoved(statusCode)) {
                        final String newUrl = conn.getHeaderField("Location");
                        if (newUrl != null) return execute(request.copy(newUrl));
                        final String message = "Received re-direct response but no Location in header";
                        throw new HttpException(request, message, null);
                    }
                    request.getContent().ifPresent(bytes -> write(bytes, conn));
                    final String message = conn.getResponseMessage();
                    final HttpStatus status = new HttpStatus(statusCode, message);
                    final HttpResponse response = new DefaultResponse(status, conn);
                    return request.getResponseHandler().flatMap(handler -> handler.onResponse(response));
                } catch (HttpException ex) {
                    throw ex; //no retries based on our own internally generated exception
                } catch (Exception ex) {
                    if (i == retryCount) {
                        throw new HttpException(request, ex.getMessage(), ex);
                    }
                }
            }
            return Optional.empty();
        }


        /**
         * Writes bytes to the output stream for the connection provided
         * @param bytes the bytes to write
         * @param conn  the connection to write bytes to
         */
        private void write(byte[] bytes, HttpURLConnection conn) {
            try (OutputStream os = new BufferedOutputStream(conn.getOutputStream())) {
                os.write(bytes);
                os.flush();
            } catch (IOException ex) {
                throw new RuntimeException("Failed to write byes to URL: " + conn.getURL(), ex);
            }
        }


        /**
         * Returns true if the status code implies the resource has moved
         * @param statusCode    the HTTP status code
         * @return              true if resource has moved
         */
        private boolean hasMoved(int statusCode) {
            switch (statusCode) {
                case HttpURLConnection.HTTP_MOVED_TEMP: return true;
                case HttpURLConnection.HTTP_MOVED_PERM: return true;
                case HttpURLConnection.HTTP_SEE_OTHER:  return true;
                default:                                return false;
            }
        }
    }

    /**
     * The HttpResponse object for the default HttpClient.
     */
    private static class DefaultResponse implements HttpResponse {

        private HttpStatus status;
        private InputStream stream;
        private HttpURLConnection conn;
        private List<HttpHeader> headers;

        /**
         * Constructor
         * @param conn  the http connection object
         */
        DefaultResponse(HttpStatus status, HttpURLConnection conn) throws IOException {
            this.conn = conn;
            this.status = status;
            this.stream = conn.getInputStream();
            this.headers = new ArrayList<>();
            conn.getHeaderFields().forEach((key, values) -> {
                if (key != null && values != null && !values.isEmpty()) {
                    values.forEach(value -> {
                        headers.add(new HttpHeader(key, value));
                    });
                }
            });
        }

        /**
         * Returns the content encoding for response
         * @return  the content encoding
         */
        private Optional<String> getContentEncoding() {
            for (HttpHeader header : headers) {
                if (header.getKey().equals("Content-Encoding")) {
                    return Optional.ofNullable(header.getValue());
                }
            }
            return Optional.empty();
        }

        @Override
        public HttpStatus getStatus() {
            return status;
        }

        @Override
        public InputStream getStream() {
            try {
                final String encoding = getContentEncoding().orElse("default").toLowerCase();
                if (encoding.equalsIgnoreCase("gzip")) {
                    return new GZIPInputStream(stream);
                } else {
                    return stream;
                }
            } catch (Exception ex) {
                throw new RuntimeException("Failed to read from input stream", ex);
            }
        }

        @Override
        public List<HttpHeader> getHeaders() {
            return headers;
        }

        @Override
        public void close() throws Exception {
            if (stream != null) {
                stream.close();
            }
        }
    }


    public static void main(String[] args) {
        HttpClient.getDefault().doGet(request -> {
            request.setUrl("https://finance.yahoo.com/quote/SPY?p=SPY");
            request.setResponseHandler(response -> {
                response.getHeaders().forEach(header -> {
                    IO.println(String.format("%s = %s", header.getKey(), header.getValue()));
                });
                return Optional.empty();
            });
        });
    }

}
