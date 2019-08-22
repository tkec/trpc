/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.trpc.benchmark;

import com.github.trpc.benchmark.service.EchoServiceImpl;
import com.github.trpc.common.protocol.rpcprotocol.RpcProtocol;
import com.github.trpc.server.RpcServer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcServerTest {
    public static void main(String[] args) {
        int port = 8002;
        if (args.length == 1) {
            port = Integer.valueOf(args[0]);
        }
//        options.setNamingServiceUrl("zookeeper://127.0.0.1:2181");
//        final RpcServer rpcServer = new RpcServer(port, options);
        final RpcServer rpcServer = new RpcServer(port, 500);
        rpcServer.registerService(new EchoServiceImpl());
        rpcServer.setProtocol(new RpcProtocol());
        rpcServer.start();

        // make server keep running
        synchronized (RpcServerTest.class) {
            try {
                RpcServerTest.class.wait();
            } catch (Throwable e) {
            }
        }
    }
}
