package spring.examples.elasticsearch.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.examples.elasticsearch.model.Product;
import spring.examples.elasticsearch.services.ProductServiceWithESRestTemplate;

import java.util.List;

//Call example - http://127.0.0.1:9999/products/v2/1
@RestController
@RequestMapping(value = "/products/v2")
public class ProductsV2Controller {

    private ProductServiceWithESRestTemplate productService;

    @PostMapping
    /**
     * @Return - document id
     */
    public String save(@RequestBody Product product) {
        return productService.index(product);
    }

    public List<String> saveBulk(@RequestBody List<Product> products) {
        return productService.bulkIndex(products);
    }
}
