package com.tianji.promotion.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.service.ICouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 前端控制器
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-19
 */
@Api("优惠券相关接口")
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {


    final ICouponService couponService;
    @PostMapping
    @ApiOperation("新增优惠券")
    public void saveCoupon(@RequestBody @Validated CouponFormDTO dto){
        couponService.saveCoupon(dto);
    }

    @GetMapping("/page")
    @ApiOperation("分页查询优惠券列表")
    public PageDTO<CouponDetailVO> queryCouponPage(CouponQuery  query){
        return couponService.queryCouponByPage(query);
    }

    @PutMapping("{id}/issue")
    @ApiOperation("发放优惠券")
    public void issueCoupon(@PathVariable Long id, @RequestBody @Validated CouponIssueFormDTO dto){
        couponService.issueCoupon(id,dto);
    }

    @GetMapping("/list")
    @ApiOperation("查询发放中的优惠券列表")
    public List<CouponVO> queryIssuingCoupons(){
        return couponService.listIssuingCoupons();
    }
}
