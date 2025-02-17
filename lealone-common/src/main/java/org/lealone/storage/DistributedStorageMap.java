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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.lealone.common.exceptions.DbException;
import org.lealone.db.Session;
import org.lealone.net.NetEndpoint;
import org.lealone.storage.type.StorageDataType;

public interface DistributedStorageMap<K, V> extends StorageMap<K, V> {

    List<NetEndpoint> getReplicationEndpoints(Object key);

    Object replicationPut(Session session, Object key, Object value, StorageDataType valueType);

    Object replicationGet(Session session, Object key);

    Object replicationAppend(Session session, Object value, StorageDataType valueType);

    void addLeafPage(PageKey pageKey, ByteBuffer page, boolean addPage);

    void removeLeafPage(PageKey pageKey);

    LeafPageMovePlan prepareMoveLeafPage(LeafPageMovePlan leafPageMovePlan);

    public default ByteBuffer readPage(PageKey pageKey) {
        throw DbException.getUnsupportedException("readPage");
    }

    void setRootPage(ByteBuffer buff);

    default Map<String, List<PageKey>> getEndpointToPageKeyMap(Session session, K from, K to) {
        return null;
    }
}
