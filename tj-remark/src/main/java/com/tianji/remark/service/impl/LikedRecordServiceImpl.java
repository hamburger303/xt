//package com.tianji.remark.service.impl;
//
//import com.tianji.api.client.user.UserClient;
//import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
//import com.tianji.common.constants.MqConstants;
//import com.tianji.common.exceptions.BadRequestException;
//import com.tianji.common.exceptions.BizIllegalException;
//import com.tianji.common.utils.BeanUtils;
//import com.tianji.common.utils.CollUtils;
//import com.tianji.common.utils.StringUtils;
//import com.tianji.common.utils.UserContext;
//import com.tianji.remark.domain.dto.LikeRecordFormDTO;
//import com.tianji.remark.domain.dto.LikedTimesDTO;
//import com.tianji.remark.domain.po.LikedRecord;
//import com.tianji.remark.mapper.LikedRecordMapper;
//import com.tianji.remark.service.ILikedRecordService;
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
///**
// * <p>
// * 点赞记录表 服务实现类
// * </p>
// *
// * @author 冯立翔
// * @since 2025-07-15
// */
//@Service
//@RequiredArgsConstructor
//public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
//
//    final RabbitMqHelper rabbitMqHelper;
//    @Override
//    public void addLikeRecord(LikeRecordFormDTO likeRecordFormDTO) {
//        Long userId = UserContext.getUser();
//        if(userId == null)
//            throw new BizIllegalException("用户未登录");
//        LikedRecord likedRecord = BeanUtils.copyBean(likeRecordFormDTO, LikedRecord.class);
//        likedRecord.setUserId(userId);
//        LikedRecord record = this.lambdaQuery()
//                .eq(LikedRecord::getUserId, userId)
//                .eq(LikedRecord::getBizId, likeRecordFormDTO.getBizId())
//                .eq(LikedRecord::getBizType, likeRecordFormDTO.getBizType())
//                .one();
//        boolean flag=false;
//        if(likeRecordFormDTO.getLiked()){
//            if(record != null)
//                return;
//            flag=this.save(likedRecord);
//        }else {
//            if(record == null)
//                return;
//            flag=this.removeById(record.getId());
//        }
//        if(!flag)
//            return;
//        Integer count = this.lambdaQuery()
//                .eq(LikedRecord::getBizId, likeRecordFormDTO.getBizId())
//                .eq(LikedRecord::getBizType, likeRecordFormDTO.getBizType())
//                .eq(LikedRecord::getUserId, userId)
//                .count();
//        LikedTimesDTO msg = LikedTimesDTO.builder()
//                .bizId(likeRecordFormDTO.getBizId())
//                .likedTimes(count)
//                .build();
//        rabbitMqHelper.send(
//                MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
//                StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, likeRecordFormDTO.getBizType()),
//                msg
//        );
//    }
//
//    @Override
//    public Set<Long> getLikesByBizIds(List<Long> bizIds) {
//        if(CollUtils.isEmpty(bizIds))
//            return CollUtils.emptySet();
//        Long user = UserContext.getUser();
//        if(user == null)
//            throw new BadRequestException("用户未登录");
//        List<LikedRecord> list = this.lambdaQuery()
//                .eq(LikedRecord::getUserId, user)
//                .in(LikedRecord::getBizId, bizIds)
//                .list();
//        return list.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
//    }
//}
