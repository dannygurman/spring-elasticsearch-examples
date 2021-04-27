package spring.examples.elasticsearch.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import spring.examples.elasticsearch.config.IndexConsts;

@Document(indexName = IndexConsts.PRODUCTS_INDEX_NAME)
@Builder
@Data
public class Product {

    //The primary difference between text and a keyword is that a text field will be tokenized while a keyword cannot.

    //We can use the keyword type when we want to perform filtering or sorting operations on the field.
    //Normalizers are similar to analyzers with the difference that normalizers don’t apply a tokenizer.

    @Id
    private String id;

    @Field(type = FieldType.Text, name = "name")
    private String name;

    @Field(type = FieldType.Double, name = "price")
    private Double price;

    @Field(type = FieldType.Integer, name = "quantity")
    private Integer quantity;

    @MultiField(
            mainField = @Field(name = "category",type = FieldType.Text, fielddata = true),
           // We use FieldType.keyword to indicate that we do NOT want to use an analyzer when performing the
            // additional indexing of the field.
            otherFields = {
                    @InnerField(suffix = "xxx", type = FieldType.Keyword, store = true )
            })
    private String category;

    @Field(type = FieldType.Text, name = "desc")
    private String description;

    @Field(type = FieldType.Keyword, name = "manufacturer")
    private String manufacturer;
}