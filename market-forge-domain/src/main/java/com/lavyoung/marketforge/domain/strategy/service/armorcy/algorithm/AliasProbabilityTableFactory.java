package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm;

import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AliasProbabilityTable;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityWeight;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.StrategyProbabilitySpace;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Alias Method 概率表装配器。
 *
 * <p>装配器使用 Vose Alias Method，将包含 n 个奖品的整数权重空间转换为
 * n 个接受概率和 n 个别名下标。装配时间复杂度为 O(n)，空间复杂度为 O(n)。</p>
 *
 * <p>装配过程将列分为“小于平均概率”和“大于等于平均概率”两组。每次使用
 * 一个大概率列补足一个小概率列，直到所有列都被压缩为标准概率列。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@Component
public class AliasProbabilityTableFactory {

    private static final double FULL_PROBABILITY = 1D;

    /**
     * 将整数权重空间装配为可进行常数时间抽样的不可变别名概率表。
     *
     * <p>整个装配过程按照以下步骤执行：</p>
     *
     * <ol>
     *     <li>读取奖品权重集合，以奖品数量 {@code n} 作为别名表的列数。</li>
     *     <li>按照
     *     {@code scaledProbability = awardWeight * n / totalWeight}
     *     计算每个奖品相对于单列平均容量的缩放概率。</li>
     *     <li>把缩放概率小于 {@code 1} 的列放入小概率栈，把大于等于
     *     {@code 1} 的列放入大概率栈。小概率列表示当前列尚未填满，
     *     大概率列表示当前列可以拿出多余概率补充其他列。</li>
     *     <li>每轮分别弹出一个小概率列和一个大概率列。小概率列保留自己的
     *     缩放概率作为接受概率，并把大概率列登记为拒绝后的别名列。</li>
     *     <li>从大概率列中扣除小概率列距离完整概率 {@code 1} 还缺少的部分，
     *     再根据大概率列的剩余缩放概率重新放回小概率栈或大概率栈。</li>
     *     <li>重复配平，直到任意一个栈为空。每轮至少完成一个小概率列，
     *     因此配平循环最多执行 {@code n - 1} 次。</li>
     *     <li>将因概率守恒或浮点误差而剩余的列设置为接受概率 {@code 1}，
     *     并让别名下标指向自身，最后建立不可变概率表。</li>
     * </ol>
     *
     * <p>装配完成后，每一列都由“原奖品接受概率”和“未接受时使用的别名列”
     * 组成。抽样阶段只需等概率选择一列，再执行一次概率判断，就能在 O(1)
     * 时间内得到奖品。</p>
     *
     * <p>该方法不会按照总权重展开概率空间，只创建与奖品数量相同的列和辅助
     * 数组，所以装配时间复杂度和空间复杂度均为 O(n)。浮点运算产生的微小
     * 边界误差会在收尾阶段归一化为完整概率列，不改变原始权重表达的整体
     * 概率分布。</p>
     *
     * @param probabilitySpace 已校验装配键、总权重和奖品权重总和的整数概率空间
     * @return 列数与奖品数量一致、可供 Alias Method 执行 O(1) 抽样的不可变概率表
     * @throws NullPointerException 概率空间为空时抛出
     */
    public AliasProbabilityTable create(StrategyProbabilitySpace probabilitySpace) {
        Objects.requireNonNull(probabilitySpace, "probabilitySpace must not be null");

        List<AwardProbabilityWeight> awardWeights = probabilitySpace.awardWeights();
        int columnCount = awardWeights.size();

        List<Long> awardIds = new ArrayList<>(columnCount);
        double[] scaledProbabilities = new double[columnCount];
        double[] acceptanceProbabilities = new double[columnCount];
        int[] aliasIndexes = new int[columnCount];

        int[] smallColumns = new int[columnCount];
        int[] largeColumns = new int[columnCount];
        int smallColumnCount = 0;
        int largeColumnCount = 0;

        for (int index = 0; index < columnCount; index++) {
            AwardProbabilityWeight awardWeight = awardWeights.get(index);
            awardIds.add(awardWeight.awardId());

            // 默认指向自己，完整概率列不需要跳转到其他奖品。
            aliasIndexes[index] = index;

            // 将原始权重缩放为“相对于单列平均权重”的倍数。
            // 小于 1 表示当前列装不满，大于 1 表示当前列有多余概率可以补给其他列。
            double scaledProbability = awardWeight.weight() * (double) columnCount / probabilitySpace.totalWeight();
            scaledProbabilities[index] = scaledProbability;

            // 使用基本类型数组模拟栈，避免 Integer 装箱产生额外对象。
            if (scaledProbability < FULL_PROBABILITY) {
                smallColumns[smallColumnCount++] = index;
            } else {
                largeColumns[largeColumnCount++] = index;
            }
        }

        while (smallColumnCount > 0 && largeColumnCount > 0) {
            // 分别取出一个未装满的列和一个可以提供剩余概率的列。
            int smallColumn = smallColumns[--smallColumnCount];
            int largeColumn = largeColumns[--largeColumnCount];

            // 小概率列先保留自己的概率，未被接受的剩余部分交给大概率列。
            acceptanceProbabilities[smallColumn] = clampProbability(scaledProbabilities[smallColumn]);
            aliasIndexes[smallColumn] = largeColumn;

            // 大概率列拿出一部分概率补满小概率列，再重新判断自己属于哪一组。
            scaledProbabilities[largeColumn] -= FULL_PROBABILITY - acceptanceProbabilities[smallColumn];

            if (scaledProbabilities[largeColumn] < FULL_PROBABILITY) {
                smallColumns[smallColumnCount++] = largeColumn;
            } else {
                largeColumns[largeColumnCount++] = largeColumn;
            }
        }

        // 理论上剩余列的缩放概率都是 1；浮点误差可能让它略大于 1。
        while (largeColumnCount > 0) {
            int column = largeColumns[--largeColumnCount];
            acceptanceProbabilities[column] = FULL_PROBABILITY;
            aliasIndexes[column] = column;
        }

        // 浮点误差也可能留下略小于 1 的列，同样按完整概率列收尾。
        while (smallColumnCount > 0) {
            int column = smallColumns[--smallColumnCount];
            acceptanceProbabilities[column] = FULL_PROBABILITY;
            aliasIndexes[column] = column;
        }

        return new AliasProbabilityTable(awardIds, acceptanceProbabilities, aliasIndexes);
    }

    /**
     * 消除浮点运算造成的极小边界误差。
     *
     * @param probability 浮点运算后的接受概率
     * @return 被限制在 [0, 1] 范围内的概率
     */
    private double clampProbability(double probability) {
        return Math.max(0D, Math.min(FULL_PROBABILITY, probability));
    }

}
