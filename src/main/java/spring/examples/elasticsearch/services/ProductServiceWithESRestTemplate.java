package spring.examples.elasticsearch.services;

import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.stereotype.Service;
import spring.examples.elasticsearch.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static spring.examples.elasticsearch.config.IndexConsts.*;

@Service
public class ProductServiceWithESRestTemplate {

    private static final IndexCoordinates indexCoordinates = IndexCoordinates.of(PRODUCTS_INDEX_NAME);
    private static final Class<Product> productClazz = Product.class;
    private static final Query MATCH_ALL_QUERY = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchAllQuery()).build();

    // ElasticsearchRestTemplate implements the interface ElasticsearchOperations,
    // which does the heavy lifting for low-level search and cluster actions.

    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    public ProductServiceWithESRestTemplate(final ElasticsearchOperations elasticsearchOperations) {
        super();
        this.elasticsearchOperations = elasticsearchOperations;
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
       return elasticsearchOperations.indexOps(productClazz);
    }

    public long count() {
        Query searchQuery = new NativeSearchQueryBuilder().build();
        return elasticsearchOperations.count(searchQuery, productClazz);
    }

    public String indexItem(Product product) {
//Index an object. Will do save or update.
        IndexQuery indexQuery = new IndexQueryBuilder()
                .withId(product.getId())
                .withObject(product)
                .build();
        String documentId = elasticsearchOperations.index(indexQuery, indexCoordinates);

        return documentId;
    }

    public List<String> bulkIndexItem(final List<Product> products) {
        final Function <Product,IndexQuery> productToIndexQueryMapper = product -> new IndexQueryBuilder()
                .withId(product.getId())
                .withObject(product)
                .build();

        List<IndexQuery> queries = products.stream()
                .map(productToIndexQueryMapper)
                .collect(Collectors.toList());
        return elasticsearchOperations.bulkIndex(queries, indexCoordinates);
    }

    public  Product findById(String id) {
        return elasticsearchOperations.get(id, productClazz);
    }

    public void deleteAllItems() {
        elasticsearchOperations.delete(MATCH_ALL_QUERY, productClazz, indexCoordinates);
    }


    public List<Product> findByName(String name) {
        return findByValue(PRODUCT_FIELD_NAME, name);
    }

    public List<Product> findByCategory(String category) {
        return findByValue(PRODUCT_FIELD_CATEGORY, category);
    }

    public List<Product> findByCategoryKeyword(String category) {
        return findByValue(PRODUCT_FIELD_CATEGORY + "." + PRODUCTS_CATEGORY_SUFFIX , category);
    }

    private List<Product> findByValue(String fieldName, String value) {
        Query searchQuery = createBasicMatchQuery(fieldName, value);
        SearchHits<Product> productHits = elasticsearchOperations.search(searchQuery, productClazz, indexCoordinates);
        List<Product> products = getProducts(productHits);
        return products;
    }

    private NativeSearchQuery createBasicMatchQuery(String fieldName, String value) {
      /*  Using NativeQuery
        NativeQuery provides the maximum flexibility for building a query using objects representing Elasticsearch
        constructs like aggregation, filter, and sort. */
        MatchQueryBuilder queryBuilder = QueryBuilders.matchQuery(fieldName, value);
        return new NativeSearchQueryBuilder().withQuery(queryBuilder).build();
    }

    private List<Product> getProducts(SearchHits<Product> productHits) {
        if (productHits.isEmpty()){
            return new ArrayList<>();
        }
        Function<SearchHit<Product>, Product> mapper = hit -> hit.getContent();
        return productHits.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }

}
