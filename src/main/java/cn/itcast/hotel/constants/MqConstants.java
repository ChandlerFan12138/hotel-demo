package cn.itcast.hotel.constants;

public class MqConstants {
    /*������*/
    public final static String HOTEL_EXCHANGE = "hotel.topic";
    /*�����������޸Ķ���*/
    public final static String HOTEL_INSERT_QUEUE = "hotel.insert.queue";
    /*����ɾ������*/
    public final static String HOTEL_DELETE_QUEUE = "hotel.delete.queue";
    /*�������޸�routingKEy*/
    public final static String HOTEL_INSERT_KEY = "hotel.insert";
    /*ɾ��routingKEy*/
    public final static String HOTEL_DELETE_KEY = "hotel.delete";
}
