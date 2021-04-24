package spring.examples.elasticsearch.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import spring.examples.elasticsearch.model.Product;
import spring.examples.elasticsearch.repositories.ProductRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceWithRepository {

    @Autowired
    private ProductRepository productRepository;

    public long count(){
        return productRepository.count();
    }

    //Also create index if not exist + save document
    //Could be used also for update
    public Product save(final Product product) {
        return productRepository.save(product);
    }

    public Iterable<Product> saveAll(final List<Product> products) {
        return productRepository.saveAll(products);
    }

    public void deleteAll() {
        productRepository.deleteAll();
    }


    public Page<Product>  findAll() {
        Page<Product>  products = (Page<Product>)productRepository.findAll();
        return products;
    }

    public Optional<Product> findById(String id) {
        return productRepository.findById(id);
    }

    public List<Product> findByName(String name) {
        return productRepository.findByName(name);
    }
}