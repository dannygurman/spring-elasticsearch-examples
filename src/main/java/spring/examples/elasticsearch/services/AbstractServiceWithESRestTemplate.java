package spring.examples.elasticsearch.services;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.*;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import spring.examples.elasticsearch.model.AggregationBucket;
import spring.examples.elasticsearch.model.Entity;
import spring.examples.elasticsearch.model.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        return findItemsByValuesInternal(matchQueryBuilder);

    }

    private MatchQueryBuilder createBasicMatchQueryBuilder(String fieldName, String value) {
        return QueryBuilders.matchQuery(fieldName, value);
    }

    public List<T> findByValue(String fieldName, String value,  Operator operator, Fuzziness fuzziness,
                                  int prefixLength) {
        QueryBuilder matchQueryBuilder = createMatchQueryBuilder(fieldName, value, operator , fuzziness, prefixLength);
        return findItemsByValuesInternal(matchQueryBuilder);
    }

    public List<T> findPhraseByValue(String fieldName, String value,  int slop) {
        MatchPhraseQueryBuilder matchQueryBuilder = createMatchPhraseQueryBuilder(fieldName, value, slop);
        return findItemsByValuesInternal(matchQueryBuilder);
    }

    public List<SearchResult<T>>  findValueByMultipleFields(String[] fieldsName, String valueToFind,
                                             MultiMatchQueryBuilder.Type scoringStrategyType) {
        MultiMatchQueryBuilder matchQueryBuilder = createMultiMatchPhraseQueryBuilder(fieldsName, valueToFind, scoringStrategyType);
        return findResultsByValuesInternal(matchQueryBuilder);
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

    private MultiMatchQueryBuilder createMultiMatchPhraseQueryBuilder(String[] fieldsName, String valueToFind,
                                                                      MultiMatchQueryBuilder.Type scoringStrategyType) {
        MultiMatchQueryBuilder queryBuilder = QueryBuilders
                .multiMatchQuery(valueToFind)
                .type(scoringStrategyType);
        for (String fieldName : fieldsName) {
            queryBuilder.field(fieldName);
        }
        return queryBuilder;
    }


    /*  Using NativeQuery
      NativeQuery provides the maximum flexibility for building a query using objects
       representing Elasticsearch constructs like aggregation, filter, and sort.
       */
    private List<SearchResult<T>> findResultsByValuesInternal(QueryBuilder queryBuilder) {
        Query searchQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder).build();
        SearchHits<T> searchHits =searchInternal(searchQuery);
        return getSearchResults(searchHits);
    }

    private SearchHits<T> searchInternal (Query searchQuery) {
      return elasticsearchOperations.search(searchQuery, itemClazz, getIndexCoordinates());
    }

    private List<T> findItemsByValuesInternal(QueryBuilder queryBuilder) {
        List<SearchResult<T>> results = findResultsByValuesInternal(queryBuilder);
       return getItemsFromResults(results);
    }

    private List<T>  getItemsFromResults(List<SearchResult<T>> results) {
        return results
                .stream()
                .map(res -> res.getContent())
                .collect(Collectors.toList());
    }

    private List<SearchResult<T>> getSearchResults(SearchHits<T> searchHits) {
        if (searchHits.isEmpty()) {
            return new ArrayList<>();
        }
        return searchHits.stream()
                .map(SearchResult::new)
                .collect(Collectors.toList());
    }

    public List <T>  getItemsFromResults(SearchHits<T> searchHits){
        List<SearchResult<T>> result = getSearchResults(searchHits);
        return getItemsFromResults(result);
    }


    public Aggregation getTermAggregationByField(String field, boolean isAscendingOrder,
                                                 int maxBucketsCount) {
        String termAggregationName = "my_agg_by_field_" + field;
        AggregationBuilder subAggregation = null;
        TermsAggregationBuilder termsAggregationBuilder = createTermsAggregationBuilder(termAggregationName, field,
                isAscendingOrder, maxBucketsCount, subAggregation);
        return invokeAggregationSearchInternal(termAggregationName, termsAggregationBuilder);
    }

    private Aggregation invokeAggregationSearchInternal(String termAggregationName,
                                                        TermsAggregationBuilder termsAggregationBuilder) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .addAggregation(termsAggregationBuilder)
                //.withSourceFilter(new FetchSourceFilter(new String[] { fieldName }, new String[] { "" }))//filter it to retrieve only the field we ar interested
                //  .withPageable(PageRequest.of(0, 1))
                .build();
        SearchHits<T> searchHits = searchInternal(searchQuery);
        Aggregations aggregationResult = searchHits.getAggregations();
        Map<String, Aggregation> aggregationMap = aggregationResult.asMap();
        Aggregation aggregation = aggregationMap.get(termAggregationName);
        return aggregation;
    }

    //Note - the bucket order is saved
    public List<AggregationBucket> getAggregatinBuckets(MultiBucketsAggregation multiBucketsAggregation) {
        return multiBucketsAggregation.getBuckets()
                .stream()
                .map(AggregationBucket::new)
                .collect(Collectors.toList());
    }

    private TermsAggregationBuilder createTermsAggregationBuilder(String termAggregationName,
                                                              String field,
                                                              boolean isAscendingOrder,
                                                              int maxBucketsCount,
                                                              AggregationBuilder subAggregation) {
        //	terms(java.lang.String name)- Create a new Terms aggregation with the given name.
        TermsAggregationBuilder aggregation = AggregationBuilders.terms(termAggregationName);
        aggregation
                //Sets the field to use for this aggregation.
                .field(field)
                //Order - Gets the order in which the buckets will be returned.
                .order(createBucketOrderByCount(isAscendingOrder))
       // Sets the size - indicating how many term buckets should be returned (defaults to 10);
        .size(maxBucketsCount);
        if (subAggregation != null) {
            aggregation.subAggregation(subAggregation);
        }
        return aggregation;
    }

    private BucketOrder createBucketOrderByCount(boolean isAscendingOrder) {
        //Creates a bucket ordering strategy that sorts buckets by their document counts (ascending or descending).
        return BucketOrder.count(isAscendingOrder);
    }


    public  List<T> findItemsWithFieldValuesInRange(String fieldName, Object lowerBound, Object upperBound) {
        CriteriaQuery query = buildRangeCriteriaQuery(fieldName, lowerBound, upperBound);
        SearchHits<T> searchHits = searchInternal(query);
        return getItemsFromResults(searchHits);
    }


    private CriteriaQuery buildRangeCriteriaQuery(String fieldName, Object lowerBound, Object upperBound) {
        Criteria criteria = new Criteria(fieldName)
                .greaterThanEqual(lowerBound)
                .lessThanEqual(upperBound);
        return new CriteriaQuery(criteria);
    }

    //Query text could include "*"
    public List<T> findItemsByFieldUsingWildcard(String fieldName, String query, int maxResults) {
        QueryBuilder queryBuilder = QueryBuilders.wildcardQuery(fieldName, query);
        Query searchQuery = new NativeSearchQueryBuilder()
                .withFilter(queryBuilder)
                .withPageable(PageRequest.of(0, maxResults))
                .build();
        SearchHits<T> searchHits = searchInternal(searchQuery);
        return getItemsFromResults(searchHits);
    }

}
