package com.epages.restdocs.openapi.sample;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static lombok.AccessLevel.PRIVATE;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.data.rest.webmvc.RestMediaTypes.JSON_PATCH_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@FieldDefaults(level = PRIVATE)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
public class ProductRestIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    private ConstrainedFields fields = new ConstrainedFields(Product.class);


    @Test
    @SneakyThrows
    public void should_get_products() {
        givenProduct();
        givenProduct("Fancy Shirt", "15.10");
        givenProduct("Fancy Shoes", "75.95");

        whenProductsAreRetrieved();

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.products", hasSize(2)))
                .andDo(document("products-get", responseFields(
                        subsectionWithPath("_embedded.products[].name").description("The name of the product."),
                        fieldWithPath("_embedded.products[].price").description("The price of the product."),
                        subsectionWithPath("_embedded.products[]._links").description("The product links."),
                        fieldWithPath("page.size").description("The size of one page."),
                        fieldWithPath("page.totalElements").description("The total number of elements found."),
                        fieldWithPath("page.totalPages").description("The total number of pages."),
                        fieldWithPath("page.number").description("The current page number."),
                        fieldWithPath("page").description("Paging information"),
                        subsectionWithPath("_links").description("Links section")),
                        links(
                                linkWithRel("first").description("Link to the first page"),
                                linkWithRel("next").description("Link to the next page"),
                                linkWithRel("last").description("Link to the next page"),
                                linkWithRel("self").ignored(),
                                linkWithRel("profile").ignored()
                        ),
                        requestParameters(
                                parameterWithName("page").description("The page to be requested."),
                                parameterWithName("size").description("Parameter determining the size of the requested page."),
                                parameterWithName("sort").description("Information about sorting items.")
                        )))
        ;
    }

    @Test
    @SneakyThrows
    public void should_get_product() {
        givenProduct();

        whenProductIsRetrieved();

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("name", notNullValue()))
                .andExpect(jsonPath("price", notNullValue()))
                .andDo(document("product-get", responseFields(
                        fields.withPath("name").description("The name of the product."),
                        fields.withPath("price").description("The price of the product."),
                        subsectionWithPath("_links").description("Links section")
                )))
        ;
    }

    @Test
    @SneakyThrows
    public void should_create_product() {
        givenProductPayload();

        whenProductIsCreated();

        resultActions
                .andExpect(status().isCreated())
                .andDo(document("products-create", requestFields(
                        fields.withPath("name").description("The name of the product."),
                        fields.withPath("price").description("The price of the product.")
                )))
        ;
    }

    @Test
    @SneakyThrows
    public void should_update_product() {
        givenProduct();
        givenProductPayload("Updated name", "12.12");

        whenProductIsPatched();

        resultActions
                .andExpect(status().isOk())
                .andDo(document("product-patch", requestFields(
                        fields.withPath("name").description("The name of the product."),
                        fields.withPath("price").description("The price of the product.")
                )))
        ;
    }

    @Test
    @SneakyThrows
    public void should_fail_to_update_product_with_negative_price() {
        givenProduct();
        givenProductPayload("Updated name", "-12.12");

        whenProductIsPatched();

        resultActions
                .andExpect(status().isBadRequest())
        ;
    }

    @Test
    @SneakyThrows
    public void should_partially_update_product() {
        givenProduct();
        givenPatchPayload();

        whenProductIsPatchedJsonPatch();

        resultActions
                .andExpect(status().isOk())
                .andDo(document("product-patch-json-patch", requestFields(
                        fields.withPath("[].op").description("Patch operation."),
                        fields.withPath("[].path").description("The path of the field."),
                        fields.withPath("[].value").description("The value to assign.")
                )))
        ;
    }

    @SneakyThrows
    private void givenPatchPayload() {
        json = objectMapper.writeValueAsString(
                ImmutableList.of(
                        ImmutableMap.of(
                                "op", "replace",
                                "path", "/name",
                                "value", "Fancy socks"
                        )
                )
        );
    }

    @SneakyThrows
    private void whenProductIsRetrieved() {
        resultActions = mockMvc.perform(get("/products/{id}", productId));
    }

    @SneakyThrows
    private void whenProductIsPatched() {
        resultActions = mockMvc.perform(patch("/products/{id}", productId)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .content(json));
    }

    @SneakyThrows
    private void whenProductIsPatchedJsonPatch() {
        resultActions = mockMvc.perform(patch("/products/{id}", productId)
                .contentType(JSON_PATCH_JSON)
                .accept(APPLICATION_JSON)
                .content(json));
    }

    @SneakyThrows
    private void whenProductsAreRetrieved() {
        resultActions = mockMvc.perform(get("/products")
                .param("page", "0")
                .param("size", "2")
                .param("sort", "name asc"))
                .andDo(print());
    }
}
