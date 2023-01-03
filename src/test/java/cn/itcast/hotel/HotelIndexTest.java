package cn.itcast.hotel;

import cn.itcast.hotel.constants.HotelConstants;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.apache.lucene.util.QueryBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class HotelIndexTest {
    private RestHighLevelClient client;

    @Autowired
    private IHotelService hotelService;
    @BeforeEach
    public void before(){
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.56.100:9200")
        ));
    }
    @Test
    void createHotelIndex() throws IOException {
        //1.创建Request对象
        CreateIndexRequest request = new CreateIndexRequest("hotel");
        // 2.准备请求的参数：DSL语句
        //这块创建的语句不对，应该是hotel的，需要修改一下
        request.source(HotelConstants.MAPPING_TEMPLATE, XContentType.JSON);
        //3. 发送请求
        client.indices().create(request, RequestOptions.DEFAULT);
    }
    @Test
     void testDeleteHotelIndex() throws IOException {
        DeleteIndexRequest request = new DeleteIndexRequest("hotel");
        client.indices().delete(request, RequestOptions.DEFAULT);
    }
    @Test
    void testExistHotelIndex() throws IOException {
        GetIndexRequest request = new GetIndexRequest("hotel1");
        Boolean exist = client.indices().exists(request, RequestOptions.DEFAULT);
        System.out.println(exist);
    }
    //以下是关于文档方面的操作
    @Test
    void testAddDocument() throws IOException {
        //根据id查询数据库数据
        Hotel hotel = hotelService.getById(36934l);
        HotelDoc hotelDoc = new HotelDoc(hotel);

        //1.准备request对象
        IndexRequest request = new IndexRequest("hotel1").id(hotel.getId()+"");
        //2.准备JSON文档
        request.source(JSON.toJSONString(hotelDoc),XContentType.JSON);
        //3.发送请求
        client.index(request,RequestOptions.DEFAULT);
    }
    @Test
    void testGetDocumentById() throws IOException {
        //创建request对象
        GetRequest request = new GetRequest("hotel","1");
        //发送请求得到结果
        GetResponse response = client.get(request,RequestOptions.DEFAULT);
        //解析结果
        String json = response.getSourceAsString();

        HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);

        System.out.println(hotelDoc);
    }
//更新分为两种，一种是全局更新，一种是局部更新，底下演示的是第二种局部更新
    @Test
    void testUpdateDocumentByID() throws IOException {
        UpdateRequest request = new UpdateRequest("hotel1","36934l");
        request.doc(
                "age",18,
                "name","Rose"
        );

        client.update(request,RequestOptions.DEFAULT);
    }
    @Test
    void testDeleteDocumentById() throws IOException {
        DeleteRequest request = new DeleteRequest("hotel1","36934l");
        client.delete(request,RequestOptions.DEFAULT);
    }
    @Test
    void testBulkRequest() throws IOException {

        List<Hotel> hotels = hotelService.list();
        List<HotelDoc> hotelsDoc = new ArrayList<>();
        hotels.stream().forEach(o->hotelsDoc.add(new HotelDoc(o)));
        //1.创建Request
        BulkRequest request = new BulkRequest();
        //2.准备参数，添加多个新增的Request
        for (HotelDoc hotelDoc : hotelsDoc) {
            request.add(new IndexRequest("hotel").id(hotelDoc.getId().toString()).source(JSON.toJSONString(hotelDoc),XContentType.JSON));
        }
        //3.发送请求
        client.bulk(request,RequestOptions.DEFAULT);
    }

    @Test
    void testMatchAll() throws IOException {
        //1.准备request
        SearchRequest request  = new SearchRequest("hotel");
        //2.组织DSL参数
        request.source().query(QueryBuilders.matchAllQuery());
        //3.发送请求，得到响应结果
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.解析结果
        SearchHits hits = response.getHits();
        System.out.println("共搜索到"+hits.getTotalHits().value+"条数据");

        SearchHit[] hits1 = hits.getHits();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            System.out.println(hotelDoc);
        }
        System.out.println(response);
    }
    @Test
    void testMatch() throws IOException {
        //1.准备request
        SearchRequest request  = new SearchRequest("hotel");
        //2.组织DSL参数
        request.source().query(QueryBuilders.matchQuery("all","如家"));
        //3.发送请求，得到响应结果
        handleResponse(request);
    }
    @Test
    void testMultiatch() throws IOException {
        //1.准备request
        SearchRequest request  = new SearchRequest("hotel");
        //2.组织DSL参数
        request.source().query(QueryBuilders.multiMatchQuery("如家","brand","name"));
        //3.发送请求，得到响应结果
        handleResponse(request);

    }
//    @Test
//    void testDistance(){
//        SearchRequest request  = new SearchRequest("hotel");
//        request.source().query(QueryBuilders.geoDistanceQuery("loction").d);
//
//    }

    @Test
    void testSortAndPage() throws IOException {
        SearchRequest request  = new SearchRequest("hotel");
        request.source().query(QueryBuilders.matchAllQuery());
        request.source().sort("price", SortOrder.DESC);
        request.source().from(15).size(10);

        handleResponse(request);
    }
    @Test
    void testHighlight() throws IOException {
        //1.准备Request
        SearchRequest request = new SearchRequest("hotel");

        //2.准备DSL
        //2.1query
        request.source().query(QueryBuilders.matchQuery("all","如家"));
        //2.2高亮
        request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false));

        handleResponse(request);
    }
    private void handleResponse(SearchRequest request) throws IOException {
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.解析结果
        SearchHits hits = response.getHits();
        System.out.println("共搜索到"+hits.getTotalHits().value+"条数据");

        SearchHit[] hits1 = hits.getHits();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();

            if(!CollectionUtils.isEmpty(highlightFields)){
                HighlightField highlightField = highlightFields.get("name");
                if(highlightField!=null){
                    String name = highlightField.getFragments()[0].string();
                    hotelDoc.setName(name);
                }
            }
            System.out.println("hotelDoc  = "+ hotelDoc);
        }
    }

    @Test
    void getObjectById(){
        Hotel hotel = hotelService.getById(36934l);
        System.out.println(hotel);
    }
    @AfterEach
    public void after() throws IOException {
        this.client.close();
    }

}
