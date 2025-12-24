// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.search.cache;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import com.google.common.cache.RemovalNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class ElasticsearchClientRemovalListenerTest {

    private ElasticsearchClientRemovalListener listener;
    private RemovalNotification<String, ElasticsearchClient> notification;
    private ElasticsearchClient mockClient;
    private ElasticsearchTransport mockTransport;

    @BeforeEach
    void setUp() {
        listener = new ElasticsearchClientRemovalListener();
        notification = mock(RemovalNotification.class);
        mockClient = mock(ElasticsearchClient.class);
        mockTransport = mock(ElasticsearchTransport.class);
    }

    @Test
    void onRemoval_shouldCloseTransport_whenClientNotNull() throws Exception {
        when(notification.getValue()).thenReturn(mockClient);
        when(mockClient._transport()).thenReturn(mockTransport);

        listener.onRemoval(notification);

        verify(mockTransport).close();
    }

    @Test
    void onRemoval_shouldDoNothing_whenClientIsNull() {
        when(notification.getValue()).thenReturn(null);
        listener.onRemoval(notification);
        verifyNoInteractions(mockClient);
    }
}
