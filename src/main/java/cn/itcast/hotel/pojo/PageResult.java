package cn.itcast.hotel.pojo;

import lombok.Data;

import java.util.List;

@Data
public class PageResult {
    private Long total;
    private List<HotelDoc> hotels;

    public PageResult() {
    }

    public PageResult(long value, List<HotelDoc> hotels) {
        this.total = value;
        this.hotels = hotels;
    }
}
