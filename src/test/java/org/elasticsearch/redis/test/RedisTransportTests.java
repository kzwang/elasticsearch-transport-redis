package org.elasticsearch.redis.test;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisDataException;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.*;

@ElasticsearchIntegrationTest.ClusterScope(transportClientRatio = 0.0, numNodes = 1, scope = ElasticsearchIntegrationTest.Scope.TEST)
public class RedisTransportTests extends ElasticsearchIntegrationTest {

    @Test
    public void test() throws IOException {
        JedisPool pool = new JedisPool(new GenericObjectPool.Config(), "localhost", 6379, 1000000);
        Jedis jedis = pool.getResource();

        // test set
        String setResult = jedis.set("/test/person/1", jsonBuilder().startObject().field("test", "value").endObject().string());

        assertThat(setResult, containsString("\"ok\":true"));
        assertThat(setResult, containsString("\"_index\":\"test\""));
        assertThat(setResult, containsString("\"_type\":\"person\""));
        assertThat(setResult, containsString("\"_id\":\"1\""));

        // test get
        String getResult = jedis.get("/test/person/1");
        assertThat(getResult, containsString("\"_index\":\"test\""));
        assertThat(getResult, containsString("\"_type\":\"person\""));
        assertThat(getResult, containsString("\"_id\":\"1\""));
        assertThat(getResult, containsString("\"exists\":true"));
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
}
