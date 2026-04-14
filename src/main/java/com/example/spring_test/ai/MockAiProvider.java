package com.example.spring_test.ai;

import org.springframework.stereotype.Component;

@Component
public class MockAiProvider implements AiProvider {
    @Override
    public String getName() {
        return "mock";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String chat(String prompt) {
        return "这是一个模拟的AI回复。请配置真实的AI服务以获得更好的体验。";
    }

    @Override
    public String chatWithSystem(String systemPrompt, String userPrompt) {
        if (userPrompt.contains("商品描述") || userPrompt.contains("描述")) {
            return "这是一款优质的农产品，产自绿色无污染的农场，新鲜采摘，品质优良。富含多种营养成分，是您健康饮食的理想选择。";
        }
        if (userPrompt.contains("食用") || userPrompt.contains("吃")) {
            return "建议清洗干净后直接食用或烹饪。可生食、凉拌、炒制等多种方式均可。注意适量食用，避免过量。";
        }
        if (userPrompt.contains("营养")) {
            return "该产品富含蛋白质、维生素、膳食纤维等多种营养成分。适量食用有助于补充人体所需营养，促进健康。";
        }
        if (userPrompt.contains("存储") || userPrompt.contains("保存")) {
            return "建议存放在阴凉干燥处，避免阳光直射。如需长期保存，建议冷藏保存，温度控制在0-4℃。";
        }
        return "这是一个模拟的AI回复，请配置真实的AI服务以获得更好的体验。";
    }
}
