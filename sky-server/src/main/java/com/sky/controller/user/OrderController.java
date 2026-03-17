package com.sky.controller.user;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/user/order")
@RestController("userOrderController")
@Slf4j
@Api(tags = "C端-订单相关接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    @ApiOperation(("用户下单"))
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户下单："+ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO=orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     * @param orderPaymentDTO
     * @return
     * @throws Exception
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO orderPaymentDTO) throws Exception{
        log.info("订单支付："+orderPaymentDTO);
        OrderPaymentVO orderPaymentVO=orderService.payment(orderPaymentDTO);
        log.info("生成预支付教育单："+orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 历史订单查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @GetMapping("historyOrders")
    @ApiOperation("历史订单查询")
    public Result<PageResult> page(int page,int pageSize,Integer status){
        log.info("查询历史订单："+page+","+pageSize+","+status);
        PageResult pageResult=orderService.pageQueryUser(page,pageSize,status);
        return Result.success(pageResult);
    }

    /**
     * 订单详情查询
     * @param id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("订单详情查询")
    public Result<OrderVO> detail(@PathVariable("id") Long id){
        OrderVO orderVO=orderService.detail(id);
        log.info("订单详情查询："+orderVO);
        return Result.success(orderVO);
    }

    /**
     * 取消订单
     * @param id
     * @return
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("cancel(取消订单)")
    public Result cancel(@PathVariable("id")  Long id){
        orderService.userCancelById(id);
        return Result.success();
    }

    /**
     * 再来一单
     * @param id
     * @return
     */
    @ApiOperation("repetition(再来一单)")
    @PostMapping("/repetition/{id}")
    public Result repetition(@PathVariable("id") Long id){
        orderService.repetition(id);
        return Result.success();
    }

    /**
     * 用户催单
     * @param id
     * @return
     */
    @GetMapping("/reminder/{id}")
    @ApiOperation("reminder(用户催单)")
    public Result reminder(@PathVariable("id") Long id){
        orderService.reminder(id);
        return Result.success();
    }
}
