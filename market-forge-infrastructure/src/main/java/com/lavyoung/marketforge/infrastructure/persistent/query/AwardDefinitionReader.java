package com.lavyoung.marketforge.infrastructure.persistent.query;

import com.lavyoung.marketforge.application.award.model.AwardDefinitionResult;
import com.lavyoung.marketforge.application.award.model.AwardDefinitionSearchQuery;
import com.lavyoung.marketforge.application.award.query.IAwardDefinitionReader;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.AwardDefinitionReadModelAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IAwardDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.AwardDefinitionSearchParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.AwardPO;
import com.lavyoung.marketforge.types.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * 基于奖品定义主表的运营查询读取适配器。
 *
 * <p>奖品定义是低数据量、强一致的配置主数据，运营人员保存或启停奖品后需要立即看到
 * 最新结果，因此直接读取现有 {@code award} 表。该类不会创建新数据库或新数据源，
 * 只复用项目已有的 {@link IAwardDao}。</p>
 *
 * <p>该适配器只负责查询参数转换、分页读取和读模型装配。奖品状态流转等业务规则仍由
 * Domain 层处理，写入仍通过领域 Repository 完成。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Repository
@RequiredArgsConstructor
public class AwardDefinitionReader implements IAwardDefinitionReader {

    private final IAwardDao awardDao;
    private final AwardDefinitionReadModelAssembler readModelAssembler;

    @Override
    public PageResult<AwardDefinitionResult> search(AwardDefinitionSearchQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        AwardDefinitionSearchParam searchParam = toSearchParam(query);
        long total = awardDao.countAwardDefinitions(searchParam);
        if (total == 0) {
            return PageResult.empty(query.page().pageNumber(), query.page().pageSize());
        }

        List<AwardPO> awardDefinitions = Objects.requireNonNull(
                awardDao.queryAwardDefinitionPage(searchParam),
                "award definition query result must not be null"
        );
        List<AwardDefinitionResult> records = readModelAssembler.toResults(awardDefinitions);
        return new PageResult<>(records, total, query.page().pageNumber(), query.page().pageSize());
    }

    /**
     * 将应用查询条件转换为 MyBatis 查询参数。
     *
     * <p>分页偏移量由共享 {@code PageRequest} 使用 {@code long} 计算，避免页码乘法产生
     * 整数溢出；过滤条件已经由应用查询对象完成空白归一化。</p>
     *
     * @param query 已校验并归一化的奖品定义查询条件
     * @return 可直接交给 MyBatis Mapper 的查询参数
     */
    private AwardDefinitionSearchParam toSearchParam(AwardDefinitionSearchQuery query) {
        return new AwardDefinitionSearchParam(query.awardCode(), query.awardName(), query.awardType(), query.state(), query.page().offset(), query.page().pageSize());
    }
}
