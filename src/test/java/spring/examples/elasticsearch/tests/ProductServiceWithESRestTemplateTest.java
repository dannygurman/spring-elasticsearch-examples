package spring.examples.elasticsearch.tests;

import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import spring.examples.elasticsearch.model.Product;
import spring.examples.elasticsearch.model.SearchResult;
import spring.examples.elasticsearch.services.ProductServiceWithESRestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static spring.examples.elasticsearch.config.IndexConsts.PRODUCT_FIELD_MANUFACTURER;
import static spring.examples.elasticsearch.config.IndexConsts.PRODUCT_FIELD_NAME;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class ProductServiceWithESRestTemplateTest {

    private  final static String SPACE = " ";
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
        productService.refresh();
        List<Product> foundProducts = productService.findByName(name);
        assertEquals(1 , foundProducts.size());
        assertEquals(pr1 , foundProducts.get(0));
    }

    @Test
    public void whenSearchByCategory_Text_And_Keyword_thenFound() {
        assertEquals(0 , productService.count());
        String id1= "id1";
        String id2= "id2";
        String name_1 = "name1";
        String name_2 = "name2";

        String category_1 = "abc";
        String cat_part1 = "def";
        String cat_part2 = "hjk";
        String category_2 = cat_part1 + SPACE + cat_part2;

        Product pr1 = Product.builder().id(id1).name(name_1).category(category_1).build();
        Product pr2 = Product.builder().id(id2).name(name_2).category(category_2).build();

        productService.bulkIndexItem(Arrays.asList(pr1, pr2));
        productService.refresh();

        List<Product> foundProducts = productService.findByCategory(category_1);
        assertEquals(1 , foundProducts.size());

        foundProducts = productService.findByCategoryKeyword(category_1);
        assertEquals(1 , foundProducts.size());


        foundProducts = productService.findByCategory(category_2);
        assertEquals(1 , foundProducts.size());

        //Text field analyzing found part of word split by spaces
        foundProducts = productService.findByCategory(category_2);
        assertEquals(1 , foundProducts.size());


        //Keyword not found part of word
        foundProducts = productService.findByCategoryKeyword(cat_part1);
        assertEquals(0 , foundProducts.size());

        //Keyword found exact match
        foundProducts = productService.findByCategoryKeyword(category_2);
        assertEquals(1 , foundProducts.size());

    }

    @Test
    public void whenSearchWithMatchQuery_UsingOrOperator_OneTermMatch_ThenFound() {
        String name_part_1 = "aaa";
        String name_part_2 = "bbb";
        String name = name_part_1 + SPACE + name_part_2;
        String searchString = "xxx" + SPACE + name_part_2;
        verifyOneTermMatchInternal(name, searchString, Operator.OR , 1);
    }

    @Test
    public void whenSearchWithMatchQuery_UsingAndOperator_OneTermMatch_ThenNOTFound() {
        String name_part_1 = "aaa";
        String name_part_2 = "bbb";
        String name = name_part_1 + SPACE + name_part_2;
        String searchString = "xxx" + SPACE + name_part_2;
        verifyOneTermMatchInternal(name, searchString, Operator.AND , 0);
    }

    private void verifyOneTermMatchInternal(String productName, String searchString ,
                                            Operator operator , int expectedFoundCount ){
        Product pr1 = Product.builder().name(productName).build();
        productService.indexItem(pr1);
        productService.refresh();
        List<Product> foundProducts = productService.findByValue(PRODUCT_FIELD_NAME , searchString,
                operator, Fuzziness.AUTO , 0);
        assertEquals(expectedFoundCount , foundProducts.size());
    }

    @Test
    public void whenSearchWithMatchQuery_variationsInFuzzinessInDistanceLimit_ThenFound() {
        String name = "abcde";
        String searchString = "abxxe";
        Fuzziness fuzziness = Fuzziness.TWO;
        verifyMatchByFuzzinessInternal(name, searchString, fuzziness , 1);
    }

    @Test
    public void whenSearchWithMatchQuery_variationsInFuzzinessAboveDistanceLimit_ThenNOTFound() {
        String name = "abcde";
        String searchString = "abxxe";
        Fuzziness fuzziness = Fuzziness.ONE;
        verifyMatchByFuzzinessInternal(name, searchString, fuzziness , 0);
    }

    private void verifyMatchByFuzzinessInternal(String productName, String searchString ,
                                             Fuzziness fuzziness , int expectedFoundCount ){
        Product pr1 = Product.builder().name(productName).build();
        productService.indexItem(pr1);
        productService.refresh();
        List<Product> foundProducts = productService.findByValue(PRODUCT_FIELD_NAME , searchString,
                Operator.AND, fuzziness , 0);
        assertEquals(expectedFoundCount , foundProducts.size());
    }

    @Test
    public void whenSearchWithMatchQuery_prefixMatch_ThenFound() {
        String name = "abcde";
        String searchString = "abxxe";
        int  prefixLength = 2;
        verifyMatchByPrefixLengthInternal(name, searchString, prefixLength , 1);
    }

    @Test
    public void whenSearchWithMatchQuery_prefixNOTMatch_ThenNOTFound() {
        String name = "abcde";
        String searchString = "xxcde";
        int  prefixLength = 2;
        verifyMatchByPrefixLengthInternal(name, searchString, prefixLength , 0);
    }

    private void verifyMatchByPrefixLengthInternal(String productName, String searchString ,
                                                int prefixLength , int expectedFoundCount ){
        Product pr1 = Product.builder().name(productName).build();
        productService.indexItem(pr1);
        productService.refresh();
        Fuzziness fuzziness = Fuzziness.TWO;
        List<Product> foundProducts = productService.findByValue(PRODUCT_FIELD_NAME , searchString,
                Operator.AND, fuzziness , prefixLength);
        assertEquals(expectedFoundCount , foundProducts.size());
    }

    @Test
    public void whenSearchWithPhraseMatchQuery_oneSearchTerm_ThenFound() {
        String productName = "abc def ghi";
        String searchString = "abc";
        int slop = 0;
        int expected = 1;
        verifyPhraseMatch_internal(slop, productName, searchString, expected);
    }

    @Test
    public void whenSearchWithPhraseMatchQuery_withSlop_ThenFound() {
        String productName = "abc def ghi";
        String searchString = "abc ghi";
        int slop = 1;
        int expected = 1;
        verifyPhraseMatch_internal(slop, productName, searchString, expected);
    }

    @Test
    public void whenSearchWithPhraseMatchQuery_noSlop_ThenNotFound() {
        String productName = "abc def ghi";
        String searchString = "abc ghi";
        int slop = 0;
        int expected = 0;
        verifyPhraseMatch_internal(slop, productName, searchString, expected);
    }


    private void verifyPhraseMatch_internal(int slop, String productName, String searchString, int expected) {
        Product pr1 = Product.builder().name(productName).build();
        productService.indexItem(pr1);
        productService.refresh();
        List<Product> foundProducts = productService.findPhraseByValue(PRODUCT_FIELD_NAME, searchString, slop);
        assertEquals(expected, foundProducts.size());
    }


    @Test
    public void whenMultiMatchSerch_WithTwoFieldsMatch_ThenFound() {
        String productName = "product1";
        String manufacturer = productName;
        String searchString = productName;
        //“best fields” scoring strategy will take the maximum score among the fields as a document score.
        MultiMatchQueryBuilder.Type scoringStrategyType = MultiMatchQueryBuilder.Type.BEST_FIELDS;
        int expected = 1;
        verifyMultiMatch_internal(productName, manufacturer, searchString, scoringStrategyType, expected);
    }

    @Test
    public void whenMultiMatchSerch_WithOneFieldsMatch_ThenFound() {
        String productName = "product1";
        String manufacturer = "xxx";
        String searchString = productName;
        //“best fields” scoring strategy will take the maximum score among the fields as a document score.
        MultiMatchQueryBuilder.Type scoringStrategyType = MultiMatchQueryBuilder.Type.BEST_FIELDS;
        int expected = 1;
        verifyMultiMatch_internal(productName, manufacturer, searchString, scoringStrategyType, expected);
    }


    private void verifyMultiMatch_internal(String productName, String manufacturer,
                                           String searchString,
                                           MultiMatchQueryBuilder.Type scoringStrategyType,
                                           int expectedNumOfMatch) {
        Product pr1 = Product.builder()
                .name(productName)
                .manufacturer(manufacturer)
                .build();
        productService.indexItem(pr1);
        productService.refresh();
        String [] fieldsToSearchIn = {PRODUCT_FIELD_NAME, PRODUCT_FIELD_MANUFACTURER};
        List<SearchResult<Product>> results = productService.findValueByMultipleFields(fieldsToSearchIn, searchString, scoringStrategyType);
        assertEquals(expectedNumOfMatch, results.size());
        printResults(results);

    }

    private void printIndexMapping(){
        System.out.println("^^^^***********************");
        System.out.println(productService.mappingToString());
        System.out.println("^^^***********************");
    }

    private void printResults(List<SearchResult<Product>> results){
        System.out.println("^^^^^^^^^^^^ results ^^^^^^^^^^^^^^^^^^^^^^^");
        results.forEach(System.out::println);
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
    }

}
