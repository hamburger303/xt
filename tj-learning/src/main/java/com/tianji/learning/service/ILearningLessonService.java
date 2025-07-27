package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;

import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-07
 */
public interface ILearningLessonService extends IService<LearningLesson> {

    void addUserLesson(Long userId, List<Long> courseIds);

    PageDTO<LearningLessonVO> queryMyLessons(PageQuery pageQuery);

    LearningLessonVO queryMyCurrentLesson();

    Long isLessonValid(Long courseId);

    LearningLessonVO getLessonInfo(Long courseId);


    Boolean deleteLessionById(Long userId, List<Long> courseIds);

    Integer countLearningLessonByCourse(Long courseId);

    void createLearningPlan(LearningPlanDTO planDTO);

    LearningPlanPageVO queryMyPlans(PageQuery pageQuery);
}
