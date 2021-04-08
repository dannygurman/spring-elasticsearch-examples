package spring.examples.elasticsearch.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;
import spring.examples.elasticsearch.config.IndexConsts;
import spring.examples.elasticsearch.model.Product;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static spring.examples.elasticsearch.config.IndexConsts.PRODUCTS_INDEX_NAME;

@Service
public class ProductServiceWithESRestTemplate {

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
        String documentId = elasticsearchOperations.index(indexQuery, IndexCoordinates.of(PRODUCTS_INDEX_NAME));

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
        return elasticsearchOperations.bulkIndex(queries, IndexCoordinates.of(PRODUCTS_INDEX_NAME));

    }
}
