package org.opengroup.osdu.search.provider.azure.persistence.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.search.provider.azure.persistence.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RedisCacheSettingsImplTest {

    private final String[] ids = {"id1", "id2"};
    private final CacheSettingSchema[] cacheSettingSchemas = {getCacheSettingSchema("port1", "host1", "secret1", true), getCacheSettingSchema("port2", "host2", "secret2", false)};
    private final CacheSettingDoc[] docs = {getCacheSettingDoc(ids[0], cacheSettingSchemas[0]), getCacheSettingDoc(ids[1], cacheSettingSchemas[1])};
    @Mock
    private CosmosDBCacheSettings db;

    @InjectMocks
    private RedisCacheSettingsImpl sut;

    @Before
    public void init() {
        assertEquals(ids.length, docs.length);
        for (int i = 0; i < ids.length; i++) {
            doReturn(Optional.of(docs[i])).when(db).findById(ids[i]);
        }
    }

    @Test
    public void testAdd() {
        sut.add(cacheSettingSchemas[0], ids[0]);
        sut.add(cacheSettingSchemas[1], ids[1]);

        ArgumentCaptor<CacheSettingDoc> docArgumentCaptor = ArgumentCaptor.forClass(CacheSettingDoc.class);

        verify(db, times(2)).save(docArgumentCaptor.capture());
        List<CacheSettingDoc> obtainedDocs = docArgumentCaptor.getAllValues();
        for (int i = 0; i < obtainedDocs.size(); ++i) {
            assertEquals(obtainedDocs.get(i).getId(), ids[i]);
            assertEquals(obtainedDocs.get(i).getSettingSchema(), cacheSettingSchemas[i]);
        }
    }

    @Test
    public void testGet_whenExistingIdGiven() {
        for (int i = 0; i < ids.length; ++i) {
            CacheSettingSchema schema = sut.get(ids[i]);
            assertEquals(schema.getPort(), cacheSettingSchemas[i].getPort());
        }
    }

    @Test
    public void testGet_whenNonExistingIdGiven() {
        assertNull(sut.get("id-that-does-not-exist"));
        assertNull(sut.get(""));
    }

    private CacheSettingSchema getCacheSettingSchema(String port, String host, String usernameAndPassword, boolean isHttps) {
        CacheSettingSchema cacheSettingSchema = new CacheSettingSchema();
        cacheSettingSchema.setPort(port);
        cacheSettingSchema.setHost(host);
        cacheSettingSchema.setUsernameAndPassword(usernameAndPassword);
        cacheSettingSchema.setHttps(isHttps);
        return cacheSettingSchema;
    }

    private CacheSettingDoc getCacheSettingDoc(String id, CacheSettingSchema cacheSettingSchema) {
        CacheSettingDoc cacheSettingDoc = new CacheSettingDoc();
        cacheSettingDoc.setId(id);
        cacheSettingDoc.setSettingSchema(cacheSettingSchema);
        return cacheSettingDoc;
    }
}
