/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modified from {@link org.elasticsearch.memcached.netty.MemcachedRestChannel}
 */

package org.elasticsearch.redis.netty;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.netty.buffer.ChannelBuffer;
import org.elasticsearch.common.netty.buffer.ChannelBuffers;
import org.elasticsearch.common.netty.channel.Channel;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.http.HttpException;
import org.elasticsearch.redis.RedisRestRequest;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.XContentRestResponse;

import java.io.IOException;
import java.nio.charset.Charset;


public class RedisRestChannel implements RestChannel {

    private final ESLogger logger = ESLoggerFactory.getLogger(RedisRestChannel.class.getSimpleName());

    public static final ChannelBuffer CRLF = ChannelBuffers.copiedBuffer("\r\n", Charset.forName("US-ASCII"));

    private final Channel channel;

    private final RedisRestRequest request;

    public static String setOption = "standard";
    public static String delOption = "standard";

    public RedisRestChannel(Channel channel, RedisRestRequest request) {
        this.channel = channel;
        this.request = request;
    }

    @Override
    public void sendResponse(RestResponse response) {
        switch (request.method()) {
            case GET:
                sendJsonResponse(response);
                break;
            case POST:
            case PUT:
                if (setOption.equals("json")) {
                    sendJsonResponse(response);
                } else {  // default to standard
                    if (response.status().getStatus() >= 400) {
                        sendByesResponse('-', 'E', 'r', 'r', 'o', 'r');
                    } else {
                        sendByesResponse('+', 'O', 'K');
                    }
                }
                break;
            case DELETE:
                if (delOption.equals("json")) {
                    sendJsonResponse(response);
                } else {   // default to standard
                    if (response.status().getStatus() >= 400) {
                        sendByesResponse(':', '0');
                    } else {
                        sendByesResponse(':', '1');
                    }
                }
                break;
            case HEAD:
                if (response.status().getStatus() >= 400) {
                    sendByesResponse(':', '0');
                } else {
                    sendByesResponse(':', '1');
                }
                break;

        }
    }

    private void sendJsonResponse(RestResponse response) {
        int contentLength = 0;
        try {
            contentLength = response.contentLength();
        } catch (IOException e) {
            logger.error("Failed to get content length", e);
        }
        ChannelBuffer writeBuffer = ChannelBuffers.dynamicBuffer(3 + contentLength);
        writeBuffer.writeByte('+');
        ChannelBuffer buf;
        if (response instanceof XContentRestResponse) {
            XContentBuilder builder = ((XContentRestResponse) response).builder();
            if (response.contentThreadSafe()) {
                buf = builder.bytes().toChannelBuffer();
            } else {
                buf = builder.bytes().copyBytesArray().toChannelBuffer();
            }
        } else {
            try {
                if (response.contentThreadSafe()) {
                    buf = ChannelBuffers.wrappedBuffer(response.content(), response.contentOffset(), response.contentLength());
                } else {
                    buf = ChannelBuffers.copiedBuffer(response.content(), response.contentOffset(), response.contentLength());
                }
            } catch (IOException e) {
                throw new HttpException("Failed to convert response to bytes", e);
            }
        }
        int readableBytes = buf.readableBytes();
        for (int i = 0; i < readableBytes; i++) {
            byte next = buf.readByte();
            if (next != '\r' && next != '\n') {  // some elasticsearch json response contains \r\n, need to remove them as redis response is terminated by \r\n
                writeBuffer.writeByte(next);
            }
        }
        writeBuffer.writeBytes(CRLF.duplicate());
        channel.write(writeBuffer);
    }

    private void sendByesResponse(char... chars) {
        ChannelBuffer writeBuffer = ChannelBuffers.dynamicBuffer(2 + chars.length);
        for (char c : chars) {
            writeBuffer.writeByte(c);
        }
        writeBuffer.writeBytes(CRLF.duplicate());
        channel.write(writeBuffer);
    }
}
