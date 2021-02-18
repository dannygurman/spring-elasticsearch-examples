package spring.examples.elasticsearch.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//Call example - http://127.0.0.1:9999/products/v2/1
@RestController
@RequestMapping(value = "/products/v2")
public class ProductsV2Controller {
}
