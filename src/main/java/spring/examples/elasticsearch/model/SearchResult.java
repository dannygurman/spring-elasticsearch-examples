package spring.examples.elasticsearch.model;

import lombok.Data;
import org.springframework.data.elasticsearch.core.SearchHit;

@Data
public class SearchResult<T extends Entity> {

    private final float score;
    private final T content;

    public SearchResult(SearchHit<T> searchHit) {
        this.score = searchHit.getScore();
        this.content = searchHit.getContent();
    }

    @Override
    public String toString() {
        return "SearchResult --- : {" +
                "score=" + score +
                ", id =" + content.getId() +
                '}';
    }
}
