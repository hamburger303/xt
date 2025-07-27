package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.constants.RedisConstants;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.dto.LikedTimesDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-15
 */
@Service
@RequiredArgsConstructor
public class LikedRecordRedisServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    final RabbitMqHelper rabbitMqHelper;

    final StringRedisTemplate redisTemplate;
    @Override
    public void addLikeRecord(LikeRecordFormDTO likeRecordFormDTO) {
        Long userId = UserContext.getUser();
        if(userId == null)
            throw new BizIllegalException("用户未登录");
        String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + likeRecordFormDTO.getBizId();
        boolean flag=false;
        if(likeRecordFormDTO.getLiked()){
            Long result = redisTemplate.opsForSet().add(key, userId.toString());
            flag=result > 0&&result!=null;
        }else {
            Long result=redisTemplate.opsForSet().remove(key, userId.toString());
            flag=result > 0&&result!=null;
        }
        if(!flag)
            return;
        Long count = redisTemplate.opsForSet().size(key);
        if(count==null)
            return;
        String BizTypeTotalLikeKey=RedisConstants.LIKE_COUNT_KEY_PREFIX + likeRecordFormDTO.getBizType();
        redisTemplate.opsForZSet().add(BizTypeTotalLikeKey, likeRecordFormDTO.getBizId().toString(), count);
    }

    @Override
    public Set<Long> getLikesByBizIds(List<Long> bizIds) {
//        if(CollUtils.isEmpty(bizIds))
//            return CollUtils.emptySet();
//        Long user = UserContext.getUser();
//        if(user == null)
//            throw new BadRequestException("用户未登录");
//        Set<Long> retSet=new HashSet<>();
//        for (Long bizId : bizIds) {
//            Boolean member = redisTemplate.opsForSet().isMember(RedisConstants.LIKE_BIZ_KEY_PREFIX + bizId, user.toString());
//            if(member){
//                retSet.add(bizId);
//            }
//        }
//        return retSet;

        // 1.获取登录用户id
        Long userId = UserContext.getUser();
        // 2.查询点赞状态
        List<Object> objects = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = (StringRedisConnection) connection;
            for (Long bizId : bizIds) {
                String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + bizId;
                src.sIsMember(key, userId.toString());
            }
            return null;
        });
        // 3.返回结果
        return IntStream.range(0, objects.size()) // 创建从0到集合size的流
                .filter(i -> (boolean) objects.get(i)) // 遍历每个元素，保留结果为true的角标i
                .mapToObj(bizIds::get)// 用角标i取bizIds中的对应数据，就是点赞过的id
                .collect(Collectors.toSet());// 收集
    }

    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
        String key = RedisConstants.LIKE_COUNT_KEY_PREFIX + bizType;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().popMin(key, maxBizSize);
        List<LikedTimesDTO> list=new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            String bizId = typedTuple.getValue();
            Double likedTimes = typedTuple.getScore();
            if (bizId == null || likedTimes == null) {
                continue;
            }
            list.add(LikedTimesDTO.of(Long.valueOf(bizId), likedTimes.intValue()));
        }
        if (CollUtils.isEmpty(list)) {
            return;
        }
        rabbitMqHelper.send(MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
                StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, bizType),
                list);
    }
}
