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
package org.lealone.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lealone.net.NetEndpoint;
import org.lealone.net.NetInputStream;
import org.lealone.net.NetOutputStream;

public class LeafPageMovePlan {

    public final String moverHostId;
    public final List<NetEndpoint> replicationEndpoints;
    public final PageKey pageKey;
    private int index;

    public LeafPageMovePlan(String moverHostId, List<NetEndpoint> replicationEndpoints, PageKey pageKey) {
        this.moverHostId = moverHostId;
        this.replicationEndpoints = replicationEndpoints;
        this.pageKey = pageKey;
    }

    public void incrementIndex() {
        index++;
    }

    public int getIndex() {
        return index;
    }

    public List<String> getReplicationEndpoints() {
        List<String> endpoints = new ArrayList<>(replicationEndpoints.size());
        for (NetEndpoint e : replicationEndpoints)
            endpoints.add(e.getHostAndPort());

        return endpoints;
    }

    public void serialize(NetOutputStream out) throws IOException {
        out.writeInt(index).writeString(moverHostId).writePageKey(pageKey);
        out.writeInt(replicationEndpoints.size());
        for (NetEndpoint e : replicationEndpoints) {
            out.writeString(e.getHostAndPort());
        }
    }

    public static LeafPageMovePlan deserialize(NetInputStream in) throws IOException {
        int index = in.readInt();
        String moverHostId = in.readString();
        PageKey pageKey = in.readPageKey();
        int size = in.readInt();
        ArrayList<NetEndpoint> replicationEndpoints = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            replicationEndpoints.add(NetEndpoint.createTCP(in.readString()));
        }
        LeafPageMovePlan plan = new LeafPageMovePlan(moverHostId, replicationEndpoints, pageKey);
        plan.index = index;
        return plan;
    }

}
