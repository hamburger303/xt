package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.copier.CopyOptions;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.discount.DiscountStrategy;
import com.tianji.promotion.domain.dto.CouponDiscountDTO;
import com.tianji.promotion.domain.dto.OrderCourseDTO;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.enums.MyLockType;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.utils.CodeUtil;
import com.tianji.promotion.utils.MyLock;
import com.tianji.promotion.utils.MyLockStrategy;
import com.tianji.promotion.utils.PermuteUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-21
 */
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

    final CouponMapper couponMapper;

    final StringRedisTemplate redisTemplate;

    final IExchangeCodeService exchangeCodeService;

    final RedissonClient redissonClient;

    final RabbitMqHelper mqHelper;

    final ICouponScopeService couponScopeService;
    @Override
    @Transactional
    @MyLock(name = "lock:coupon:uid:#{id}",
            lockType = MyLockType.RE_ENTRANT_LOCK,
            lockStrategy = MyLockStrategy.FAIL_AFTER_RETRY_TIMEOUT)
    public void receiveCoupon(Long id) {
        if(id==null)
            throw new BadRequestException("非法参数");
//        Coupon coupon = couponMapper.selectById(id);
        Coupon coupon = queryCouponByCache(id);
        if(coupon==null){
            throw new BadRequestException("优惠券不存在");
        }
//        if(coupon.getStatus()!= CouponStatus.ISSUING){
//            throw new BadRequestException("优惠券不存在或者优惠券正在使用中");
//        }
//        if(coupon.getTotalNum()<=coupon.getIssueNum()||coupon.getTotalNum()<=0)
        if(coupon.getTotalNum()<=0)
            throw new BadRequestException("优惠券已领完");
        LocalDateTime now = LocalDateTime.now();
        if(now.isBefore(coupon.getIssueBeginTime())||now.isAfter(coupon.getIssueEndTime()))
            throw new BadRequestException("优惠券不在领取时间段内");
        Long user = UserContext.getUser();
        String key=PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX+id;
        Long increment = redisTemplate.opsForHash().increment(key, user.toString(), 1);
        if(increment>coupon.getUserLimit()) {
            redisTemplate.opsForHash().increment(key, user.toString(), -1);
            throw new BizIllegalException("超出领取限制");
        }
        String couponKey=PromotionConstants.COUPON_CACHE_KEY_PREFIX+id;
        redisTemplate.opsForHash().increment(couponKey, "totalNum", -1);
        UserCouponDTO dto = new UserCouponDTO();
        dto.setUserId(user);
        dto.setCouponId(id);
        mqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE, MqConstants.Key.COUPON_RECEIVE, dto);
