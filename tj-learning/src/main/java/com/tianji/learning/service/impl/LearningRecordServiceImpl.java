package com.tianji.learning.service.impl;

import cn.hutool.db.DbRuntimeException;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.utils.LearningRecordDelayTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-08
 */
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {

    final ILearningLessonService lessonService;

    final CourseClient courseClient;

    final LearningRecordDelayTaskHandler learningRecordDelayTaskHandler;

    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        Long userId = UserContext.getUser();
        if (userId==null)
            throw new BadRequestException("需要登录");
        LearningLesson learningLesson = lessonService.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .select(LearningLesson::getId, LearningLesson::getLatestSectionId)
                .one();
        if(learningLesson==null)
            throw new BizIllegalException("没有该课程的记录");
        Long lessonId = learningLesson.getId();
        List<LearningRecord> learningRecordList = this.lambdaQuery()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getLessonId, lessonId)
                .list();
        LearningLessonDTO dto=new LearningLessonDTO();
        dto.setId(learningLesson.getId());
        dto.setLatestSectionId(learningLesson.getLatestSectionId());
        List<LearningRecordDTO> learningRecordDTOS = BeanUtils.copyList(learningRecordList, LearningRecordDTO.class);
        dto.setRecords(learningRecordDTOS);
        return dto;
    }

    @Override
    public void addLearningRecord(LearningRecordFormDTO record) {
        Long userId = UserContext.getUser();
        boolean Finished=false;
        if (record.getSectionType() == SectionType.VIDEO) {
            Finished=handleVideoRecord(record, userId);
        }else {
            Finished=handleExamRecord(record, userId);
        }
        if(!Finished){
            return;
        }
        handleLessonData(record);
    }

    private void handleLessonData(LearningRecordFormDTO record) {
        Long lessonId = record.getLessonId();
        LearningLesson learningLesson = lessonService.lambdaQuery()
                .eq(LearningLesson::getId, lessonId)
                .one();
        if (learningLesson==null)
            throw new BizIllegalException("课表不存在");
        boolean allFinished=false;
        CourseFullInfoDTO infoDTO = courseClient.getCourseInfoById(learningLesson.getCourseId(), false, false);
        if(infoDTO==null){
            throw new BizIllegalException("课程不存在");
        }
        Integer sectionNum = infoDTO.getSectionNum();
        Integer sections = learningLesson.getLearnedSections();
        sections+=1;
        allFinished=sections>=sectionNum;
        lessonService.lambdaUpdate()
                .set(learningLesson.getStatus()==LessonStatus.NOT_BEGIN,LearningLesson::getStatus,LessonStatus.LEARNING)
                .set(allFinished,LearningLesson::getStatus, LessonStatus.FINISHED)
                .set(LearningLesson::getLearnedSections, learningLesson.getLearnedSections()+1)
                .set(LearningLesson::getLatestSectionId, record.getSectionId())
                .set(LearningLesson::getLatestLearnTime,record.getCommitTime())
                .eq(LearningLesson::getId, learningLesson.getId())
                .update();
    }

    private boolean handleVideoRecord(LearningRecordFormDTO record, Long userId) {
        LearningRecord learningRecord = learningRecordDelayTaskHandler.readRecordCache(record.getLessonId(), record.getSectionId());
        if(learningRecord == null) {
             learningRecord = this.lambdaQuery()
                    .eq(LearningRecord::getUserId, userId)
                    .eq(LearningRecord::getLessonId, record.getLessonId())
                    .eq(LearningRecord::getSectionId, record.getSectionId())
                    .one();
        }
        if(learningRecord != null)
            learningRecordDelayTaskHandler.writeRecordCache(learningRecord);
        if(learningRecord == null){
            LearningRecord learningRecord1 = BeanUtils.copyBean(record, LearningRecord.class);
            learningRecord1.setUserId(userId);
            boolean result = this.save(learningRecord1);
            if (!result) {
                throw new DbException("保存学习记录失败");
            }
            return false;
        }
        boolean isFinished =!learningRecord.getFinished()&&record.getMoment()>= record.getDuration()*0.9;
        if(!isFinished){
            LearningRecord learningRecord1 = new LearningRecord();
            learningRecord1.setFinished(false);
            learningRecord1.setId(learningRecord.getId());
            learningRecord1.setMoment(record.getMoment());
            learningRecord1.setSectionId(record.getSectionId());
            learningRecord1.setLessonId(record.getLessonId());
            learningRecordDelayTaskHandler.addLearningRecordTask(learningRecord1);
            return false;
        }
        boolean update = this.lambdaUpdate()
                .set(isFinished, LearningRecord::getFinished, true)
                .set(isFinished,LearningRecord::getFinishTime, record.getCommitTime())
                .set(LearningRecord::getMoment, record.getMoment())
                .eq(LearningRecord::getId, learningRecord.getId())
                .update();
        if (!update) {
            throw new DbException("更新学习记录失败");
        }
        learningRecordDelayTaskHandler.cleanRecordCache(record.getLessonId(), record.getSectionId());
        return true;
    }

    private boolean handleExamRecord(LearningRecordFormDTO record, Long userId) {
        LearningRecord learningRecord = BeanUtils.copyBean(record, LearningRecord.class);
        learningRecord.setUserId(userId);
        learningRecord.setFinished(true);
        learningRecord.setFinishTime(record.getCommitTime());
        boolean result = this.save(learningRecord);
        if (!result) {
            throw new DbException("保存学习记录失败");
        }
        return true;
    }
}
