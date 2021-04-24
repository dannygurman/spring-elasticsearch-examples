package spring.examples.elasticsearch.controllers;

import org.springframework.web.bind.annotation.*;
import spring.examples.elasticsearch.model.Product;
import spring.examples.elasticsearch.services.ProductServiceWithESRestTemplate;

import java.util.List;

//Call example - http://127.0.0.1:9999/products/v2/1
@RestController
@RequestMapping(value = "/products/v2")
public class ProductsV2Controller {

    private ProductServiceWithESRestTemplate productService;

    @PostMapping(path = "/createindex")
    public boolean createIndex() {
        return productService.createIndex();
    }

    @DeleteMapping(path = "/deleteindex")
    public boolean deleteIndex() {
        return productService.deleteIndex();
    }

    /**
     * @Return - document id
     */
    @PostMapping(path = "/save")
    public String saveItem(@RequestBody Product product) {
        return productService.indexItem(product);
    }

    @PostMapping(path = "/savebulk")
    public List<String> saveItemsBulk(@RequestBody List<Product> products) {
        return productService.bulkIndexItem(products);
    }

    @GetMapping(path = "/byname/{id}")
    public Product findById(@PathVariable(value = "id") String id) {
        return productService.findById(id);
    }

    @GetMapping(path = "/byname/{name}")
    public List<Product> findByName(@PathVariable(value = "name") String name) {
        return productService.findByName(name);
    }
}
