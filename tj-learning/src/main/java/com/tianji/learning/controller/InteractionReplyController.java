package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionReplyService;
import com.tianji.learning.service.impl.InteractionReplyServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动问题的回答或评论 前端控制器
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-11
 */
@RestController
@Api("互动问题回答或评论")
@RequestMapping("/replies")
@RequiredArgsConstructor
public class InteractionReplyController {

    final IInteractionReplyService interactionReplyService;

    @ApiOperation("新增回答或评论")
    @PostMapping
    public void addReply(@RequestBody @Validated ReplyDTO replyDTO){
        interactionReplyService.addReply(replyDTO);
    }

    @ApiOperation("分页查询回答或评论列表")
    @GetMapping("/page")
    public PageDTO<ReplyVO> page(ReplyPageQuery pageQuery){
        return interactionReplyService.pageUser(pageQuery);
    }
}
