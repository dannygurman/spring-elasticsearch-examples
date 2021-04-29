package spring.examples.elasticsearch.model;

import lombok.Data;
import org.springframework.data.elasticsearch.core.SearchHit;

@Data
public class QueryResponse <T> {

    private final float score;
    private final T content;

    public QueryResponse(SearchHit<T> searchHit) {
        this.score = searchHit.getScore();
        this.content = searchHit.getContent();
    }
}
