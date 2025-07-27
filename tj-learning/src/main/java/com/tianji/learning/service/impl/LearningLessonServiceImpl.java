package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.unit.DataUnit;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import  com.tianji.common.utils.BeanUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-07
 */
@Service
@RequiredArgsConstructor
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {

    final CourseClient courseClient;

    final CatalogueClient catalogueClient;

    final LearningRecordMapper learningRecordMapper;
    @Override
    @Transactional
    public void addUserLesson(Long userId, List<Long> courseIds) {
        List<CourseSimpleInfoDTO> list = courseClient.getSimpleInfoList(courseIds);
        List<LearningLesson> lessons = new ArrayList<>();
        for (CourseSimpleInfoDTO dto : list) {
            LearningLesson learningLesson=new LearningLesson();
            learningLesson.setUserId(userId);
            learningLesson.setCourseId(dto.getId());
            Integer validDuration = dto.getValidDuration();
            if(validDuration!=null) {
                learningLesson.setExpireTime(LocalDateTime.now().plusMonths(validDuration));
                learningLesson.setCreateTime(LocalDateTime.now());
            }
            lessons.add(learningLesson);
        }
        this.saveBatch(lessons);
    }

    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery pageQuery) {
        Long userId = UserContext.getUser();
        if(userId==null)
            throw new BadRequestException("需要登录");
        Page<LearningLesson> lessonPage = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .page(pageQuery.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = lessonPage.getRecords();
        if(CollUtils.isEmpty(records)){
            return PageDTO.empty(lessonPage);
        }
        List<CourseSimpleInfoDTO> list = courseClient.getSimpleInfoList(records.stream().map(LearningLesson::getCourseId).collect(Collectors.toList()));
        if(CollUtils.isEmpty(list)){
            throw new BizIllegalException("没有该课程");
        }
        Map<Long, CourseSimpleInfoDTO> DTOMap = list.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        List<LearningLessonVO> listVO = new ArrayList<>();
        for (LearningLesson record : records) {
            LearningLessonVO vo = new LearningLessonVO();
            BeanUtils.copyProperties(record, vo);
            CourseSimpleInfoDTO courseSimpleInfoDTO = DTOMap.get(record.getCourseId());
            if(courseSimpleInfoDTO!=null) {
                vo.setCourseName(courseSimpleInfoDTO.getName());
                vo.setCourseCoverUrl(courseSimpleInfoDTO.getCoverUrl());
                vo.setSections(courseSimpleInfoDTO.getSectionNum());
            }
            listVO.add(vo);
        }
        return PageDTO.of(lessonPage, listVO);
    }

    @Override
    public LearningLessonVO queryMyCurrentLesson() {
        Long userId = UserContext.getUser();
        if(userId==null)
            throw new BadRequestException("需要登录");
        LearningLesson learningLesson = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING)
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1")
                .one();
        if(learningLesson==null)
            return null;
        CourseFullInfoDTO infoDTO = courseClient.getCourseInfoById(learningLesson.getCourseId(), false, false);
        if(infoDTO==null)
            throw new BizIllegalException("课程不存在");
        LearningLessonVO vo = BeanUtil.copyProperties(learningLesson, LearningLessonVO.class);
        vo.setCourseName(infoDTO.getName());
        vo.setCourseCoverUrl(infoDTO.getCoverUrl());
        vo.setSections(infoDTO.getSectionNum());
        vo.setCourseAmount(lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .count());
        List<CataSimpleInfoDTO> cataSimpleInfoDTOS = catalogueClient.batchQueryCatalogue(CollUtils.singletonList(learningLesson.getLatestSectionId()));
        if(CollUtils.isEmpty(cataSimpleInfoDTOS)){
            throw new BizIllegalException("课程目录不存在");
        }
        vo.setLatestSectionName(cataSimpleInfoDTOS.get(0).getName());
        vo.setLatestSectionIndex(cataSimpleInfoDTOS.get(0).getCIndex());
        return vo;
    }

    @Override
    public Long isLessonValid(Long courseId) {
        Long userId = UserContext.getUser();
        if(userId==null)
            throw new BadRequestException("需要登录");
        LearningLesson learningLesson = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        if(learningLesson==null)
            return null;
        if(learningLesson.getExpireTime()!=null&&learningLesson.getExpireTime().isBefore(LocalDateTime.now()))
            return null;
        return learningLesson.getId();
    }

    @Override
    public LearningLessonVO getLessonInfo(Long courseId) {
        Long userId = UserContext.getUser();
        if(userId==null)
            throw new BadRequestException("需要登录");
        LearningLesson learningLesson = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        if(learningLesson==null)
            return null;
        return BeanUtil.copyProperties(learningLesson, LearningLessonVO.class);
    }

    @Override
    public Boolean deleteLessionById(Long userId, List<Long> courseIds) {
        LambdaQueryChainWrapper<LearningLesson> wrapper = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .in(LearningLesson::getCourseId, courseIds);
        return this.remove(wrapper);
    }

    @Override
    public Integer countLearningLessonByCourse(Long courseId) {
        return this.lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .count();
    }

    @Override
    public void createLearningPlan(LearningPlanDTO planDTO) {
        Long userId = UserContext.getUser();
        if(userId==null)
            throw new BadRequestException("需要登录");
        LearningLesson learningLesson = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, planDTO.getCourseId())
                .one();
        if(learningLesson==null)
            throw new BizIllegalException("该课程没有加入课表");
        this.lambdaUpdate()
                .eq(LearningLesson::getId, learningLesson.getId())
                .set(LearningLesson::getWeekFreq, planDTO.getFreq())
                .set(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .update();
    }

    @Override
    public LearningPlanPageVO queryMyPlans(PageQuery pageQuery) {
        Long userId = UserContext.getUser();
        if(userId==null)
            throw new BadRequestException("需要登录");
        LearningPlanPageVO vo=new LearningPlanPageVO();
        QueryWrapper<LearningLesson> wrapper = new QueryWrapper<>();
        wrapper.select("sum(week_freq) as plansTotal");
        wrapper.eq("user_id", userId);
        wrapper.eq("plan_status", PlanStatus.PLAN_RUNNING);
        wrapper.in("status", LessonStatus.LEARNING, LessonStatus.NOT_BEGIN);
        Map<String, Object> map = this.getMap(wrapper);
        Integer plansTotal=0;
        if(map!=null&&map.get("plansTotal")!=null){
             plansTotal = Integer.valueOf(map.get("plansTotal").toString());
        }
        LocalDate now = LocalDate.now();
        LocalDateTime weekBeginTime = DateUtils.getWeekBeginTime(now);
        LocalDateTime weekEndTime = DateUtils.getWeekEndTime(now);
        Integer i = learningRecordMapper.selectCount(Wrappers.<LearningRecord>lambdaQuery()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getFinished, true)
                .between(LearningRecord::getFinishTime, weekBeginTime, weekEndTime));
        Page<LearningLesson> lessonPage = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .in(LearningLesson::getStatus, LessonStatus.LEARNING, LessonStatus.NOT_BEGIN)
                .page(pageQuery.toMpPage("latest_learn_time", false));
        List<LearningPlanVO> list = new ArrayList<>();
        List<LearningLesson> records = lessonPage.getRecords();
        if (CollUtils.isEmpty(records))
            return null;
        List<Long> collect = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toList());
        List<CourseSimpleInfoDTO> courseinfoList = courseClient.getSimpleInfoList(collect);
        if(CollUtils.isEmpty(courseinfoList))
            throw new BizIllegalException("课程信息不存在");
        QueryWrapper<LearningRecord> wrapper1 = new QueryWrapper<>();
        wrapper1.select("lesson_id as lessonId  , count(*) as userId");
        wrapper1.eq("user_id", userId);
        wrapper1.eq("finished",true);
        wrapper1.between("finish_time", weekBeginTime, weekEndTime);
        wrapper1.groupBy("lesson_id");
        List<LearningRecord> learningRecords = learningRecordMapper.selectList(wrapper1);
        Map<Long, Long> map1 = learningRecords.stream().collect(Collectors.toMap(LearningRecord::getLessonId, LearningRecord::getUserId));
        Map<Long, CourseSimpleInfoDTO> dtoMap = courseinfoList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        for (LearningLesson record : records) {
            LearningPlanVO learningPlanVO = BeanUtils.copyBean(record, LearningPlanVO.class);
            CourseSimpleInfoDTO courseSimpleInfoDTO = dtoMap.get(record.getCourseId());
            if(courseSimpleInfoDTO!=null) {
                learningPlanVO.setCourseName(courseSimpleInfoDTO.getName());
                learningPlanVO.setSections(courseSimpleInfoDTO.getSectionNum());
            }
            Long l = map1.get(record.getId());
            if(l!=null)
                learningPlanVO.setWeekLearnedSections(l.intValue());
            else
                learningPlanVO.setWeekLearnedSections(0);
            list.add(learningPlanVO);
        }
        vo.setWeekTotalPlan(plansTotal);
        vo.setWeekFinished(i);
        vo.setPages(lessonPage.getPages());
        vo.setTotal(lessonPage.getTotal());
        vo.setList(list);
        return vo;
    }


}
