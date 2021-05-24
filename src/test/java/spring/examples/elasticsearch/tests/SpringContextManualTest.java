package spring.examples.elasticsearch.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import spring.examples.elasticsearch.config.ElasticsearchConfig;

/**
 * This Manual test requires: Elasticsearch instance running on localhost:9200.
 *
 * The following docker command can be used: docker run.txt -d --name es762 -p
 * 9200:9200 -e "discovery.type=single-node" elasticsearch:7.6.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ElasticsearchConfig.class)
public class SpringContextManualTest {

    @Test
    public void whenSpringContextIsBootstrapped_thenNoExceptions() {
    }
}