package com.tianji.learning.controller;


import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.service.IPointsRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 前端控制器
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-16
 */
@Api(tags = "积分记录接口")
@RestController
@RequestMapping("/points")

public class PointsRecordController {

    IPointsRecordService pointsRecordService;
    @ApiOperation("查询我的积分今日情况")
    @GetMapping("/today")
    public List<PointsStatisticsVO> queryMyTodayPoints(){
        return pointsRecordService.queryMyTodayPoints();
    }

}
