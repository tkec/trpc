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

package com.github.trpc.example.benchmark;

import com.github.trpc.core.client.RpcClient;
import com.github.trpc.core.client.RpcProxy;
import com.github.trpc.core.common.exception.RpcException;
import com.github.trpc.core.common.protocol.rpcprotocol.RpcProtocol;
import com.github.trpc.example.benchmark.service.EchoService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class SyncBenchmarkTest {

    private static volatile boolean stop = false;

    public static class SendInfo {
        public long successRequestNum = 0;
        public long failRequestNum = 0;
        public long elapsedNs = 0;
    }

    public static void main(String[] args) throws InterruptedException, IOException {
//        if (args.length != 2) {
//            System.out.println("usage: BenchmarkTest list://127.0.0.1:8002 threadNum");
//            System.exit(-1);
//        }
//        RpcClient rpcClient = new RpcClient("127.0.0.1", 8002);
        RpcClient rpcClient = new RpcClient("list://127.0.0.1:8002,127.0.0.1:8003");
//        rpcClient.setProtocol(new RpcProtocol());
        // int threadNum = Integer.parseInt(args[1]);
        int threadNum = 1000;

        InputStream inputStream = Thread.currentThread().getClass()
                .getResourceAsStream("/message.txt");
        int length = inputStream.available();
        byte[] messageBytes = new byte[length];
        inputStream.read(messageBytes);
        log.info("message size=" + messageBytes.length);

        EchoService echoService = RpcProxy.getProxy(rpcClient, EchoService.class);

        SendInfo[] sendInfos = new SendInfo[threadNum];
        Thread[] threads = new Thread[threadNum];

        for (int i = 0; i < threadNum; i++) {
            sendInfos[i] = new SendInfo();
            threads[i] = new Thread(new ThreadTask(i, rpcClient, messageBytes, sendInfos[i], echoService),
                    "work-thread-" + i);
            threads[i].start();
        }
        long lastSuccessRequestNum = 0;
        long lastFailRequestNum = 0;
        long lastElapsedNs = 0;
        while (!stop) {
            long beginTime = System.nanoTime();
            try {
                Thread.sleep(1000);
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
            long successNum = 0;
            long failNum = 0;
            long elapseNs = 0;
            long averageElapsedNs = 0;
            for (SendInfo sendInfo : sendInfos) {
                successNum += sendInfo.successRequestNum;
                failNum += sendInfo.failRequestNum;
                elapseNs += sendInfo.elapsedNs;
            }
            if (successNum - lastSuccessRequestNum > 0) {
                averageElapsedNs = (elapseNs - lastElapsedNs) / (successNum - lastSuccessRequestNum);
            }
            long endTime = System.nanoTime();
            log.info("success={},fail={},average={}ns",
                    (successNum - lastSuccessRequestNum) * 1000 * 1000 * 1000 / (endTime - beginTime),
                    (failNum - lastFailRequestNum) * 1000 * 1000 * 1000 / (endTime - beginTime),
                    averageElapsedNs);
            lastSuccessRequestNum = successNum;
            lastFailRequestNum = failNum;
            lastElapsedNs = elapseNs;
        }
    }

    public static class ThreadTask implements Runnable {
        private int id;
        private RpcClient rpcClient;
        private byte[] messageBytes;
        private SendInfo sendInfo;
        private EchoService echoService;

        public ThreadTask(int id, RpcClient rpcClient, byte[] messageBytes,
                          SendInfo sendInfo, EchoService echoService) {
            this.id = id;
            this.rpcClient = rpcClient;
            this.messageBytes = messageBytes;
            this.sendInfo = sendInfo;
            this.echoService = echoService;
            // this.echoService = RpcProxy.getProxy(rpcClient, EchoService.class);
        }

        @Override
        public void run() {
            String request = "hello";

            while (!stop) {
                try {
                    long beginTime = System.nanoTime();
                    String response = echoService.echo(request);
                    if (!response.equals(request)) {
                        log.warn("id:{} request:{}, response:{}", id, request, response);
                    }
                    sendInfo.elapsedNs += (System.nanoTime() - beginTime);
                    sendInfo.successRequestNum++;
                } catch (RpcException ex) {
                    log.info("send exception:" + ex.getMessage());
                    sendInfo.failRequestNum++;
                }
            }
        }
    }
}
