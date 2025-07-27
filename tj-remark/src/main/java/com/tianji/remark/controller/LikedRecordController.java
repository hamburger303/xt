package com.tianji.remark.controller;


import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.dto.LikedTimesDTO;
import com.tianji.remark.service.ILikedRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 前端控制器
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-15
 */
@Api(tags = "点赞相关接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/likes")
public class LikedRecordController {

    final ILikedRecordService likedRecordService;

    @PostMapping
    @ApiOperation("点赞或取消")
    public void addLikeRecord(@RequestBody @Validated LikeRecordFormDTO likeRecordFormDTO){
        likedRecordService.addLikeRecord(likeRecordFormDTO);
    }

    @GetMapping("/list")
    @ApiOperation("查询点赞列表")
    public Set<Long> getLikesByBizIds(@RequestParam("bizIds") List<Long> bizIds){
        return likedRecordService.getLikesByBizIds(bizIds);
    }
}
