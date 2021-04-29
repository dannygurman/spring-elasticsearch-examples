package spring.examples.elasticsearch.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import spring.examples.elasticsearch.model.Product;

import java.util.List;

import static spring.examples.elasticsearch.config.IndexConsts.*;

@Service
public class ProductServiceWithESRestTemplate extends AbstractServiceWithESRestTemplate<Product> {

    @Autowired
    public ProductServiceWithESRestTemplate(final ElasticsearchOperations elasticsearchOperations) {
        super(Product.class , elasticsearchOperations);
    }

    @Override
    protected String getIndexName() {
        return PRODUCTS_INDEX_NAME;
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


}
