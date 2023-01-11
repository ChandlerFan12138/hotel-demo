package cn.itcast.hotel.mq;

import cn.itcast.hotel.constants.MqConstants;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HotelListener {
    @Autowired
    private IHotelService hotelService;
    /*监听酒店新增或修改业务*/
    @RabbitListener(queues = MqConstants.HOTEL_INSERT_QUEUE)
    public void listenHotelInsertOrUdate(Long id) throws IOException {
        hotelService.insertById(id);

    }
    /*监听酒店删除业务*/
    @RabbitListener(queues = MqConstants.HOTEL_INSERT_QUEUE)
    public void listenHotelDelete(Long id){
        hotelService.deleteById(id);
    }
}
