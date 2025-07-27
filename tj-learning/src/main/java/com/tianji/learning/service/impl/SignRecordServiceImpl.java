package com.tianji.learning.service.impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.Contstants.RedisContstants;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.msg.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {

    final StringRedisTemplate redisTemplate;

    final RabbitMqHelper rabbitMqHelper;
    @Override
    public SignResultVO addSignRecords() {
        Long user = UserContext.getUser();
        LocalDate now = LocalDate.now();
        String format = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = RedisContstants.SIGN_RECORD_KEY_PREFIX + user.toString()+ format;
        Boolean b = redisTemplate.opsForValue().setBit(key, now.getDayOfMonth() - 1, true);
        if(b)
            throw new BizIllegalException("不能重复签到");
        List<Long> list = redisTemplate.opsForValue().bitField(key,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(now.getDayOfMonth())).valueAt(0));
        int count=0;
        if(list!=null) {
            Long l = list.get(0);
            while ((l & 1) == 1) {
                count++;
                l >>= 1;
            }
        }
        int rewardPoints = 0;
        if(count==7){
            rewardPoints = 10;
        }else if(count==14){
            rewardPoints = 20;
        }else if(count==28){
            rewardPoints = 50;
        }
        rabbitMqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE, MqConstants.Key.SIGN_IN, SignInMessage.of(user, rewardPoints+1));
        SignResultVO signResultVO = new SignResultVO();
        signResultVO.setSignDays(count);
        signResultVO.setRewardPoints(rewardPoints);
        return signResultVO;
    }

    @Override
    public List<Integer> querySignRecords() {
        Long user = UserContext.getUser();
        if(user==null)
            return new ArrayList<>();
        LocalDate now = LocalDate.now();
        String format = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = RedisContstants.SIGN_RECORD_KEY_PREFIX + user.toString()+ format;
        List<Long> list = redisTemplate.opsForValue().bitField(key,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(now.getDayOfMonth())).valueAt(0));
        if (CollUtils.isEmpty(list)) {
             return CollUtils.emptyList();
        }
        Long l=list.get(0);
        List<Integer> result = new ArrayList<>();
        for(int i=0;i<now.getDayOfMonth();i++){
            result.add((l & 1) == 1 ? 1 : 0);
            l >>= 1;
        }
        return result;
    }
}
