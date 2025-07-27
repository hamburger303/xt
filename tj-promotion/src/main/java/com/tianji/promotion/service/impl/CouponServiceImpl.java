package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.*;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.ObtainType;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.ICouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.remark.constants.RedisConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 优惠券的规则信息 服务实现类
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-19
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {

    final ICouponScopeService couponScopeService;

    final IExchangeCodeService exchangeCodeService;

    final IUserCouponService userCouponService;

    final StringRedisTemplate redisTemplate;
    @Override
    @Transactional
    public void saveCoupon(CouponFormDTO dto) {
        Coupon coupon = BeanUtils.copyBean(dto, Coupon.class);
        save(coupon);
        Boolean specific = dto.getSpecific();
        if (specific != null && specific) {
            // 保存作用域
            if(CollUtils.isEmpty(dto.getScopes())){
                throw new BadRequestException("请选择作用范围");
            }
            List<Long> scopes = dto.getScopes();
            List<CouponScope> collect = scopes.stream().map(new Function<Long, CouponScope>() {
                @Override
                public CouponScope apply(Long aLong) {
                    return new CouponScope().setCouponId(coupon.getId()).setBizId(aLong).setType(1);
                }
            }).collect(Collectors.toList());
            couponScopeService.saveBatch( collect);
        }
    }

    @Override
    public PageDTO<CouponDetailVO> queryCouponByPage(CouponQuery query) {
        Page<Coupon> page = this.lambdaQuery()
                .eq(query.getStatus() != null, Coupon::getStatus, query.getStatus())
                .eq(query.getType() != null, Coupon::getDiscountType, query.getType())
                .like(StringUtils.isNotBlank(query.getName()), Coupon::getName, query.getName())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<Coupon> records = page.getRecords();
        if(CollUtils.isEmpty(records))
            return PageDTO.empty(page);
        List<CouponDetailVO> couponDetailVOS = BeanUtils.copyList(records, CouponDetailVO.class);
        return PageDTO.of(page, couponDetailVOS);
    }

    @Override
    @Transactional
    public void issueCoupon(Long id, CouponIssueFormDTO dto) {
        if(id==null||!id.equals(dto.getId()))
            throw new BadRequestException("参数错误");
        Coupon coupon = this.getById(id);
        if(coupon.getStatus()!= CouponStatus.DRAFT&&coupon.getStatus()!= CouponStatus.PAUSE){
            throw new BizIllegalException("优惠券状态错误");
        }
        LocalDateTime issueBeginTime = dto.getIssueBeginTime();
        LocalDateTime issueEndTime = dto.getIssueEndTime();
        LocalDateTime now = LocalDateTime.now();
        boolean isBegin=issueBeginTime==null||!issueBeginTime.isAfter(now);
        if(isBegin){
            issueBeginTime=now;
            coupon.setIssueBeginTime(issueBeginTime);
            coupon.setStatus(CouponStatus.ISSUING);
        }else {
            coupon.setStatus(CouponStatus.UN_ISSUE);
        }
        if(isBegin){
            String key= PromotionConstants.COUPON_CACHE_KEY_PREFIX +id;
            redisTemplate.opsForHash().put(key,"issueBeginTime",String.valueOf(DateUtils.toEpochMilli(now)));
            redisTemplate.opsForHash().put(key,"issueEndTime",String.valueOf(DateUtils.toEpochMilli(dto.getTermEndTime())));
            redisTemplate.opsForHash().put(key,"totalNum",String.valueOf(coupon.getTotalNum()));
            redisTemplate.opsForHash().put(key,"userLimit",String.valueOf(coupon.getUserLimit()));
        }
        coupon.setIssueEndTime(issueEndTime);
        coupon.setTermDays(dto.getTermDays());
        coupon.setTermBeginTime(dto.getTermBeginTime());
        coupon.setTermEndTime(dto.getTermEndTime());
        this.updateById(coupon);
        if(coupon.getObtainWay()== ObtainType.ISSUE&&coupon.getStatus()== CouponStatus.DRAFT){
            coupon.setIssueEndTime(issueEndTime);
            exchangeCodeService.generateCode(coupon);
        }
    }

    @Override
    public List<CouponVO> listIssuingCoupons() {
        Long user = UserContext.getUser();
        List<Coupon> list = this.lambdaQuery()
                .eq(Coupon::getStatus, CouponStatus.ISSUING)
                .eq(Coupon::getObtainWay, ObtainType.PUBLIC)
                .list();
        if(CollUtils.isEmpty(list))
            return CollUtils.emptyList();
        List<Long> couponIds = list.stream().map(Coupon::getId).collect(Collectors.toList());
        List<UserCoupon> listed = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, user)
                .in(UserCoupon::getCouponId, couponIds)
                .list();
        Map<Long, Long> map = listed
                .stream()
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        Map<Long, Long> unuseMap = listed
                .stream().filter(c -> c.getStatus() == UserCouponStatus.UNUSED)
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        List<CouponVO> couponVOS = new ArrayList<>();
        for (Coupon c : list) {
            CouponVO couponVO = BeanUtils.copyBean(c, CouponVO.class);
            Long issueNum = map.getOrDefault(c.getId(), 0L);
            boolean available = c.getIssueNum()<c.getTotalNum() && issueNum.intValue() < c.getUserLimit();
            couponVO.setAvailable(available);
            boolean receive=unuseMap.getOrDefault(c.getId(), 0L)>0;
            couponVO.setReceived(receive);
            couponVOS.add(couponVO);
        }
        return couponVOS;
    }
}
