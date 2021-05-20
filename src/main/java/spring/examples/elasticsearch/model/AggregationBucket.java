package spring.examples.elasticsearch.model;

//Internal class represent aggregation bucket - spring ES bucketwrapper

import lombok.Data;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation.Bucket;

@Data
public class AggregationBucket {
    private String key;
    private long docsCount;

    public AggregationBucket(Bucket bucket) {
        this.key = bucket.getKeyAsString();
        this.docsCount = bucket.getDocCount();
    }
}
