package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.lucene.search.BooleanQuery;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

    @Autowired
    private RestHighLevelClient client;

    @Override
    public PageResult search(RequestParams requestParams) {
        //1.׼��Request
        try {
            SearchRequest request = new SearchRequest("hotel");
            //2.׼��DSL
            //2.1�ؼ�������
            buildBasicSearch(requestParams, request);
            //2.2��ҳ
            int page = requestParams.getPage();
            int size = requestParams.getSize();
            request.source().from((page-1)*size).size(size);
            //2.3����-������������
            String location = requestParams.getLocation();
            if(location!=null&&!location.equals("")){
                request.source().sort(SortBuilders.geoDistanceSort("location",new GeoPoint(location))
                        .order(SortOrder.ASC).unit(DistanceUnit.KILOMETERS));
            }
            //3.�������󣬵õ���Ӧ
            return handleResponse(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, List<String>> filters(RequestParams requestParams) throws IOException {
        //1��׼��Request
        SearchRequest request = new SearchRequest("hotel");

        buildBasicSearch(requestParams,request);
        //2.׼��DSL
        request.source().size(0);
        buildAggregation(request);

        //3.��������
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //�������
        Aggregations aggregations = response.getAggregations();
        //���췵�ؽ����
        Map<String, List<String>> ret = new HashMap<>();

        List<String> brandList = resultExtracted(aggregations,"brand_agg");
        ret.put("Ʒ��",brandList);
        List<String> cityList = resultExtracted(aggregations,"city_agg");
        ret.put("����",cityList);
        List<String> starList = resultExtracted(aggregations,"star_agg");
        ret.put("�Ǽ�",brandList);

        return ret;
    }

    @Override
    public List<String> getSuggestions(String key) throws IOException {
        //1.׼��Request
        SearchRequest request = new SearchRequest("hotel");
        //2.׼��DSL
        request.source().suggest(new SuggestBuilder().addSuggestion(
                "mySuggestion", SuggestBuilders.completionSuggestion("suggestion")
                        .prefix(key).skipDuplicates(true).size(10)
        ));
        SearchResponse result = client.search(request, RequestOptions.DEFAULT);
        //4.�������
        Suggest suggest = result.getSuggest();
        //4.1���ݲ�ѯ��ȫ���ƣ���ȡ��ȫ���
        CompletionSuggestion mySuggestion = suggest.getSuggestion("mySuggestion");
        //4.2��ȡoptions
        List<String> ret = new ArrayList<>();
        List<CompletionSuggestion.Entry.Option> options = mySuggestion.getOptions();
        for (CompletionSuggestion.Entry.Option option : options) {
            String text = option.getText().toString();
            ret.add(text);
        }
        return ret;
    }

    @Override
    public void deleteById(Long id) {
        DeleteRequest request = new DeleteRequest("hotel",id.toString());
        try {
            client.delete(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void insertById(Long id)  {
        //0.����id��ѯ����
        Hotel hotel = this.getById(id);
        HotelDoc hotelDoc = new HotelDoc(hotel);

        //1.׼��request����
        IndexRequest request = new IndexRequest("hotel").id(hotel.getId()+"");
        //2.׼��JSON�ĵ�
        request.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
        //3.��������
        try {
            client.index(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private List<String> resultExtracted(Aggregations aggregations,String conditions) {
        Terms brandTerms = aggregations.get(conditions);
        List<? extends Terms.Bucket> brandBuckets = brandTerms.getBuckets();
        List<String> brandList = new ArrayList<>();
        for (Terms.Bucket bucket : brandBuckets) {
            String key = bucket.getKeyAsString();
            brandList.add(key);
        }
        return brandList;
    }

    private void buildAggregation(SearchRequest request) {
        request.source().aggregation(AggregationBuilders.
                terms("brand_agg").
                field("brand")
                .size(100));
        request.source().aggregation(AggregationBuilders.
                terms("city_agg").
                field("city")
                .size(100));
        request.source().aggregation(AggregationBuilders.
                terms("star_agg").
                field("starName")
                .size(100));
    }

    private void buildBasicSearch(RequestParams requestParams, SearchRequest request) {
        BoolQueryBuilder booleanQuery = QueryBuilders.boolQuery();
        //�ؼ�������
        String key = requestParams.getKey();
        if(key==null||"".equals(key)){
            booleanQuery.must(QueryBuilders.matchAllQuery());
        }
        else {
            booleanQuery.must(QueryBuilders.matchQuery("all",key));
        }
        //��������
        //����
        if(requestParams.getCity()!=null&&!requestParams.getCity().equals("")){
            booleanQuery.filter(QueryBuilders.termQuery("city", requestParams.getCity()));
        }
        //Ʒ��
        if(requestParams.getBrand()!=null&&!requestParams.getBrand().equals("")){
            booleanQuery.filter(QueryBuilders.termQuery("brand", requestParams.getBrand()));
        }
        //�Ǽ�
        if(requestParams.getStarName()!=null&&!requestParams.getStarName().equals("")){
            booleanQuery.filter(QueryBuilders.termQuery("starName", requestParams.getStarName()));
        }
        //�۸�
        if(requestParams.getMinPrice()!=null&& requestParams.getMaxPrice()!=null){
            booleanQuery.filter(QueryBuilders.rangeQuery("price").gte(requestParams.getMinPrice())
                    .lte(requestParams.getMaxPrice()));
        }

        //2.��ֿ���
        FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(
                //ԭʼ��ѯ������������
                booleanQuery,
                //functionScore������
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                        //����һ��FunctionScoreԪ��
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                //��������
                                QueryBuilders.termQuery("isAD",true),
                                //��ֺ���
                                ScoreFunctionBuilders.weightFactorFunction(10)
                        )
                });
        request.source().query(functionScoreQueryBuilder);
    }

    private PageResult handleResponse(SearchRequest request) throws IOException {
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.�������
        SearchHits hits = response.getHits();
        System.out.println("��������"+hits.getTotalHits().value+"������");

        SearchHit[] hits1 = hits.getHits();
        List<HotelDoc> hotels = new ArrayList<>();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
//            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
//
//            if(!CollectionUtils.isEmpty(highlightFields)){
//                HighlightField highlightField = highlightFields.get("name");
//                if(highlightField!=null){
//                    String name = highlightField.getFragments()[0].string();
//                    hotelDoc.setName(name);
//                }
//            }
            Object[] sortValues = hit.getSortValues();
            if(sortValues.length>0){
                Object sortValue = sortValues[0];
                hotelDoc.setDistance(sortValue);
            }
            hotels.add(hotelDoc);
        }
        return new PageResult(hits.getTotalHits().value,hotels);
    }
}