//        synchronized (user.toString().intern()) {
//            IUserCouponService userCouponService =(IUserCouponService) AopContext.currentProxy();
//            userCouponService.checkAndCreateUserCoupon(coupon,user,null);
//        }
//        RLock lock = redissonClient.getLock("lock:coupon:uid:" + user);
//        try{
//            boolean tryLock = lock.tryLock();
//            if(tryLock){
//                IUserCouponService userCouponService =(IUserCouponService) AopContext.currentProxy();
//                userCouponService.checkAndCreateUserCoupon(coupon,user,null);
//            }
//            else {
//                throw new BizIllegalException("操作繁忙");
//            }
//        }finally {
//            lock.unlock();
//        }

    }

    private Coupon queryCouponByCache(Long id) {
        String key= PromotionConstants.COUPON_CACHE_KEY_PREFIX +id;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        return BeanUtils.mapToBean(entries, Coupon.class, false, CopyOptions.create());
    }

    @Override
    public void exchangeCoupon(String code) {
        if(code==null)
            throw new BadRequestException("非法参数");
        Long user = UserContext.getUser();
        long parseCode = CodeUtil.parseCode(code);
        String key= PromotionConstants.COUPON_CODE_MAP_KEY;
        Boolean flag=redisTemplate.opsForValue().setBit(key, parseCode, true);
        if(flag!=null&& flag){
            throw new BizIllegalException("优惠券已使用");
        }
        ExchangeCode exchangeCode = exchangeCodeService.getById(parseCode);
        if(exchangeCode==null||exchangeCode.getStatus()!= ExchangeCodeStatus.UNUSED){
            throw new BadRequestException("优惠券不存在或者已使用");
        }
        LocalDateTime now = LocalDateTime.now();
        if(now.isAfter(exchangeCode.getExpiredTime())){
            throw new BadRequestException("优惠券已过期");
        }
        Coupon coupon = couponMapper.selectById(exchangeCode.getExchangeTargetId());
        if(coupon==null){
            throw new BadRequestException("优惠券不存在");
        }
//        synchronized (user.toString().intern()) {
//            IUserCouponService userCouponService =(IUserCouponService) AopContext.currentProxy();
//            userCouponService.checkAndCreateUserCoupon(coupon,user,parseCode);
//        }
//        RLock lock = redissonClient.getLock("lock:coupon:uid:" + user);
//        try{
//            boolean tryLock = lock.tryLock();
//            if(tryLock){
                IUserCouponService userCouponService =(IUserCouponService) AopContext.currentProxy();
                userCouponService.checkAndCreateUserCoupon(coupon,user,null);
//            }
//            else {
//                throw new BizIllegalException("操作繁忙");
//            }
//        }finally {
//            lock.unlock();
//        }
    }

    @Override
    @Transactional
    @MyLock(name = "lock:coupon:uid:#{userId}",lockType = MyLockType.RE_ENTRANT_LOCK,lockStrategy = MyLockStrategy.FAIL_AFTER_RETRY_TIMEOUT)
    public void checkAndCreateUserCoupon(Coupon coupon, Long user, Long parseCode){
        Integer count = this.lambdaQuery()
                .eq(UserCoupon::getUserId, user)
                .eq(UserCoupon::getCouponId, coupon.getId())
                .count();
        if (count >= coupon.getUserLimit())
            throw new BadRequestException("您已领取过该优惠券");
        couponMapper.IncrIssueNum(coupon.getId());
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(user);
        userCoupon.setCouponId(coupon.getId());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime termBeginTime = coupon.getTermBeginTime();
        LocalDateTime termEndTime = coupon.getTermEndTime();
        if (termEndTime == null && termBeginTime == null) {
            userCoupon.setTermBeginTime(now);
            userCoupon.setTermEndTime(now.plusDays(coupon.getTermDays()));
        } else {
            userCoupon.setTermBeginTime(termBeginTime);
            userCoupon.setTermEndTime(termEndTime);
        }
        this.save(userCoupon);
        if(parseCode!= null) {
            exchangeCodeService.lambdaUpdate()
                    .eq(ExchangeCode::getId, parseCode)
                    .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
                    .set(ExchangeCode::getUserId, user)
                    .update();
        }
    }

    @Override
    @Transactional
    public void NewcheckAndCreateUserCoupon(UserCouponDTO msg) {
       Long user = UserContext.getUser();
//        Integer count = this.lambdaQuery()
//                .eq(UserCoupon::getUserId, user)
//                .eq(UserCoupon::getCouponId, coupon.getId())
//                .count();
//        if (count >= coupon.getUserLimit())
//            throw new BadRequestException("您已领取过该优惠券");
        Coupon coupon = couponMapper.selectById(msg.getCouponId());
        if(coupon==null)
            return;
        couponMapper.IncrIssueNum(msg.getCouponId());
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(user);
        userCoupon.setCouponId(coupon.getId());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime termBeginTime = coupon.getTermBeginTime();
        LocalDateTime termEndTime = coupon.getTermEndTime();
        if (termEndTime == null && termBeginTime == null) {
            userCoupon.setTermBeginTime(now);
            userCoupon.setTermEndTime(now.plusDays(coupon.getTermDays()));
        } else {
            userCoupon.setTermBeginTime(termBeginTime);
            userCoupon.setTermEndTime(termEndTime);
        }
        this.save(userCoupon);
//        if(parseCode!= null) {
//            exchangeCodeService.lambdaUpdate()
//                    .eq(ExchangeCode::getId, parseCode)
//                    .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
//                    .set(ExchangeCode::getUserId, user)
//                    .update();
//        }
    }

    @Override
    public List<CouponDiscountDTO> queryCouponDiscount(List<OrderCourseDTO> courses) {
        List<Coupon> couponList = getBaseMapper().queryMyCoupon(UserContext.getUser());
        if(couponList.isEmpty())
            return Collections.emptyList();
        int totalAmount = courses.stream().mapToInt(OrderCourseDTO::getPrice).sum();
        List<Coupon> availableCoupons=couponList
                .stream()
                .filter(coupon -> DiscountStrategy.getDiscount(coupon.getDiscountType()).canUse(totalAmount, coupon))
                .collect(Collectors.toList());
        if(availableCoupons.isEmpty())
            return Collections.emptyList();
        Map<Coupon, List<OrderCourseDTO>> availbleCouponMap= findAvailableCoupon(availableCoupons, courses);
        availableCoupons=new ArrayList<>(availbleCouponMap.keySet());
        List<List<Coupon>> permute = PermuteUtil.permute(availableCoupons);
        for (Coupon coupons : availableCoupons) {
            permute.add(List.of(coupons));
        }
        for (List<Coupon> coupons : permute) {
            List<Long> cids = coupons.stream().map(Coupon::getId).collect(Collectors.toList());
        }
        List<CouponDiscountDTO> discountDTOs = new ArrayList<>();
        for (List<Coupon> coupons : permute) {
            CouponDiscountDTO discountDTO=calculateSolutionDiscount(availbleCouponMap, courses, coupons);
            discountDTOs.add(discountDTO);
        }
        return discountDTOs;
    }

    private CouponDiscountDTO calculateSolutionDiscount(Map<Coupon, List<OrderCourseDTO>> availbleCouponMap,
                                                        List<OrderCourseDTO> courses,
                                                        List<Coupon> coupons) {
        CouponDiscountDTO discountDTO = new CouponDiscountDTO();
        Map<Long, Integer> detailMap = courses.stream().collect(Collectors.toMap(OrderCourseDTO::getId, c -> 0));
        for (Coupon coupon : coupons) {
            List<OrderCourseDTO> courseDTOS = availbleCouponMap.get(coupon);
            int totalMount = courseDTOS.stream().mapToInt(value -> value.getPrice() - detailMap.get(value.getId())).sum();
            boolean canUse = DiscountStrategy.getDiscount(coupon.getDiscountType()).canUse(totalMount, coupon);
            if (!canUse)
                continue;
            int discountAmount = DiscountStrategy.getDiscount(coupon.getDiscountType()).calculateDiscount(totalMount, coupon);
            calculateDiscountDetails(detailMap, courseDTOS, totalMount, discountAmount);
            discountDTO.getIds().add(coupon.getId());
            discountDTO.getRules().add(DiscountStrategy.getDiscount(coupon.getDiscountType()).getRule(coupon));
            discountDTO.setDiscountAmount(discountAmount + discountDTO.getDiscountAmount());
        }
        return discountDTO;
    }

    private void calculateDiscountDetails(Map<Long, Integer> detailMap,
                                          List<OrderCourseDTO> courseDTOS,
                                          int totalMount,
                                          int discountAmount) {
        int times = 0;
        int remainDiscount = discountAmount;
        int discount=0;
        for (OrderCourseDTO courseDTO : courseDTOS) {
            times++;
            if(times==courseDTOS.size()){
                discount = totalMount - remainDiscount;
            }else {
                discount = (courseDTO.getPrice()-detailMap.get(courseDTO.getId())) * discountAmount / totalMount;
                remainDiscount=remainDiscount-discount;
            }
            detailMap.put(courseDTO.getId(), discount + detailMap.get(courseDTO.getId()));
        }
    }

    private Map<Coupon, List<OrderCourseDTO>> findAvailableCoupon(List<Coupon> availableCoupons, List<OrderCourseDTO> courses) {
        Map<Coupon, List<OrderCourseDTO>> map = new HashMap<>();
        for (Coupon coupon : availableCoupons) {
            Boolean specific = coupon.getSpecific();
            List<OrderCourseDTO> scopeCourses=courses;
            if(specific != null && specific){
                List<CouponScope> list = couponScopeService.lambdaQuery()
                        .eq(CouponScope::getCouponId, coupon.getId())
                        .list();
                if(list.isEmpty())
                    continue;
                List<Long> scopeIds = list.stream().map(CouponScope::getBizId).collect(Collectors.toList());
                scopeCourses = courses.stream()
                        .filter(course -> scopeIds.contains(course.getCateId()))
                        .collect(Collectors.toList());
                }
            int totalAmount = scopeCourses.stream().mapToInt(OrderCourseDTO::getPrice).sum();
            boolean canUse = DiscountStrategy.getDiscount(coupon.getDiscountType()).canUse(totalAmount, coupon);
            if (canUse) {
                map.put(coupon, scopeCourses);
            }
        }
        return map;
    }


}
