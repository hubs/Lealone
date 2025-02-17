/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lealone.transaction.aote;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.lealone.net.NetEndpoint;

class TransactionValidator extends Thread {

    private static final QueuedMessage CLOSE_SENTINEL = new QueuedMessage(null, null);
    private static final BlockingQueue<QueuedMessage> backlog = new LinkedBlockingQueue<>();

    private static final TransactionValidator INSTANCE = new TransactionValidator();

    static TransactionValidator getInstance() {
        return INSTANCE;
    }

    private volatile boolean isStopped = false;
    private volatile boolean isStarted = false;

    private TransactionValidator() {
        super("TransactionValidator");
        setDaemon(true);
    }

    void close() {
        if (!isStopped) {
            backlog.clear();
            isStopped = true;
            backlog.add(CLOSE_SENTINEL);
        }
    }

    @Override
    public void run() {
        final List<QueuedMessage> drainedMessages = new ArrayList<>(128);
        outer: while (true) {
            if (backlog.drainTo(drainedMessages, drainedMessages.size()) == 0) {
                try {
                    drainedMessages.add(backlog.take());
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
            }

            for (QueuedMessage qm : drainedMessages) {
                if (isStopped)
                    break outer;
                try {
                    validateTransaction(qm);
                } catch (Exception e) {
                }
            }
            drainedMessages.clear();
        }
    }

    @Override
    public synchronized void start() {
        if (isStarted)
            return;
        isStarted = true;
        super.start();
    }

    private static void validateTransaction(QueuedMessage qm) {
        String[] allLocalTransactionNames = qm.allLocalTransactionNames.split(",");
        boolean isFullSuccessful = true;
        String localHostAndPort = NetEndpoint.getLocalTcpHostAndPort();
        for (String localTransactionName : allLocalTransactionNames) {
            if (!localTransactionName.startsWith(localHostAndPort)) {
                if (!qm.t.validator.validate(localTransactionName)) {
                    isFullSuccessful = false;
                    break;
                }
            }
        }

        if (isFullSuccessful) {
            qm.t.commitAfterValidate(qm.t.transactionId);
        }
    }

    static void enqueue(AOTransaction t, String allLocalTransactionNames) {
        try {
            backlog.put(new QueuedMessage(t, allLocalTransactionNames));
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    private static class QueuedMessage {
        final AOTransaction t;
        final String allLocalTransactionNames;

        QueuedMessage(AOTransaction t, String allLocalTransactionNames) {
            this.t = t;
            this.allLocalTransactionNames = allLocalTransactionNames;
        }
    }
}
