package com.tianji.learning.controller;

import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.service.ISignRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Api(tags = "签到记录接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/sign-records")
public class SignRecordController {

    final ISignRecordService signRecordService;
    @ApiOperation("签到")
    @PostMapping
    public SignResultVO addSignRecords() {
        return signRecordService.addSignRecords();
    }

    @GetMapping
    @ApiOperation("查询签到记录")
    public List<Integer> querySignRecords(){
        return signRecordService.querySignRecords();
    }

}
