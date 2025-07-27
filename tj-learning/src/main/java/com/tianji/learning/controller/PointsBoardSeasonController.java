package com.tianji.learning.controller;


import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.vo.PointsBoardSeasonVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.service.IPointsBoardSeasonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-16
 */
@Api(tags = "积分排行榜季接口")
@RestController
@RequestMapping("/boards/seasons")
@RequiredArgsConstructor
public class PointsBoardSeasonController {

    final IPointsBoardSeasonService pointsBoardSeasonService;
    @ApiOperation("查询积分排行榜")
    @GetMapping("/list")
    public List<PointsBoardSeason> queryPointsBoard(){
        return pointsBoardSeasonService.queryPointsBoard();
    }


}
