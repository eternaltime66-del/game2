package org.wx.core.wxBusiness.account.service;

import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBase.unit.WordUnit;
import org.wx.core.wxBusiness.account.entity.PointPayOrder;
import org.wx.core.wxBusiness.account.entity.enums.MoneyDirectionType;
import org.wx.core.wxBusiness.account.entity.enums.MoneyRecordType;
import org.wx.core.wxBusiness.account.entity.enums.PointOrderState;
import org.wx.core.wxBusiness.account.entity.enums.PointOrderType;
import org.wx.core.wxBusiness.account.mapper.PointPayOrderMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * PointPayOrder Service实现类
 * @author 无心
 * @date 2026-01-21
 */
@Service
public class PointPayOrderService extends WxServiceImpl<PointPayOrderMapper, PointPayOrder> {



    public PointPayOrder createOrder(String uid, BigDecimal amount, PointOrderType type) {
        PointPayOrder order = new PointPayOrder();
        order.setOrderNo(type+WordUnit.nowId(8,2));
        order.setId(order.getOrderNo());
        order.setState(PointOrderState.Running);
        order.setUid(uid);
        order.setType(type);
        order.setAmount(amount);
        this.save(order);
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public PointPayOrder success(String orderId) {
        PointPayOrder pointPayOrder = this.getById(orderId);
        pointPayOrder.setState(PointOrderState.Success);
        this.wxUpdateById(pointPayOrder,PointPayOrder::getState);
        return pointPayOrder;
    }
    @Transactional(rollbackFor = Exception.class)
    public PointPayOrder fail(String orderId) {
        PointPayOrder pointPayOrder = this.getById(orderId);
        pointPayOrder.setState(PointOrderState.Success);
        this.wxUpdateById(pointPayOrder,PointPayOrder::getState);
        return pointPayOrder;
    }
    @Transactional(rollbackFor = Exception.class)
    public PointPayOrder success(PointPayOrder pointPayOrder) {
        pointPayOrder.setState(PointOrderState.Success);
        this.wxUpdateById(pointPayOrder,PointPayOrder::getState);
        return pointPayOrder;
    }
    @Transactional(rollbackFor = Exception.class)
    public PointPayOrder fail(PointPayOrder pointPayOrder) {
        pointPayOrder.setState(PointOrderState.Success);
        this.wxUpdateById(pointPayOrder,PointPayOrder::getState);
        return pointPayOrder;
    }
}
