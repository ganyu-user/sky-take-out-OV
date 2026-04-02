package com.sky.utils;

import org.springframework.stereotype.Component;

/**
 * Snowflake雪花算法ID生成器
 *
 * 概述：
 * Snowflake是Twitter开源的分布式ID生成算法
 * 生成64位long型ID，趋势递增且不重复
 *
 * ID结构（从高位到低位）：
 * - 1位符号位：固定为0，表示正数
 * - 41位时间戳：毫秒级时间戳，可以使用69年
 * - 10位工作机器ID：5位数据中心ID + 5位工作机器ID
 * - 12位序列号：每毫秒内最多生成4096个ID
 *
 * 优点：
 * - 不依赖数据库或第三方服务
 * - 本地生成，效率高
 * - ID趋势递增，有利于数据库索引
 *
 * 注意：
 * - 默认使用workerId=1, datacenterId=1
 * - 分布式环境下需要确保各节点workerId不同
 */
@Component
public class SnowflakeIdUtil {

    /**
     * 起始时间戳：2021-01-01 00:00:00
     * 减少时间戳占用位数，延长使用寿命
     */
    private final long twepoch = 1609459200000L;

    /**
     * 机器ID占用的位数
     * 5位可以表示0-31共32个不同的机器
     */
    private final long workerIdBits = 5L;

    /**
     * 数据中心ID占用的位数
     * 5位可以表示0-31共32个不同的数据中心
     */
    private final long datacenterIdBits = 5L;

    /**
     * 机器ID的最大值
     * 用于校验workerId是否合法
     * ~(-1L << 5) = 31
     */
    private final long maxWorkerId = ~(-1L << workerIdBits);

    /**
     * 数据中心ID的最大值
     * 用于校验datacenterId是否合法
     * ~(-1L << 5) = 31
     */
    private final long maxDatacenterId = ~(-1L << datacenterIdBits);

    /**
     * 序列号占用的位数
     * 12位可以表示0-4095共4096个不同的序列号
     */
    private final long sequenceBits = 12L;

    /**
     * 机器ID向左偏移的位数
     */
    private final long workerIdShift = sequenceBits;

    /**
     * 数据中心ID向左偏移的位数
     */
    private final long datacenterIdShift = sequenceBits + workerIdBits;

    /**
     * 时间戳向左偏移的位数
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    /**
     * 序列号的最大值
     * 用于控制序列号在达到最大值后归零
     * ~(-1L << 12) = 4095
     */
    private final long sequenceMask = ~(-1L << sequenceBits);

    /**
     * 工作机器ID
     * 标识当前节点的工作机器编号
     */
    private final long workerId;

    /**
     * 数据中心ID
     * 标识当前节点所属的数据中心编号
     */
    private final long datacenterId;

    /**
     * 序列号
     * 同一毫秒内生成的不同序列号
     */
    private long sequence = 0L;

    /**
     * 上一次生成ID时使用的时间戳
     * 用于判断是否在同一毫秒内生成
     */
    private long lastTimestamp = -1L;

    /**
     * 默认构造函数
     * 使用workerId=1, datacenterId=1
     * 适用于单机部署或单节点场景
     */
    public SnowflakeIdUtil() {
        this(1, 1);
    }

    /**
     * 构造函数
     *
     * @param workerId 工作机器ID，范围0-31
     * @param datacenterId 数据中心ID，范围0-31
     * @throws IllegalArgumentException 如果workerId或datacenterId超出合法范围
     */
    public SnowflakeIdUtil(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException("workerId 不能大于 " + maxWorkerId);
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId 不能大于 " + maxDatacenterId);
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 生成下一个全局唯一ID
     * 线程安全，同步方法
     *
     * @return long类型的全局唯一ID
     * @throws RuntimeException 如果发生时钟回拨（当前时间小于上次时间）
     */
    public synchronized long nextId() {
        // 获取当前时间戳（毫秒）
        long timestamp = timeGen();

        // 时钟回拨检查：当前时间不能小于上次生成ID的时间
        // 如果发生回拨，说明系统时间被调整过，抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨");
        }

        // 同一毫秒内生成
        if (lastTimestamp == timestamp) {
            // 序列号自增
            sequence = (sequence + 1) & sequenceMask;
            // 如果序列号达到最大值（4095），说明同一毫秒内生成超过4096个ID
            // 需要等待到下一毫秒
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，序列号归零
            sequence = 0L;
        }

        // 更新上次时间戳
        lastTimestamp = timestamp;

        /**
         * 组装ID：
         * ((timestamp - twepoch) << timestampLeftShift)  时间戳部分
         * | (datacenterId << datacenterIdShift)             数据中心ID部分
         * | (workerId << workerIdShift)                    机器ID部分
         * | sequence                                        序列号部分
         */
        return ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    /**
     * 等待直到下一毫秒
     * 当同一毫秒内序列号用尽时调用此方法等待下一毫秒
     *
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 下一毫秒的时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        // 获取当前时间戳
        long timestamp = timeGen();
        // 如果当前时间戳仍然等于lastTimestamp，说明还没到下一毫秒
        // 继续循环等待
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳（毫秒）
     * 单独提取出来是为了方便测试时进行mock
     *
     * @return 当前系统时间戳
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }
}