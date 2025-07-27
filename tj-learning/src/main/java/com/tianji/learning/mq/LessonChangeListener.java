package com.tianji.learning.mq;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LessonChangeListener {

    final ILearningLessonService lessonService;
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "learning.lesson.pay.queue", durable = "true"),
                                                            exchange = @Exchange(value=MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
                                                            key = MqConstants.Key.ORDER_PAY_KEY))
    public void listenLessonPay(OrderBasicDTO orderBasicDTO){
        log.info("订单支付成功，开始处理课程信息");
        log.info("订单信息：{}", orderBasicDTO);
        if(orderBasicDTO.getOrderId() == null || orderBasicDTO.getUserId() == null || CollUtils.isEmpty(orderBasicDTO.getCourseIds())){
            return;
        }
        lessonService.addUserLesson(orderBasicDTO.getUserId(), orderBasicDTO.getCourseIds());
    }

    @RabbitListener(bindings = @QueueBinding(value=@Queue(value = "learning.lesson.refund.queue", durable = "true"),
                                                            exchange = @Exchange(value = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
                                                            key = MqConstants.Key.ORDER_REFUND_KEY))
    public void receiveMsg(OrderBasicDTO orderBasicDTO){
        log.info("订单退款成功，开始处理课程信息");
        if(orderBasicDTO.getOrderId() == null
                || orderBasicDTO.getUserId() == null
                || CollUtils.isEmpty(orderBasicDTO.getCourseIds()))
            return;
        lessonService.deleteLessionById(orderBasicDTO.getUserId(),orderBasicDTO.getCourseIds());
    }
}
