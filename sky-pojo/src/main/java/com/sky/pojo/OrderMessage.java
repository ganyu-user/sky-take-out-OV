package com.sky.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单消息实体类
 * 用于MQ消息队列传输的订单信息载体
 *
 * 注意：此类不是数据库实体类，不对应任何数据表
 *      仅用于在RabbitMQ消息传递过程中承载订单相关数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage implements Serializable {

    /**
     * 用户ID
     * 下单用户的唯一标识
     */
    private Long userId;

    /**
     * 地址簿ID
     * 用户选择的收货地址在地址簿中的ID
     */
    private Long addressBookId;

    /**
     * 订单ID
     * 订单在数据库中的主键ID
     */
    private Long orderId;

    /**
     * 订单号
     * 用于对外展示的订单编号，由Snowflake算法生成
     */
    private String orderNumber;

    /**
     * 订单金额
     * 用户需要支付的订单总金额
     */
    private BigDecimal amount;

    /**
     * 备注
     * 用户下单时的备注信息，如口味要求等
     */
    private String remark;

    /**
     * 预计送达时间
     * 根据距离计算出的预计送达时间
     */
    private LocalDateTime estimatedDeliveryTime;

    /**
     * 打包费
     * 订单的打包费用
     */
    private Integer packAmount;

    /**
     * 餐具数量
     * 订单需要的餐具数量
     */
    private Integer tablewareNumber;

    /**
     * 餐具状态
     * 标识是否需要餐具：1-需要，0-不需要
     */
    private Integer tablewareStatus;

    /**
     * 联系电话
     * 收货人的联系电话
     */
    private String phone;

    /**
     * 收货人
     * 收货人的姓名
     */
    private String consignee;

    /**
     * 收货地址
     * 完整的收货地址，包含省市区和详细地址
     */
    private String address;
}