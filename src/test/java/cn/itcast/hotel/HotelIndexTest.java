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
        //1.����Request����
        CreateIndexRequest request = new CreateIndexRequest("hotel");
        // 2.׼������Ĳ�����DSL���
        //��鴴������䲻�ԣ�Ӧ����hotel�ģ���Ҫ�޸�һ��
        request.source(HotelConstants.MAPPING_TEMPLATE, XContentType.JSON);
        //3. ��������
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
    //�����ǹ����ĵ�����Ĳ���
    @Test
    void testAddDocument() throws IOException {
        //����id��ѯ���ݿ�����
        Hotel hotel = hotelService.getById(36934l);
        HotelDoc hotelDoc = new HotelDoc(hotel);

        //1.׼��request����
        IndexRequest request = new IndexRequest("hotel1").id(hotel.getId()+"");
        //2.׼��JSON�ĵ�
        request.source(JSON.toJSONString(hotelDoc),XContentType.JSON);
        //3.��������
        client.index(request,RequestOptions.DEFAULT);
    }
    @Test
    void testGetDocumentById() throws IOException {
        //����request����
        GetRequest request = new GetRequest("hotel","1");
        //��������õ����
        GetResponse response = client.get(request,RequestOptions.DEFAULT);
        //�������
        String json = response.getSourceAsString();

        HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);

        System.out.println(hotelDoc);
    }
//���·�Ϊ���֣�һ����ȫ�ָ��£�һ���Ǿֲ����£�������ʾ���ǵڶ��־ֲ�����
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
        //1.����Request
        BulkRequest request = new BulkRequest();
        //2.׼����������Ӷ��������Request
        for (HotelDoc hotelDoc : hotelsDoc) {
            request.add(new IndexRequest("hotel").id(hotelDoc.getId().toString()).source(JSON.toJSONString(hotelDoc),XContentType.JSON));
        }
        //3.��������
        client.bulk(request,RequestOptions.DEFAULT);
    }

    @Test
    void testMatchAll() throws IOException {
        //1.׼��request
        SearchRequest request  = new SearchRequest("hotel");
        //2.��֯DSL����
        request.source().query(QueryBuilders.matchAllQuery());
        //3.�������󣬵õ���Ӧ���
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.�������
        SearchHits hits = response.getHits();
        System.out.println("��������"+hits.getTotalHits().value+"������");

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
        //1.׼��request
        SearchRequest request  = new SearchRequest("hotel");
        //2.��֯DSL����
        request.source().query(QueryBuilders.matchQuery("all","���"));
        //3.�������󣬵õ���Ӧ���
        handleResponse(request);
    }
    @Test
    void testMultiatch() throws IOException {
        //1.׼��request
        SearchRequest request  = new SearchRequest("hotel");
        //2.��֯DSL����
        request.source().query(QueryBuilders.multiMatchQuery("���","brand","name"));
        //3.�������󣬵õ���Ӧ���
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
        //1.׼��Request
        SearchRequest request = new SearchRequest("hotel");

        //2.׼��DSL
        //2.1query
        request.source().query(QueryBuilders.matchQuery("all","���"));
        //2.2����
        request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false));

        handleResponse(request);
    }
    private void handleResponse(SearchRequest request) throws IOException {
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.�������
        SearchHits hits = response.getHits();
        System.out.println("��������"+hits.getTotalHits().value+"������");

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
