package com.tianji.api.client.remark.fallback;

import com.tianji.api.client.remark.RemarkClient;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;
import java.util.Set;

public class RemarkClientFallBack implements FallbackFactory<RemarkClient> {

    @Override
    public RemarkClient create(Throwable cause) {
        return new RemarkClient() {
            @Override
            public Set<Long> getLikesByBizIds(List<Long> bizIds) {
                return null;
            }
        };
    }
}
