package org.elasticsearch.redis.test;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.Test;
import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisDataException;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.*;

@ElasticsearchIntegrationTest.ClusterScope(transportClientRatio = 0.0, numNodes = 1, scope = ElasticsearchIntegrationTest.Scope.TEST)
public class RedisTransportTests extends ElasticsearchIntegrationTest {

    @Test
    public void test_standard() throws IOException {
        Settings.Builder builder = ImmutableSettings.builder().put("redis.response.set", "standard")
                .put("redis.response.del", "standard");
        client().admin().cluster().prepareUpdateSettings().setTransientSettings(builder).get();
        JedisPool pool = new JedisPool(new GenericObjectPool.Config(), "localhost", 6379, 1000000);
        Jedis jedis = pool.getResource();

        // test set
        String setResult = jedis.set("/test/person/1", jsonBuilder().startObject().field("test", "value").endObject().string());

        assertThat(setResult, equalTo("OK"));

        // test put
        setResult = jedis.set("/test/person/1",  "put" + jsonBuilder().startObject().field("test", "value").endObject().string());

        assertThat(setResult, equalTo("OK"));

        // test get
        String getResult = jedis.get("/test/person/1");
        assertThat(getResult, containsString("\"_index\":\"test\""));
        assertThat(getResult, containsString("\"_type\":\"person\""));
        assertThat(getResult, containsString("\"_id\":\"1\""));
        assertThat(getResult, containsString("\"found\":true"));
        assertThat(getResult, containsString("{\"test\":\"value\"}"));

        // test exist
        boolean existResult = jedis.exists("/test/person/1");
        assertThat(existResult, equalTo(true));

        // test delete
        Long deleteResult = jedis.del("/test/person/1");
        assertThat(deleteResult, equalTo(1l));

        // should not exist after delete
        boolean notExistResult = jedis.exists("/test/person/1");
        assertThat(notExistResult, equalTo(false));

        // should throw exception for unknown command
        try {
            jedis.mget("test");
        } catch (JedisDataException ex) {
            assertThat(ex.getMessage(), containsString("Not supported command"));
        }
    }

    @Test
    public void test_json() throws IOException {
        Settings.Builder builder = ImmutableSettings.builder().put("redis.response.set", "json")
                .put("redis.response.del", "json");
        client().admin().cluster().prepareUpdateSettings().setTransientSettings(builder).get();
        JedisPool pool = new JedisPool(new GenericObjectPool.Config(), "localhost", 6379, 1000000);
        Jedis jedis = pool.getResource();

        // test set
        String setResult = jedis.set("/test/person/1", jsonBuilder().startObject().field("test", "value").endObject().string());

        assertThat(setResult, containsString("\"created\":true"));
        assertThat(setResult, containsString("\"_index\":\"test\""));
        assertThat(setResult, containsString("\"_type\":\"person\""));
        assertThat(setResult, containsString("\"_id\":\"1\""));

        Client jedisClient = jedis.getClient();

        // test delete
        jedisClient.del("/test/person/1");
        String deleteResult = jedisClient.getBulkReply();
        assertThat(deleteResult, containsString("\"found\":true"));
        assertThat(deleteResult, containsString("\"_index\":\"test\""));
        assertThat(deleteResult, containsString("\"_type\":\"person\""));
        assertThat(deleteResult, containsString("\"_id\":\"1\""));


    }
}

