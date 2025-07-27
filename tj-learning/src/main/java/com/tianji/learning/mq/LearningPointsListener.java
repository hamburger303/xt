package com.tianji.learning.mq;

import com.tianji.common.constants.MqConstants;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mq.msg.SignInMessage;
import com.tianji.learning.service.IPointsRecordService;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LearningPointsListener {

    final IPointsRecordService pointsRecordService;
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "sign.points.queue", durable = "true"),
                exchange = @Exchange(value = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
                key = MqConstants.Key.SIGN_IN))
    public void listenSignIn(SignInMessage signInMessage) {
        pointsRecordService.addPoints(signInMessage, PointsRecordType.SIGN);
    }
}
