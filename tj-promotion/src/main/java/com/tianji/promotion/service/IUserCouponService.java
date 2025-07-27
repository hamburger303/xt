package com.tianji.promotion.service;

import com.tianji.promotion.domain.dto.CouponDiscountDTO;
import com.tianji.promotion.domain.dto.OrderCourseDTO;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务类
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-21
 */
public interface IUserCouponService extends IService<UserCoupon> {

    void receiveCoupon(Long id);

    void exchangeCoupon(String code);

    @Transactional
    void checkAndCreateUserCoupon(Coupon coupon, Long user, Long parseCode);

    @Transactional
    void NewcheckAndCreateUserCoupon(UserCouponDTO msg);

    List<CouponDiscountDTO> queryCouponDiscount(List<OrderCourseDTO> courses);
}
