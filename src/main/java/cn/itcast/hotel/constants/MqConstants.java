package cn.itcast.hotel.constants;

public class MqConstants {
    /*交换机*/
    public final static String HOTEL_EXCHANGE = "hotel.topic";
    /*监听新增和修改队列*/
    public final static String HOTEL_INSERT_QUEUE = "hotel.insert.queue";
    /*监听删除队列*/
    public final static String HOTEL_DELETE_QUEUE = "hotel.delete.queue";
    /*新增或修改routingKEy*/
    public final static String HOTEL_INSERT_KEY = "hotel.insert";
    /*删除routingKEy*/
    public final static String HOTEL_DELETE_KEY = "hotel.delete";
}
