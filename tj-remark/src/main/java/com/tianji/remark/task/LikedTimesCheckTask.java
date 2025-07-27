package com.tianji.remark.task;

import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LikedTimesCheckTask {

    final ILikedRecordService likedRecordService;

    static final List<String> BIZ_TYPE=List.of("QA","NOTE");
    static final int MAX_BIZ_SIZE=30;
    @Scheduled(cron = "0/20 * * * * ?")
    public void checkLikedTimes(){
        for (String bizType : BIZ_TYPE) {
            likedRecordService.readLikedTimesAndSendMessage(bizType, MAX_BIZ_SIZE);
        }
    }
}
