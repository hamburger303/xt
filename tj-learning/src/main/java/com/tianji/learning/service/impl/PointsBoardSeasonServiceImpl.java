package com.tianji.learning.service.impl;

import com.tianji.learning.Contstants.LearningContstant;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.mapper.PointsBoardSeasonMapper;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 冯立翔
 * @since 2025-07-16
 */
@Service
public class PointsBoardSeasonServiceImpl extends ServiceImpl<PointsBoardSeasonMapper, PointsBoardSeason> implements IPointsBoardSeasonService {

    @Override
    public List<PointsBoardSeason> queryPointsBoard() {
        return this.list();
    }

    @Override
    public void createPointsBoardLatesTable(Integer id) {
        getBaseMapper().createPointsBoardLatesTable(LearningContstant.POINTS_BOARD_TABLE_PREFIX+id);
    }
}
