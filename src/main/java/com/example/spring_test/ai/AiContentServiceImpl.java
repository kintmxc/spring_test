package com.example.spring_test.ai;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AiContentServiceImpl implements AiContentService {
    private final AiProviderFactory providerFactory;

    public AiContentServiceImpl(AiProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    @Override
    public String generateProductDescription(String productName, String category, String origin) {
        if (!providerFactory.isAiEnabled()) {
            return generateFallbackDescription(productName, category, origin);
        }

        String prompt = buildProductDescriptionPrompt(productName, category, origin);
        return providerFactory.getProvider().chat(prompt);
    }

    @Override
    public String generateEatAdvice(String productName) {
        if (!providerFactory.isAiEnabled()) {
            return generateFallbackEatAdvice(productName);
        }

        String prompt = "请为\"" + productName + "\"提供详细的食用建议，包括：\n" +
                "1. 最佳食用方式（生食、烹饪方法等）\n" +
                "2. 搭配建议\n" +
                "3. 注意事项\n" +
                "4. 适宜人群\n" +
                "请用简洁的语言回答，控制在100字以内。";
        return providerFactory.getProvider().chat(prompt);
    }

    @Override
    public String generateNutritionAnalysis(String productName) {
        if (!providerFactory.isAiEnabled()) {
            return generateFallbackNutrition(productName);
        }

        String prompt = "请为\"" + productName + "\"提供营养分析，包括：\n" +
                "1. 主要营养成分\n" +
                "2. 营养价值\n" +
                "3. 健康益处\n" +
                "请用简洁的语言回答，控制在100字以内。";
        return providerFactory.getProvider().chat(prompt);
    }

    @Override
    public String generateStoreAdvice(String productName) {
        if (!providerFactory.isAiEnabled()) {
            return generateFallbackStoreAdvice(productName);
        }

        String prompt = "请为\"" + productName + "\"提供存储建议，包括：\n" +
                "1. 最佳存储温度\n" +
                "2. 存储方式\n" +
                "3. 保质期建议\n" +
                "4. 注意事项\n" +
                "请用简洁的语言回答，控制在100字以内。";
        return providerFactory.getProvider().chat(prompt);
    }

    @Override
    public Map<String, Object> generateAllContent(String productName, String category, String origin) {
        Map<String, Object> result = new HashMap<>();
        result.put("description", generateProductDescription(productName, category, origin));
        result.put("eatAdvice", generateEatAdvice(productName));
        result.put("nutritionAnalysis", generateNutritionAnalysis(productName));
        result.put("storeAdvice", generateStoreAdvice(productName));
        result.put("aiGenerated", providerFactory.isAiEnabled());
        result.put("provider", providerFactory.getProvider().getName());
        return result;
    }

    @Override
    public String generateChatResponse(String content, java.util.List<java.util.Map<String, String>> history, String userType) {
        if (!providerFactory.isAiEnabled()) {
            return generateFallbackChatResponse(content);
        }

        String prompt = buildChatPrompt(content, history, userType);
        return providerFactory.getProvider().chat(prompt);
    }

    private String buildChatPrompt(String content, java.util.List<java.util.Map<String, String>> history, String userType) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个专业的农业产品客服助手，根据以下聊天历史和最新消息，生成专业、友好的回复：\n\n");
        
        if (history != null && !history.isEmpty()) {
            for (java.util.Map<String, String> msg : history) {
                String msgContent = msg.get("content");
                sb.append("用户: ").append(msgContent).append("\n");
            }
        }
        
        sb.append("用户: ").append(content).append("\n\n");
        sb.append("请生成专业的回复，针对农业产品相关问题提供准确信息，语气友好自然。");
        return sb.toString();
    }

    private String generateFallbackChatResponse(String content) {
        return "感谢您的咨询。我们的AI服务暂时不可用，请稍后再试。如果您有关于农产品的问题，欢迎随时联系我们的客服团队。";
    }

    private String buildProductDescriptionPrompt(String productName, String category, String origin) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为以下农产品生成一段吸引人的商品描述：\n");
        sb.append("商品名称：").append(productName).append("\n");
        if (category != null && !category.isEmpty()) {
            sb.append("分类：").append(category).append("\n");
        }
        if (origin != null && !origin.isEmpty()) {
            sb.append("产地：").append(origin).append("\n");
        }
        sb.append("\n要求：\n");
        sb.append("1. 突出产品特色和优势\n");
        sb.append("2. 语言生动有吸引力\n");
        sb.append("3. 控制在150字以内\n");
        sb.append("4. 不要使用夸张或虚假宣传");
        return sb.toString();
    }

    private String generateFallbackDescription(String productName, String category, String origin) {
        StringBuilder sb = new StringBuilder();
        sb.append(productName);
        if (origin != null && !origin.isEmpty()) {
            sb.append("，产自").append(origin);
        }
        sb.append("，品质优良，新鲜直达。");
        if (category != null) {
            sb.append("精选").append(category).append("类产品");
        }
        sb.append("，绿色健康，值得信赖。");
        return sb.toString();
    }

    private String generateFallbackEatAdvice(String productName) {
        return productName + "建议清洗干净后食用。可根据个人喜好选择生食或烹饪，注意适量食用。";
    }

    private String generateFallbackNutrition(String productName) {
        return productName + "富含多种营养成分，适量食用有益健康。具体营养成分请参考产品包装说明。";
    }

    private String generateFallbackStoreAdvice(String productName) {
        return productName + "建议存放在阴凉干燥处，避免阳光直射。如需长期保存，建议冷藏保存。";
    }

    @Override
    public String getProviderName() {
        return providerFactory.getProvider().getName();
    }

    @Override
    public boolean isAvailable() {
        return providerFactory.isAiEnabled();
    }
}
