package spring.examples.elasticsearch.services;

import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import spring.examples.elasticsearch.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static spring.examples.elasticsearch.config.IndexConsts.PRODUCTS_INDEX_NAME;

@Service
public class ProductServiceWithESRestTemplate {

    private final IndexCoordinates indexCoordinates = IndexCoordinates.of(PRODUCTS_INDEX_NAME);
    private final Class<Product> productClazz = Product.class;
    private final String FIELD_NAME  = "name";


    // ElasticsearchRestTemplate implements the interface ElasticsearchOperations,
    // which does the heavy lifting for low-level search and cluster actions.

    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    public ProductServiceWithESRestTemplate(final ElasticsearchOperations elasticsearchOperations) {
        super();
        this.elasticsearchOperations = elasticsearchOperations;
    }


    public String index(Product product) {
//Index an object. Will do save or update.
        IndexQuery indexQuery = new IndexQueryBuilder()
                .withId(product.getId())
                .withObject(product)
                .build();
        String documentId = elasticsearchOperations.index(indexQuery, indexCoordinates);

        return documentId;
    }

    public List<String> bulkIndex(final List<Product> products) {
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

    /*
    Using NativeQuery
    NativeQuery provides the maximum flexibility for building a query using objects representing Elasticsearch
     constructs like aggregation, filter, and sort. Here is a NativeQuery for
    searching products matching a particular name:
     */
    public List<Product> findByName(String name) {
        MatchQueryBuilder queryBuilder = QueryBuilders.matchQuery(FIELD_NAME, name);
        Query searchQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder).build();
        SearchHits<Product> productHits = elasticsearchOperations.search(searchQuery, productClazz, indexCoordinates);
        List<Product> products = getProducts(productHits);
        return products;
    }


    private List<Product> getProducts(SearchHits<Product> productHits) {
        if (productHits.isEmpty()){
            return new ArrayList<>();
        }
        Function <SearchHit<Product>, Product> mapper =  hit -> hit.getContent();
        return   productHits.stream().map( mapper).collect(Collectors.toList());
    }

}
