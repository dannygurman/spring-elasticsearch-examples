package spring.examples.elasticsearch.controllers;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import spring.examples.elasticsearch.model.Product;
import spring.examples.elasticsearch.services.ProductService;

import java.util.List;
import java.util.Optional;

//Call example - http://127.0.0.1:9999/products/1
@RestController
@RequestMapping(value = "/products")
public class ProductsController {

    @Autowired
    private ProductService productService;

    //-------------------------
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

    //------------------------------
    //DELETE http://127.0.0.1:9999/products/
    @DeleteMapping
    public void deleteAll() {
        productService.deleteAll();
    }

    //------------------------------------
    @GetMapping()
    public Page<Product>  findAll() {
        Page<Product> products = productService.findAll();
        return products;
    }

    //------------------------------------
    //Call example - http://127.0.0.1:9999/products/1
    @GetMapping(path = "{id}")
    public Optional<Product> findById(@PathVariable(value = "id") String id) {
        return productService.findById(id);
    }

    @GetMapping(path = "/byname/{name}")
    public List<Product> findByName(@PathVariable(value = "name") String name) {
        return productService.findByName(name);
    }
}
