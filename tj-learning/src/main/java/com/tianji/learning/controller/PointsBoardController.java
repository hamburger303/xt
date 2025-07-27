package com.tianji.learning.controller;


import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.service.IPointsBoardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学霸天梯榜 前端控制器
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-16
 */
@Api(tags = "积分排行榜接口")
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class PointsBoardController {

    final IPointsBoardService pointsBoardService;

    @GetMapping
    @ApiOperation("查询积分排行榜")
    public PointsBoardVO queryPointsBoardList(PointsBoardQuery  query){
        return pointsBoardService.queryPointsBoardList(query);
    }
}
