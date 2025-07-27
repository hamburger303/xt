package com.tianji.api.client.remark;

import com.tianji.api.client.remark.fallback.RemarkClientFallBack;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@FeignClient(name = "remark-service",fallbackFactory = RemarkClientFallBack.class)
public interface RemarkClient {
    @GetMapping("/likes/list")
    public Set<Long> getLikesByBizIds(@RequestParam("bizIds") List<Long> bizIds);
}
