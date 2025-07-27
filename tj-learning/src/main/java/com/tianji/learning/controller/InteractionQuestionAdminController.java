package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-11
 */
@Api("管理端互动问题相关接口")
@RestController
@RequestMapping("/admin/questions")
@RequiredArgsConstructor
public class InteractionQuestionAdminController {

    final IInteractionQuestionService interactionQuestionService;

    @ApiOperation("分页查询-管理端")
    @GetMapping("/page")
    public PageDTO<QuestionAdminVO> queryQuestionsAdminPage(QuestionAdminPageQuery query){
        return interactionQuestionService.queryQuestionsAdminPage(query);
    }
}
