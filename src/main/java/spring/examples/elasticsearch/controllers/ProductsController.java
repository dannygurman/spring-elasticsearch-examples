package spring.examples.elasticsearch.controllers;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import spring.examples.elasticsearch.model.Product;
import spring.examples.elasticsearch.services.ProductService;

import java.util.Optional;

//Call example - http://127.0.0.1:9999/products/1
@RestController
@RequestMapping(value = "/products")
public class ProductsController {

    @Autowired
    private ProductService productService;

    /*
    //POST http://127.0.0.1:9999/products/
        {
                "name": " p1name"
        }
        Note - id field optional*/
    @PostMapping
    public Product save(@RequestBody Product product) {
        return productService.save(product);
    }


    //Call example - http://127.0.0.1:9999/products/1
    @GetMapping(path = "{id}")
    public Optional<Product> findById(@PathVariable(value = "id") String id) {
        return productService.findById(id);
    }
}
