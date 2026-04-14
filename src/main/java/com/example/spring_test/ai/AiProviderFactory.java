package com.example.spring_test.ai;

import com.example.spring_test.config.AiConfig;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AiProviderFactory {
    private final Map<String, AiProvider> providers;
    private final AiConfig config;

    public AiProviderFactory(List<AiProvider> providerList, AiConfig config) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(AiProvider::getName, Function.identity()));
        this.config = config;
    }

    public AiProvider getProvider() {
        String providerName = config.getProvider();
        AiProvider provider = providers.get(providerName);
        
        if (provider == null) {
            provider = providers.get("mock");
        }
        
        if (provider != null && !provider.isAvailable()) {
            provider = providers.get("mock");
        }
        
        return provider;
    }

    public boolean isAiEnabled() {
        return config.isEnabled() && getProvider().isAvailable();
    }
}
