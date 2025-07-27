package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;

/**
 * <p>
 * 学生课程表 前端控制器
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-07
 */
@Api("我的课程相关接口")
@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LearningLessonController {

    final ILearningLessonService learningLessonService;

    @GetMapping("/page")
    @ApiOperation("分页查询我的课程")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery pageQuery) {
        return learningLessonService.queryMyLessons(pageQuery);
    }

    @GetMapping("/now")
    @ApiOperation("查询当前正在学习的课程")
    public LearningLessonVO queryNowLesson() {
        return learningLessonService.queryMyCurrentLesson();
    }

    @GetMapping("/{courseId}/valid")
    @ApiOperation("查询课程是否有效")
    public Long IsLessonValid(@PathVariable Long courseId) {
        return learningLessonService.isLessonValid(courseId);
    }

    @GetMapping("/{courseId}")
    @ApiOperation("查询用户课表中指定课程状态")
    public LearningLessonVO getLessonInfo(@PathVariable Long courseId){
        return learningLessonService.getLessonInfo(courseId);
    }

    @DeleteMapping("/{courseId}")
    @ApiOperation("删除用户课表中指定课程")
    public Boolean deleteLessonById(@PathVariable Long courseId){
        Long userId = UserContext.getUser();
        if(userId == null){
            throw new RuntimeException("用户未登录");
        }
        return learningLessonService.deleteLessionById(userId, Collections.singletonList(courseId));
    }

    @ApiOperation("查询课程表中指定课程数量")
    @GetMapping("/{courseId}/count")
    public Integer countLearningLessonByCourse(@PathVariable("courseId") Long courseId){
        return learningLessonService.countLearningLessonByCourse(courseId);
    }

    @ApiOperation("创建学习计划")
    @PostMapping("/plans")
    public void createLearningPlans(@Validated @RequestBody LearningPlanDTO planDTO){
        learningLessonService.createLearningPlan(planDTO);
    }

    @GetMapping("/plans")
    @ApiOperation("分页查询学习计划")
    public LearningPlanPageVO queryMyPlans(PageQuery pageQuery){
        return learningLessonService.queryMyPlans(pageQuery);
    }
}
