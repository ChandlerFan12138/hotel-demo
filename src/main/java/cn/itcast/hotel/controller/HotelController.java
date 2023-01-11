package cn.itcast.hotel.controller;

import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hotel")
public class HotelController {

    @Autowired
    private IHotelService iHotelService;

    @PostMapping("/list")
    public PageResult search(@RequestBody RequestParams requestParams) throws IOException {

        return iHotelService.search(requestParams);

    }
    @PostMapping("/filters")
    public Map<String, List<String>> line(@RequestBody RequestParams requestParams) throws IOException {
        return iHotelService.filters(requestParams);
    }

    @GetMapping("suggestion")
    public List<String> getSuggestion(@RequestParam("key") String key ) throws IOException {
        return iHotelService.getSuggestions(key);
    }
}
