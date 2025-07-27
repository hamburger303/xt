package com.tianji.learning.mq;

import com.tianji.api.dto.msg.LikedTimesDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class LikedRecordListener {

    final IInteractionReplyService replyService;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = "qa.liked.times.queue",durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.LIKE_RECORD_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.QA_LIKED_TIMES_KEY))
    public void onMsg(List<LikedTimesDTO> list){
        List<InteractionReply> replies=new ArrayList<>();
        for (LikedTimesDTO likedTimesDTO : list) {
            InteractionReply reply = new InteractionReply();
            reply.setId(likedTimesDTO.getBizId());
            reply.setLikedTimes(likedTimesDTO.getLikedTimes());
            replies.add(reply);
        }
        replyService.updateBatchById(replies);
    }


//    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = "qa.liked.times.queue",durable = "true"),
//            exchange = @Exchange(value = MqConstants.Exchange.LIKE_RECORD_EXCHANGE,type = ExchangeTypes.TOPIC),
//            key = MqConstants.Key.QA_LIKED_TIMES_KEY))
//    public void onMsg(LikedTimesDTO dto){
//        InteractionReply reply = replyService.getById(dto.getBizId());
//        if(reply == null)
//            return;
//        reply.setLikedTimes(dto.getLikedTimes());
//        replyService.updateById(reply);
//    }
}
