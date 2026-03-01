package com.eazyai.ai.nexus.core.planner;

import com.eazyai.ai.nexus.api.dto.AgentContext;
import com.eazyai.ai.nexus.api.intent.IntentResult;
import com.eazyai.ai.nexus.api.planner.TaskPlan;
import com.eazyai.ai.nexus.api.planner.TaskPlanner;
import com.eazyai.ai.nexus.core.planner.strategy.LlmDrivenRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 混合模式任务规划器
 * 
 * <p>说明：</p>
 * 简单任务已被 LangChain4j AiServices 自动处理，
 * 此规划器仅用于需要显式编排插件执行顺序的高级场景。
 * 
 * <p>使用场景：</p>
 * <ul>
 *   <li>多插件协作编排</li>
 *   <li>需要 Pipeline 模式传递中间结果</li>
 *   <li>需要精确控制执行顺序和重试策略</li>
 * </ul>
 */
@Slf4j
@Component
public class HybridPlanner implements TaskPlanner {

    private final LlmDrivenRouter router;

    @Autowired
    public HybridPlanner(LlmDrivenRouter router) {
        this.router = router;
        log.info("HybridPlanner 初始化完成（高级模式）");
    }

    @Override
    public TaskPlan createPlan(IntentResult intent, AgentContext context) {
        log.debug("高级模式规划任务: {}", context.getUserInput());
        return router.createPlan(intent, context);
    }

    @Override
    public boolean supports(String intentType) {
        // 仅支持需要编排的复杂意图
        return "orchestration".equals(intentType) || "multi-step".equals(intentType);
    }

    @Override
    public String getName() {
        return "HybridPlanner";
    }
}
