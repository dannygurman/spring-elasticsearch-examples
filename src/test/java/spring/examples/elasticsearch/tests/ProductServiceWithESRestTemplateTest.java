package spring.examples.elasticsearch.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import spring.examples.elasticsearch.model.Product;
import spring.examples.elasticsearch.services.ProductServiceWithESRestTemplate;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class ProductServiceWithESRestTemplateTest {
    @Autowired
    private ProductServiceWithESRestTemplate productService;

    @Before
    public  void setUp() {
        if (productService.isExists()) {
            productService.deleteIndex();
        }

        productService.createIndex();
        productService.putMapping();

        printIndexMapping();
        productService.refresh();
    }

    @After
    public void tearDown() {
        productService.deleteAllItems();
        productService.refresh();
    }

    @Test
    public void whenCreateDocument_thenCountIncreased() {
        assertEquals(0 , productService.count());
        Product pr1 = Product.builder().id("id1").category("cat1").build();

        productService.indexItem(pr1);
        productService.refresh();
        sleep();

        assertEquals(1 , productService.count());

        productService.deleteAllItems();
        productService.refresh();
        assertEquals(0 , productService.count());

    }

    @Test
    public void whenSearchByName_thenFound() {
        assertEquals(0 , productService.count());
        String name = "name1";
        Product pr1 = Product.builder()
                .id("id1").name(name)
                .category("cat1").build();

        productService.indexItem(pr1);
        sleep();
        List<Product> foundProducts = productService.findByName(name);
        assertEquals(1 , foundProducts.size());
        assertEquals(pr1 , foundProducts.get(0));
    }

    @Test
    public void whenSearchByCategory_thenFound() {
        assertEquals(0 , productService.count());
        String name = "name1";
        Product pr1 = Product.builder()
                .id("id1").name(name)
                .category("cat1 xxx").build();

        productService.indexItem(pr1);
        sleep();
        List<Product> foundProducts = productService.findByName(name);
        assertEquals(1 , foundProducts.size());
        assertEquals(pr1 , foundProducts.get(0));
    }

    private void sleep() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void printIndexMapping(){
        System.out.println("*************************");
        System.out.println(productService.mappingToString());
        System.out.println("*************************");
    }

}
