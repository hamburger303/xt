package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.Contstants.RedisContstants;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.mq.msg.SignInMessage;
import com.tianji.learning.service.IPointsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-16
 */
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {

    final StringRedisTemplate redisTemplate;
    @Override
    public void addPoints(SignInMessage signInMessage, PointsRecordType sign) {
        if(signInMessage.getUserId()== null|| signInMessage.getPoints()== null)
            return;
        int maxPoints = sign.getMaxPoints();
        int realPoints = signInMessage.getPoints();
        if(maxPoints>0){
            QueryWrapper<PointsRecord> query = new QueryWrapper<>();
            query.select("sum(points) as totalPoints");
            query.eq("user_id", signInMessage.getUserId());
            query.eq("type", sign);
            query.between("create_time", LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay());
            Map<String, Object> map = this.getMap(query);
            int currentPoints = 0;
            if(map!= null){
                BigDecimal totalPoints = (BigDecimal) map.get("totalPoints");
                currentPoints = totalPoints.intValue();
            }
            if(currentPoints>=maxPoints){
                return;
            }
            if(maxPoints-currentPoints<realPoints){
                realPoints=maxPoints-currentPoints;
            }
        }
        PointsRecord record = new PointsRecord();
        record.setUserId(signInMessage.getUserId());
        record.setPoints(realPoints);
        record.setType(sign);
        save(record);
        String format = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        redisTemplate.opsForZSet().incrementScore(RedisContstants.POINTS_BOARD_KEY_PREFIX+format,
                signInMessage.getUserId().toString(),
                realPoints);
    }

    @Override
    public List<PointsStatisticsVO> queryMyTodayPoints() {
        Long user = UserContext.getUser();
        QueryWrapper<PointsRecord> pointsRecordQueryWrapper = new QueryWrapper<>();
        pointsRecordQueryWrapper.select("type", "sum(points) as points")
                .eq("user_id", user)
                .between("create_time", LocalDate.now().atStartOfDay(),LocalDate.now().plusDays(1).atStartOfDay())
                .groupBy("type");
        List<PointsRecord> list = this.list(pointsRecordQueryWrapper);
        if(CollUtils.isEmpty( list))
            return CollUtils.emptyList();
        List<PointsStatisticsVO> result = new ArrayList<>();
        for (PointsRecord pointsRecord : list) {
            PointsStatisticsVO pointsStatisticsVO = new PointsStatisticsVO();
            pointsStatisticsVO.setType(pointsRecord.getType().getDesc());
            pointsStatisticsVO.setPoints(pointsRecord.getPoints());
            pointsStatisticsVO.setMaxPoints(pointsRecord.getType().getMaxPoints());
            result.add(pointsStatisticsVO);
        }
        return result;
    }
}
