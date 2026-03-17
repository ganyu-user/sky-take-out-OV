package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param orderPaymentDTO
     * @return
     * @throws Exception
     */
    OrderPaymentVO payment(OrdersPaymentDTO orderPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 历史订单查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    PageResult pageQueryUser(int page, int pageSize, Integer status);

    /**
     * 订单详情查询
     * @param id
     * @return
     */
    OrderVO detail(Long id);

    /**
     * 取消订单
     * @param id
     * @return
     */
    void userCancelById(Long id);

    /**
     * 再来一单
     * @param id
     * @return
     */
    void repetition(Long id);

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 各个状态的订单数量统计
     * @return
     */
    OrderStatisticsVO statistics();

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 拒单
     * @param ordersRejectionDTO
     * @return
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     * @return
     */
    void cancel(OrdersCancelDTO ordersCancelDTO);

    /**
     * 派送订单
     * @param id
     * @return
     */
    void delivery(Long id);

    /**
     * 完成订单
     * @param id
     * @return
     */
    void complete(Long id);

    /**
     * 用户催单
     * @param id
     * @return
     */
    void reminder(Long id);
}
