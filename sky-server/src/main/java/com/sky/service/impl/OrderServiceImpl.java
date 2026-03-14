package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //1.处理业务异常（判断地址簿是否为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //2.处理业务异常（判断购物车是否为空）
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //3.向订单表插入一条数据
        Orders  orders = new Orders();//创建订单实体
        BeanUtils.copyProperties(ordersSubmitDTO, orders);//把DTO里面的数据复制到实体
        //剩下的逐个封装
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(orders.UN_PAID);
        orders.setStatus(orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());

        orderMapper.insert(orders);

        //4.向订单明细表插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();//设置一个订单明细列表
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail  orderDetail = new OrderDetail();//创建订单明细实体
            BeanUtils.copyProperties(cart, orderDetail);//把购物车的一个商品信息复制给一个实体
            orderDetail.setOrderId(orders.getId());//设置当前明细关联的订单id
            orderDetailList.add(orderDetail);//把所有订单明细封装成一个列表
        }

        orderDetailMapper.insertBatch(orderDetailList);//批量插入明细数据

        //5.清空当前用户购物车数据
        shoppingCartMapper.cleanByUserId(userId);

        //6.封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     * @param orderPaymentDTO
     * @return
     * @throws Exception
     */
    public OrderPaymentVO payment(OrdersPaymentDTO orderPaymentDTO) throws Exception {
        //获取当前订单的用户
        Long userId = BaseContext.getCurrentId();
        //User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        //JSONObject jsonObject = weChatPayUtil.pay(
        //        orderPaymentDTO.getOrderNumber(),//商户订单号
        //        new BigDecimal(0.01),//支付金额，单位 元
        //        "苍穹外卖订单", //商品描述
        //        user.getOpenid()//微信用户的openid
        //);
        //
        //if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
        //    throw new OrderBusinessException("该订单已支付");
        //}

        JSONObject jsonObject= new JSONObject();
        jsonObject.put("code","ORDERPAID");

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        Integer orderStatus=Orders.TO_BE_CONFIRMED;  //订单状态：待接单
        Integer orderPaidStatus = Orders.PAID;  //支付状态：已支付
        LocalDateTime checkOutTime= LocalDateTime.now();//更新支付时间
        Orders orders = orderMapper.getByNumber(orderPaymentDTO.getOrderNumber());

        Orders updateorder = Orders.builder()
                .id(orders.getId())
                .status(orderStatus)
                .payStatus(orderPaidStatus)
                .checkoutTime(checkOutTime)
                .build();

        orderMapper.update(updateorder);

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {
        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }
}
