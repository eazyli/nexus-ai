package com.eazyai.ai.nexus.application.scene;

import com.eazyai.ai.nexus.api.application.SceneDescriptor;
import com.eazyai.ai.nexus.infra.dal.entity.AiScene;
import com.eazyai.ai.nexus.infra.dal.repository.AiSceneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 场景配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SceneService {

    private final AiSceneRepository aiSceneRepository;

    /**
     * 注册场景
     */
    public SceneDescriptor registerScene(SceneDescriptor descriptor) {
        AiScene scene = toEntity(descriptor);
        scene.setCreateTime(LocalDateTime.now());
        scene.setUpdateTime(LocalDateTime.now());
        scene.setStatus(1); // 默认启用
        
        aiSceneRepository.insert(scene);
        log.info("注册场景: {} - {} (类型: {})", 
                scene.getSceneId(), scene.getSceneName(), scene.getSceneType());
        
        return toDescriptor(scene);
    }

    /**
     * 获取场景
     */
    public Optional<SceneDescriptor> getScene(String sceneId) {
        return aiSceneRepository.findById(sceneId)
                .map(this::toDescriptor);
    }

    /**
     * 获取应用下的所有场景
     */
    public List<SceneDescriptor> getScenesByApp(String appId) {
        return aiSceneRepository.findEnabledByAppId(appId).stream()
                .map(this::toDescriptor)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有场景
     */
    public List<SceneDescriptor> getAllScenes() {
        return aiSceneRepository.findAllEnabled().stream()
                .map(this::toDescriptor)
                .collect(Collectors.toList());
    }

    /**
     * 更新场景
     */
    public SceneDescriptor updateScene(String sceneId, SceneDescriptor descriptor) {
        AiScene existing = aiSceneRepository.findById(sceneId)
                .orElseThrow(() -> new IllegalArgumentException("场景不存在: " + sceneId));
        
        AiScene scene = toEntity(descriptor);
        scene.setSceneId(sceneId);
        scene.setCreateTime(existing.getCreateTime());
        scene.setUpdateTime(LocalDateTime.now());
        scene.setStatus(existing.getStatus());
        
        aiSceneRepository.updateById(scene);
        log.info("更新场景: {}", sceneId);
        
        return toDescriptor(scene);
    }

    /**
     * 删除场景
     */
    public void deleteScene(String sceneId) {
        aiSceneRepository.deleteById(sceneId);
        log.info("删除场景: {}", sceneId);
    }

    /**
     * 根据类型查找场景
     */
    public List<SceneDescriptor> getScenesByType(String type) {
        return aiSceneRepository.findBySceneType(type).stream()
                .filter(s -> s.getStatus() != null && s.getStatus() == 1)
                .map(this::toDescriptor)
                .collect(Collectors.toList());
    }

    /**
     * 获取场景配置
     */
    @SuppressWarnings("unchecked")
    public Optional<SceneDescriptor.SceneConfig> getSceneConfig(String sceneId) {
        return getScene(sceneId).map(SceneDescriptor::getConfig);
    }

    /**
     * 匹配最佳场景
     */
    public Optional<SceneDescriptor> matchScene(String appId, String input) {
        return aiSceneRepository.findEnabledByAppId(appId).stream()
                .map(this::toDescriptor)
                .max(Comparator.comparing(SceneDescriptor::getPriority, 
                        Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /**
     * 实体转描述符
     */
    @SuppressWarnings("unchecked")
    private SceneDescriptor toDescriptor(AiScene scene) {
        if (scene == null) return null;
        
        SceneDescriptor descriptor = new SceneDescriptor();
        descriptor.setSceneId(scene.getSceneId());
        descriptor.setName(scene.getSceneName());
        descriptor.setDescription(scene.getDescription());
        descriptor.setAppId(scene.getAppId());
        descriptor.setType(scene.getSceneType());
        descriptor.setEnabled(scene.getStatus() != null && scene.getStatus() == 1);
        descriptor.setPriority(scene.getPriority());
        
        // 解析能力ID列表
        if (scene.getAbilityIds() != null && !scene.getAbilityIds().isEmpty()) {
            descriptor.setCapabilityIds(Arrays.asList(scene.getAbilityIds().split(",")));
        }
        // 解析工具ID列表
        if (scene.getToolIds() != null && !scene.getToolIds().isEmpty()) {
            descriptor.setToolIds(Arrays.asList(scene.getToolIds().split(",")));
        }
        // 解析知识库ID列表
        if (scene.getKnowledgeIds() != null && !scene.getKnowledgeIds().isEmpty()) {
            descriptor.setKnowledgeBaseIds(Arrays.asList(scene.getKnowledgeIds().split(",")));
        }
        
        // 解析配置
        if (scene.getConfig() != null) {
            Map<String, Object> configMap = scene.getConfig();
            SceneDescriptor.SceneConfig config = SceneDescriptor.SceneConfig.builder()
                    .promptTemplate((String) configMap.get("promptTemplate"))
                    .extra((Map<String, Object>) configMap.get("extra"))
                    .build();
            
            // 解析NLU配置
            if (configMap.get("nlu") != null) {
                Map<String, Object> nluMap = (Map<String, Object>) configMap.get("nlu");
                SceneDescriptor.NluConfig nlu = SceneDescriptor.NluConfig.builder()
                        .enabled((Boolean) nluMap.get("enabled"))
                        .intentTypes((List<String>) nluMap.get("intentTypes"))
                        .entityTypes((List<String>) nluMap.get("entityTypes"))
                        .build();
                config.setNlu(nlu);
            }
            
            // 解析RAG配置
            if (configMap.get("rag") != null) {
                Map<String, Object> ragMap = (Map<String, Object>) configMap.get("rag");
                SceneDescriptor.RagConfig rag = SceneDescriptor.RagConfig.builder()
                        .enabled((Boolean) ragMap.get("enabled"))
                        .topK((Integer) ragMap.get("topK"))
                        .scoreThreshold((Double) ragMap.get("scoreThreshold"))
                        .rerankModel((String) ragMap.get("rerankModel"))
                        .build();
                config.setRag(rag);
            }
            
            // 解析Agent配置
            if (configMap.get("agent") != null) {
                Map<String, Object> agentMap = (Map<String, Object>) configMap.get("agent");
                SceneDescriptor.AgentConfig agent = SceneDescriptor.AgentConfig.builder()
                        .mode((String) agentMap.get("mode"))
                        .maxIterations((Integer) agentMap.get("maxIterations"))
                        .maxSteps((Integer) agentMap.get("maxSteps"))
                        .build();
                config.setAgent(agent);
            }
            
            // 解析输出配置
            if (configMap.get("output") != null) {
                Map<String, Object> outputMap = (Map<String, Object>) configMap.get("output");
                SceneDescriptor.OutputConfig output = SceneDescriptor.OutputConfig.builder()
                        .format((String) outputMap.get("format"))
                        .jsonSchema((String) outputMap.get("jsonSchema"))
                        .streaming((Boolean) outputMap.get("streaming"))
                        .build();
                config.setOutput(output);
            }
            
            descriptor.setConfig(config);
        }
        
        return descriptor;
    }

    /**
     * 描述符转实体
     */
    private AiScene toEntity(SceneDescriptor descriptor) {
        AiScene scene = new AiScene();
        scene.setSceneId(descriptor.getSceneId() != null ? descriptor.getSceneId() : UUID.randomUUID().toString());
        scene.setSceneName(descriptor.getName());
        scene.setDescription(descriptor.getDescription());
        scene.setAppId(descriptor.getAppId());
        scene.setSceneType(descriptor.getType());
        scene.setStatus(descriptor.getEnabled() != null && descriptor.getEnabled() ? 1 : 0);
        scene.setPriority(descriptor.getPriority());
        
        if (descriptor.getCapabilityIds() != null && !descriptor.getCapabilityIds().isEmpty()) {
            scene.setAbilityIds(String.join(",", descriptor.getCapabilityIds()));
        }
        if (descriptor.getToolIds() != null && !descriptor.getToolIds().isEmpty()) {
            scene.setToolIds(String.join(",", descriptor.getToolIds()));
        }
        if (descriptor.getKnowledgeBaseIds() != null && !descriptor.getKnowledgeBaseIds().isEmpty()) {
            scene.setKnowledgeIds(String.join(",", descriptor.getKnowledgeBaseIds()));
        }
        
        // 转换配置
        if (descriptor.getConfig() != null) {
            Map<String, Object> configMap = new HashMap<>();
            SceneDescriptor.SceneConfig config = descriptor.getConfig();
            
            configMap.put("promptTemplate", config.getPromptTemplate());
            configMap.put("extra", config.getExtra());
            
            if (config.getNlu() != null) {
                Map<String, Object> nluMap = new HashMap<>();
                nluMap.put("enabled", config.getNlu().getEnabled());
                nluMap.put("intentTypes", config.getNlu().getIntentTypes());
                nluMap.put("entityTypes", config.getNlu().getEntityTypes());
                configMap.put("nlu", nluMap);
            }
            
            if (config.getRag() != null) {
                Map<String, Object> ragMap = new HashMap<>();
                ragMap.put("enabled", config.getRag().getEnabled());
                ragMap.put("topK", config.getRag().getTopK());
                ragMap.put("scoreThreshold", config.getRag().getScoreThreshold());
                ragMap.put("rerankModel", config.getRag().getRerankModel());
                configMap.put("rag", ragMap);
            }
            
            if (config.getAgent() != null) {
                Map<String, Object> agentMap = new HashMap<>();
                agentMap.put("mode", config.getAgent().getMode());
                agentMap.put("maxIterations", config.getAgent().getMaxIterations());
                agentMap.put("maxSteps", config.getAgent().getMaxSteps());
                configMap.put("agent", agentMap);
            }
            
            if (config.getOutput() != null) {
                Map<String, Object> outputMap = new HashMap<>();
                outputMap.put("format", config.getOutput().getFormat());
                outputMap.put("jsonSchema", config.getOutput().getJsonSchema());
                outputMap.put("streaming", config.getOutput().getStreaming());
                configMap.put("output", outputMap);
            }
            
            scene.setConfig(configMap);
        }
        
        return scene;
    }
}
