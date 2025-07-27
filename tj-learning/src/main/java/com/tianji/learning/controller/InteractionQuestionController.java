package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.query.QuestionPageQuery;
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
@Api("互动问题相关接口")
@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class InteractionQuestionController {

    final IInteractionQuestionService interactionQuestionService;

    @ApiOperation("新增互动问题")
    @PostMapping
    public void saveQuestion(@Validated @RequestBody QuestionFormDTO questionDTO) {
        interactionQuestionService.saveQuestion(questionDTO);
    }


    @ApiOperation("编辑互动问题")
    @PutMapping("/{id}")
    public void updateQuertion(@PathVariable("id") Long userid, @RequestBody QuestionFormDTO questionDTO){
        interactionQuestionService.updateQuestion(userid, questionDTO);
    }

    @GetMapping("page")
    @ApiOperation("分页查询互动问题-用户端")
    public PageDTO<QuestionVO> queryQuestionsPage(QuestionPageQuery query){
        return interactionQuestionService.queryQuestionsPage(query);
    }

    @ApiOperation("查询互动问题详情-用户端")
    @GetMapping("/{id}")
    public QuestionVO queryQuestionById(@PathVariable("id") Long id){
        return interactionQuestionService.queryQuestionById(id);
    }

    @ApiOperation("删除提问")
    @DeleteMapping("/{id}")
    public boolean deleteMyQuestionById(@PathVariable("id")Long id){
        return interactionQuestionService.deleteMyQuestionById(id);
    }


}
