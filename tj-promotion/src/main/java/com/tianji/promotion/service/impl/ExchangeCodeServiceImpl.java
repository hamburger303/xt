package com.tianji.promotion.service.impl;

import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.mapper.ExchangeCodeMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 兑换码 服务实现类
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-19
 */
@Service
@RequiredArgsConstructor
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {

    final StringRedisTemplate redisTemplate;
    @Override
    @Async("generateExchangeCodeExecutor")
    @Transactional
    public void generateCode(Coupon coupon) {
        Integer totalNum = coupon.getTotalNum();
        String key= PromotionConstants.COUPON_CODE_SERIAL_KEY;
        Long increment = redisTemplate.opsForValue().increment(key, totalNum);
        if(increment==null)
            return;
        int inr = increment.intValue();
        List<ExchangeCode> exchangeCodes = new ArrayList<>();
        for(int i=inr-totalNum+1;i<=inr;i++){
            String code = CodeUtil.generateCode(i, coupon.getId());
            ExchangeCode exchangeCode = new ExchangeCode();
            exchangeCode.setCode(code);
            exchangeCode.setId(i);
            exchangeCode.setExpiredTime(coupon.getIssueEndTime());
            exchangeCode.setExchangeTargetId(coupon.getId());
            exchangeCodes.add(exchangeCode);
        }
        this.saveBatch(exchangeCodes);
        redisTemplate.opsForZSet().add(PromotionConstants.COUPON_RANGE_KEY,coupon.getId().toString(), inr);
    }
}
