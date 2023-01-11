package cn.itcast.hotel;

import cn.itcast.hotel.service.IHotelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class HotelDemoApplicationTests {

    @Autowired
    private IHotelService iHotelService;

    @Test
    void contextLoads() {
    }

//    @Test
//    void testAggregation() throws IOException {
//        System.out.println(iHotelService.filters());;
//    }
}
