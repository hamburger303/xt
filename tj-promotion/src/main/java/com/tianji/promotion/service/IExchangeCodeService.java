package com.tianji.promotion.service;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 兑换码 服务类
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-19
 */
public interface IExchangeCodeService extends IService<ExchangeCode> {

    void generateCode(Coupon coupon);
}
