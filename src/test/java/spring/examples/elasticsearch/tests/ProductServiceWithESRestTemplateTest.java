package spring.examples.elasticsearch.tests;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import spring.examples.elasticsearch.model.Product;
import spring.examples.elasticsearch.services.ProductServiceWithESRestTemplate;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class ProductServiceWithESRestTemplateTest {

    @Autowired
    private ProductServiceWithESRestTemplate productService;

    @Before
    public void setUp() {
        productService.deleteIndex();
        productService.createIndex();
        productService.refresh();

    }

    @Test
    public void whenCreateDocument_thenCountIncreased() {
        assertEquals(0 , productService.count());
        Product pr1 = Product.builder().id("id1").category("cat1").build();

        productService.indexItem(pr1);
        productService.refresh();
        sleep();

        printIndexMapping();

        assertEquals(1 , productService.count());

        productService.deleteAllItems();
        productService.refresh();
        assertEquals(0 , productService.count());

    }

    @Test
    public void whenCreateDocument_thenCountIncreased2() {
        assertEquals(0 , productService.count());
        Product pr1 = Product.builder().id("id1").category("cat1").build();

        productService.indexItem(pr1);
        sleep();
        assertEquals(1 , productService.count());
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
