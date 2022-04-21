package com.qzb.ble.listener;

/**
 * 生活白科技-居家小确幸
 *
 * @ClassName: IFilter
 * @Author: Leon.Qin
 * @Date: 2022/3/23 10:14
 * @Description:
 */
public interface IFilter {
    boolean filter(byte[] bytes);
}