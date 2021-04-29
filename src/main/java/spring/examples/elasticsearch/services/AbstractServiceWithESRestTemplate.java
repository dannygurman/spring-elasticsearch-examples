package spring.examples.elasticsearch.services;

import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import spring.examples.elasticsearch.model.Entity;
import spring.examples.elasticsearch.model.QueryResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static spring.examples.elasticsearch.config.IndexConsts.*;

public abstract class AbstractServiceWithESRestTemplate<T extends Entity> {

    private final Class<T> itemClazz;
    private IndexCoordinates indexCoordinates;

    // ElasticsearchRestTemplate implements the interface ElasticsearchOperations,
    // which does the heavy lifting for low-level search and cluster actions.
    private ElasticsearchOperations elasticsearchOperations;

    public AbstractServiceWithESRestTemplate(Class<T> itemClazz, final ElasticsearchOperations elasticsearchOperations) {
        this.itemClazz = itemClazz;
        this.elasticsearchOperations = elasticsearchOperations;
    }


    private static final Query MATCH_ALL_QUERY = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchAllQuery()).build();


    protected abstract String getIndexName();

    public IndexCoordinates getIndexCoordinates(){
        if (indexCoordinates == null){
            indexCoordinates = IndexCoordinates.of(getIndexName());
        }
        return indexCoordinates;
    }


    public boolean isExists() {
        return getIndexOperations().exists();
    }

    public boolean createIndex() {
        return getIndexOperations().create();
    }

    public boolean deleteIndex() {
        return getIndexOperations().delete();
    }

    //From ES doc:
    // A refresh makes all operations performed on an index since
    // the last refresh available for search.
    public void refresh() {
        getIndexOperations().refresh();
    }

    private Map getMapping() {
        return getIndexOperations().getMapping();
    }

    //Creates the index mapping for the entity this IndexOperations is bound to.
    private Document createMapping () {
        return getIndexOperations().createMapping();
    }

    //writes a mapping to the index
    public boolean putMapping () {
        return getIndexOperations().putMapping(createMapping());
    }

    public String mappingToString() {
        StringBuffer  sb = new StringBuffer(" ------ Index mapping:\n");
        Map mapping = getMapping();
        mapping.forEach((key, val) -> {
            sb.append("key:");
            sb.append(key);
            sb.append(" val:");
            sb.append(val);
            sb.append("\n");
        });
        return sb.toString();
    }

    private IndexOperations getIndexOperations(){
        return elasticsearchOperations.indexOps(itemClazz);
    }

    public long count() {
        Query searchQuery = new NativeSearchQueryBuilder().build();
        return elasticsearchOperations.count(searchQuery, itemClazz);
    }

    public String indexItem(T item) {
//Index an object. Will do save or update.
        IndexQuery indexQuery = new IndexQueryBuilder()
                .withId(item.getId())
                .withObject(item)
                .build();
        String documentId = elasticsearchOperations.index(indexQuery, getIndexCoordinates());

        return documentId;
    }

    public List<String> bulkIndexItem(final List<T> items) {
        final Function<T,IndexQuery>itemToIndexQueryMapper = item -> new IndexQueryBuilder()
                .withId(item.getId())
                .withObject(item)
                .build();

        List<IndexQuery> queries = items.stream()
                .map(itemToIndexQueryMapper)
                .collect(Collectors.toList());
        return elasticsearchOperations.bulkIndex(queries, getIndexCoordinates());
    }

    public  T findById(String id) {
        return elasticsearchOperations.get(id, itemClazz);
    }

    public void deleteAllItems() {
        elasticsearchOperations.delete(MATCH_ALL_QUERY, itemClazz, getIndexCoordinates());
    }


    protected List<T> findByValue(String fieldName, String value) {
        Query searchQuery = createBasicMatchQuery(fieldName, value);
        SearchHits<T> searchHits = elasticsearchOperations.search(searchQuery, itemClazz, getIndexCoordinates());
        List<T> items = getItems(searchHits);
        return items;
    }

    protected NativeSearchQuery createBasicMatchQuery(String fieldName, String value) {
      /*  Using NativeQuery
        NativeQuery provides the maximum flexibility for building a query using objects representing Elasticsearch
        constructs like aggregation, filter, and sort. */
        MatchQueryBuilder queryBuilder = createMatchQueryBuilder(fieldName, value) ;
        return new NativeSearchQueryBuilder().withQuery(queryBuilder).build();
    }

    private MatchQueryBuilder createMatchQueryBuilder(String fieldName, String value) {
        return QueryBuilders
                .matchQuery(fieldName, value);
        // .fuzziness(Fuzziness.ONE)
        //    .operator(Operator.AND)
        // .prefixLength(3);
    }

    private List<QueryResponse<T>> getQueryResponses(SearchHits<T> searchHits) {
        if (searchHits.isEmpty()){
            return new ArrayList<>();
        }
        return searchHits.stream()
                .map(QueryResponse::new)
                .collect(Collectors.toList());
    }

    private List<T> getItems(SearchHits<T> searchHits) {
        return getQueryResponses(searchHits)
                .stream()
                .map(res -> res.getContent()).collect(Collectors.toList());
    }
}
