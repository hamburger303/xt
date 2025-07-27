package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-11
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

    final UserClient userClient;

    final IInteractionReplyService interactionReplyService;

    final SearchClient searchClient;

    final CourseClient courseClient;

    final CatalogueClient catalogueClient;

    final CategoryCache categoryCache;
    @Override
    public void saveQuestion(QuestionFormDTO questionDTO) {
        Long user = UserContext.getUser();
        InteractionQuestion interactionQuestion = BeanUtils.copyBean(questionDTO, InteractionQuestion.class);
        interactionQuestion.setUserId(user);
        this.save(interactionQuestion);
    }

    @Override
    public void updateQuestion(Long userid, QuestionFormDTO questionDTO) {
        if(questionDTO.getAnonymity() == null||
                StringUtils.isBlank(questionDTO.getTitle())||
                StringUtils.isBlank(questionDTO.getDescription()))
            throw new BadRequestException("参数错误");
        InteractionQuestion interactionQuestion = this.getById(userid);
        if(interactionQuestion == null)
            throw new BizIllegalException("无内容");
        Long user = UserContext.getUser();
        if(!interactionQuestion.getUserId().equals(user))
            throw new BadRequestException("无权限");
        this.lambdaUpdate()
                .eq(InteractionQuestion::getId, userid)
                .set(InteractionQuestion::getTitle, questionDTO.getTitle())
                .set(InteractionQuestion::getDescription, questionDTO.getDescription())
                .set(InteractionQuestion::getAnonymity, questionDTO.getAnonymity())
                .update();
    }

    @Override
    public PageDTO<QuestionVO> queryQuestionsPage(QuestionPageQuery query) {
        if (query.getCourseId()== null)
            throw new BadRequestException("参数错误");
        Long user = UserContext.getUser();
        Page<InteractionQuestion> questionPage = this.lambdaQuery()
                .select(InteractionQuestion.class, info -> !info.getProperty().equals("description"))
                .eq(InteractionQuestion::getCourseId, query.getCourseId())
                .eq(query.getOnlyMine(), InteractionQuestion::getUserId, user)
                .eq(InteractionQuestion::getHidden, false)
                .eq(query.getSectionId() != null, InteractionQuestion::getSectionId, query.getSectionId())
                .page(query.toMpPage("create_time", false));
        if(questionPage.getTotal() == 0)
            return PageDTO.empty(questionPage);
        List<InteractionQuestion> records = questionPage.getRecords();
        Set<Long> collect = records.stream().
                filter(c->c.getLatestAnswerId()!= null).
                map(InteractionQuestion::getLatestAnswerId).
                collect(Collectors.toSet());
        Set<Long> collectUserId = records.stream().filter(c -> c.getAnonymity() != null)
                .map(InteractionQuestion::getUserId)
                .collect(Collectors.toSet());
        Map<Long, InteractionReply> interactionReplyMap=new HashMap<>();
        if(!CollUtils.isEmpty(collect)) {
          /*List<InteractionReply> interactionReplies = interactionReplyService.listByIds(collect);*/
            List<InteractionReply> interactionReplies = interactionReplyService.list(Wrappers
                    .<InteractionReply>lambdaQuery()
                    .in(InteractionReply::getId, collect)
                    .eq(InteractionReply::getHidden, false));
            if(!CollUtils.isEmpty(interactionReplies)) {
                interactionReplyMap = interactionReplies.stream().collect(Collectors.toMap(InteractionReply::getId, c -> c));
                for (InteractionReply interactionReply : interactionReplies) {
                    if (interactionReply.getAnonymity() != null)
                        collectUserId.add(interactionReply.getUserId());
                }
            }
        }
        Map<Long, UserDTO> userDTOMap = new HashMap<>();
        if(!CollUtils.isEmpty(collectUserId)) {
            List<UserDTO> userDTOS = userClient.queryUserByIds(collectUserId);
            if(!CollUtils.isEmpty(userDTOS))
                userDTOMap = userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, c -> c));
        }
        List<QuestionVO> questionVOS = new ArrayList<>();
        for (InteractionQuestion record : records) {
            QuestionVO questionVO = BeanUtils.copyBean(record, QuestionVO.class);
                if (!questionVO.getAnonymity()) {
                    UserDTO userDTO = userDTOMap.get(record.getUserId());
                    if(userDTO != null){
                        questionVO.setUserName(userDTO.getName());
                        questionVO.setUserIcon(userDTO.getIcon());
                    }
                }
                InteractionReply reply = interactionReplyMap.get(record.getLatestAnswerId());
                if (reply != null) {
                    UserDTO userDTO = userDTOMap.get(reply.getUserId());
                    questionVO.setLatestReplyContent(reply.getContent());
                    if (!reply.getAnonymity()&&userDTO!= null)
                        questionVO.setLatestReplyUser(userDTO.getName());
                }
                questionVOS.add(questionVO);
        }
        return PageDTO.of(questionPage, questionVOS);
    }

    @Override
    public QuestionVO queryQuestionById(Long id) {
        if(id == null)
            throw new BadRequestException("参数错误");
        InteractionQuestion interactionQuestion = this.getById(id);
        if(interactionQuestion == null) {
            throw new BadRequestException("问题不存在");
        }
        Boolean hidden = interactionQuestion.getHidden();
        if(hidden)
            return null;
        Boolean anonymity = interactionQuestion.getAnonymity();
        QuestionVO questionVO = BeanUtils.copyBean(interactionQuestion, QuestionVO.class);
        if(!anonymity){
            UserDTO userDTO = userClient.queryUserById(interactionQuestion.getUserId());
            if(userDTO != null){
                questionVO.setUserName(userDTO.getName());
                questionVO.setUserIcon(userDTO.getIcon());
            }
        }
        return questionVO;
    }

    @Override
    public PageDTO<QuestionAdminVO> queryQuestionsAdminPage(QuestionAdminPageQuery query) {
        String courseName = query.getCourseName();
        List<Long> list=null;
        if(StringUtils.isNotBlank(courseName)) {
            list = searchClient.queryCoursesIdByName(courseName);
            if(CollUtils.isEmpty(list))
                return PageDTO.empty(0L, 0L);
        }
        Page<InteractionQuestion> questionPage = this.lambdaQuery()
                .eq(query.getStatus() != null, InteractionQuestion::getStatus, query.getStatus())
                .between(query.getBeginTime() != null && query.getEndTime() != null, InteractionQuestion::getCreateTime, query.getBeginTime(), query.getEndTime())
                .in(CollUtils.isNotEmpty(list),InteractionQuestion::getCourseId, list)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = questionPage.getRecords();
        if (CollUtils.isEmpty(records)){
            return PageDTO.empty(0L,0L);
        }
        HashSet<Long> uids = new HashSet<>();
        HashSet<Long> cids = new HashSet<>();
        HashSet<Long> chapterAndSectionids = new HashSet<>();
        for (InteractionQuestion record : records) {
            uids.add(record.getUserId());
            cids.add(record.getCourseId());
            if (record.getChapterId() != null&&record.getSectionId() != null)
                chapterAndSectionids.add(record.getChapterId());
        }
        List<UserDTO> userDTOS = userClient.queryUserByIds(uids);
        List<CourseSimpleInfoDTO> courselist = courseClient.getSimpleInfoList(cids);
        List<CataSimpleInfoDTO> cataSimpleInfoDTOS = catalogueClient.batchQueryCatalogue(chapterAndSectionids);
        if(CollUtils.isEmpty(userDTOS))
            throw new BizIllegalException("用户数据有误");
        if(CollUtils.isEmpty(courselist))
            throw new BizIllegalException("课程数据有误");
        if(CollUtils.isEmpty(cataSimpleInfoDTOS))
            throw new BizIllegalException("目录数据有误");
        Map<Long, CourseSimpleInfoDTO> courseSimpleInfoDTOMap = courselist.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        Map<Long, UserDTO> userDTOMap = userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, c -> c));
        Map<Long, CataSimpleInfoDTO> cataSimpleInfoDTOMap = cataSimpleInfoDTOS.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, c -> c));
        List<QuestionAdminVO> listVo=new ArrayList<>();
        for (InteractionQuestion record : records) {
            QuestionAdminVO questionAdminVO = BeanUtils.copyBean(record, QuestionAdminVO.class);
            UserDTO userDTO = userDTOMap.get(record.getUserId());
            if(userDTO != null)
                questionAdminVO.setUserName(userDTO.getName());
            CourseSimpleInfoDTO courseSimpleInfoDTO = courseSimpleInfoDTOMap.get(record.getCourseId());
            if(courseSimpleInfoDTO != null)
                questionAdminVO.setCourseName(courseSimpleInfoDTO.getName());
            CataSimpleInfoDTO cataSimpleInfoDTO = cataSimpleInfoDTOMap.get(record.getChapterId());
            if(cataSimpleInfoDTO != null)
                questionAdminVO.setCategoryName(cataSimpleInfoDTO.getName());
            CataSimpleInfoDTO sectionInfo = cataSimpleInfoDTOMap.get(record.getSectionId());
            if(sectionInfo != null)
                questionAdminVO.setSectionName(sectionInfo.getName());
            List<Long> categoryIds = null;
            if (courseSimpleInfoDTO != null) {
                categoryIds = courseSimpleInfoDTO.getCategoryIds();
                questionAdminVO.setCategoryName(categoryCache.getCategoryNames(categoryIds));
            }
            listVo.add(questionAdminVO);
        }
        return PageDTO.of(questionPage, listVo);
    }

    @Override
    public boolean deleteMyQuestionById(Long id) {
        InteractionQuestion interactionQuestion = this.getById(id);
        if(interactionQuestion == null)
            throw new BizIllegalException("问题不存在");
        Long user = UserContext.getUser();
        if(!Objects.equals(id, user)){
            throw new BizIllegalException("无权限删除该问题");
        }
        this.removeById(interactionQuestion.getId());
        return interactionReplyService.remove(Wrappers.<InteractionReply>lambdaQuery()
                .eq(InteractionReply::getQuestionId, interactionQuestion.getId()));
    }
}
