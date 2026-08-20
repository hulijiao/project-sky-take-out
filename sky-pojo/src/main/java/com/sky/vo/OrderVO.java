package com.sky.vo;

import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO extends Orders implements Serializable {

    // 订单菜品信息 String类型
    // "orderDishes":"宫保鸡丁*3;豆腐汤*2;"
    //列表接口用,简单展示
    private String orderDishes;

    //订单详情 List类型
    //详情接口返回，前端可以循环遍历，取出名字、份数、单价，详情页面用来展示完整账单
    /*
    "orderDetailList": [
    {
        "id":101,
        "orderId":5001,
        "name":"宫保鸡丁",
        "number":3,
        "amount":38.0
    },
    {
        "id":102,
        "orderId":5001,
        "name":"豆腐汤",
        "number":2,
        "amount":14.0
    }
     */
    private List<OrderDetail> orderDetailList;

}
