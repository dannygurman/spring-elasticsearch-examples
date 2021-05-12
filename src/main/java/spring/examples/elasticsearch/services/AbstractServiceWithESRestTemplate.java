package spring.examples.elasticsearch.services;

import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;
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


    public List<T> findByValue(String fieldName, String value) {
        QueryBuilder matchQueryBuilder = createBasicMatchQueryBuilder(fieldName, value);
        return findByValuesInternal(matchQueryBuilder);

    }

    private MatchQueryBuilder createBasicMatchQueryBuilder(String fieldName, String value) {
        return QueryBuilders.matchQuery(fieldName, value);
    }

    public List<T> findByValue(String fieldName, String value,  Operator operator, Fuzziness fuzziness,
                                  int prefixLength) {
        QueryBuilder matchQueryBuilder = createMatchQueryBuilder(fieldName, value, operator , fuzziness, prefixLength);
        return findByValuesInternal(matchQueryBuilder);
    }

    public List<T> findPhraseByValue(String fieldName, String value,  int slop) {
        MatchPhraseQueryBuilder matchQueryBuilder = createMatchPhraseQueryBuilder(fieldName, value, slop);
        return findByValuesInternal(matchQueryBuilder);
    }

    /**
     *
     * fuzziness:  When the user makes a typo in a word, it is still possible to match it with a search by
     * specifying a fuzziness parameter, which allows inexact matching.     *
     * For string fields, fuzziness means the edit distance: the number of one-character changes that need
     * to be made to one string to make it the same as another string.
     *
     * The prefix_length parameter is used to improve performance.
     * For example if set to 3 it require that the first three characters should match exactly,
     * which reduces the number of possible combinations.
     */
    private MatchQueryBuilder createMatchQueryBuilder(String fieldName, String value, Operator operator,
                                                        Fuzziness fuzziness, int prefixLength) {
        return QueryBuilders
                .matchQuery(fieldName, value)
                .fuzziness(fuzziness)
                .operator(operator)
                .prefixLength(prefixLength);
    }

    private MatchPhraseQueryBuilder createMatchPhraseQueryBuilder(String fieldName, String value, int slop) {
        return QueryBuilders
                .matchPhraseQuery(fieldName, value)
                .slop(slop);
    }



    /*  Using NativeQuery
      NativeQuery provides the maximum flexibility for building a query using objects
       representing Elasticsearch constructs like aggregation, filter, and sort.
       */
    private List <T> findByValuesInternal(QueryBuilder queryBuilder) {
        Query searchQuery =  new NativeSearchQueryBuilder().withQuery(queryBuilder).build();
        SearchHits<T> searchHits = elasticsearchOperations.search(searchQuery, itemClazz, getIndexCoordinates());
        List<T> items = getItems(searchHits);
        return items;
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
