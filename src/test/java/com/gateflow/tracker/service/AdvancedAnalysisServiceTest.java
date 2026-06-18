package com.gateflow.tracker.service;

import com.gateflow.tracker.domain.dto.advanced.FunnelDto;
import com.gateflow.tracker.domain.dto.advanced.RetentionDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AdvancedAnalysisServiceTest {

    // ── 漏斗:level 分布 → 各步到达人数 ──
    @Test
    void usersReachedFromLevels() {
        long[] r = AdvancedAnalysisService.usersReachedFromLevels(Map.of(1, 10L, 2, 5L, 3, 2L), 3);
        assertThat(r).containsExactly(17L, 7L, 2L); // >=1, >=2, >=3
    }

    @Test
    void usersReachedHandlesGaps() {
        long[] r = AdvancedAnalysisService.usersReachedFromLevels(Map.of(0, 8L, 2, 4L), 3);
        // level0 不计入任何步;reached[0]=>=1=4, reached[1]=>=2=4, reached[2]=>=3=0
        assertThat(r).containsExactly(4L, 4L, 0L);
    }

    @Test
    void buildFunnelStepsComputesConversionRates() {
        List<FunnelDto.StepDef> defs = List.of(
                new FunnelDto.StepDef("访问", "page_view", null),
                new FunnelDto.StepDef("加购", "add_cart", null),
                new FunnelDto.StepDef("下单", "purchase", null));
        long[] reached = {100, 40, 10};
        List<FunnelDto.Step> steps = AdvancedAnalysisService.buildFunnelSteps(defs, reached,
                Map.of("page_view", 500L, "add_cart", 60L, "purchase", 12L));

        assertThat(steps.get(0).getConversionRate()).isEqualTo(1.0);
        assertThat(steps.get(0).getStepConversionRate()).isEqualTo(1.0);
        assertThat(steps.get(1).getConversionRate()).isCloseTo(0.4, within(1e-9));
        assertThat(steps.get(1).getStepConversionRate()).isCloseTo(0.4, within(1e-9));
        assertThat(steps.get(2).getConversionRate()).isCloseTo(0.1, within(1e-9));
        assertThat(steps.get(2).getStepConversionRate()).isCloseTo(0.25, within(1e-9));
        assertThat(steps.get(0).getCount()).isEqualTo(500); // 事件量口径
    }

    // ── 过滤/时间归一/防注入 ──
    @Test
    void baseFilterBuildsTimeAndOptionalDimensions() {
        String f = AdvancedAnalysisService.baseFilter("2026-06-01", "2026-06-30", "web", "A_MAIN");
        assertThat(f).contains("timestamp >= '2026-06-01'")
                .contains("timestamp <= '2026-06-30'")
                .contains("platform = 'web'")
                .contains("app_code = 'A_MAIN'");
    }

    @Test
    void baseFilterOmitsBlankDimensions() {
        String f = AdvancedAnalysisService.baseFilter("2026-06-01", "2026-06-30", null, "  ");
        assertThat(f).doesNotContain("platform").doesNotContain("app_code");
    }

    @Test
    void toChTimeNormalizesIso() {
        assertThat(AdvancedAnalysisService.toChTime("2026-06-15T07:00:00.123Z")).isEqualTo("2026-06-15 07:00:00");
        assertThat(AdvancedAnalysisService.toChTime("2026-06-15")).isEqualTo("2026-06-15");
    }

    @Test
    void sanStripsInjectionChars() {
        assertThat(AdvancedAnalysisService.san("a' OR '1'='1")).isEqualTo("a OR 1=1");
        assertThat(AdvancedAnalysisService.baseFilter("x','y", "e", "p'--", null)).doesNotContain("'p'--'");
    }

    // ── 留存曲线与汇总 ──
    @Test
    void retentionCurveAndSummary() {
        RetentionDto.Cohort c1 = new RetentionDto.Cohort("2026-06-01", 100,
                Map.of(), Map.of("d1", 50L, "d7", 20L, "d30", 5L));
        RetentionDto.Cohort c2 = new RetentionDto.Cohort("2026-06-02", 100,
                Map.of(), Map.of("d1", 30L, "d7", 10L, "d30", 5L));
        List<Integer> days = List.of(1, 7, 30);

        var curve = AdvancedAnalysisService.retentionCurve(List.of(c1, c2), days);
        assertThat(curve).hasSize(3);
        assertThat(curve.get(0).getRate()).isCloseTo(0.4, within(1e-9)); // (50+30)/200
        assertThat(curve.get(1).getRate()).isCloseTo(0.15, within(1e-9)); // (20+10)/200

        var summary = AdvancedAnalysisService.retentionSummary(List.of(c1, c2), days);
        assertThat(summary.getTotalInitialUsers()).isEqualTo(200);
        assertThat(summary.getDay1Rate()).isCloseTo(0.4, within(1e-9));
        assertThat(summary.getDay30Rate()).isCloseTo(0.05, within(1e-9)); // 10/200
    }
}
