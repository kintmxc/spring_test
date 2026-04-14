package com.example.spring_test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.spring_test.dto.EvaluationSaveDTO;
import com.example.spring_test.entity.Evaluation;
import com.example.spring_test.exception.BusinessException;
import com.example.spring_test.mapper.EvaluationMapper;
import com.example.spring_test.security.CurrentUserUtil;
import com.example.spring_test.security.SessionUser;
import com.example.spring_test.service.EvaluationService;
import com.example.spring_test.util.UrlUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class EvaluationServiceImpl implements EvaluationService {
    private final EvaluationMapper evaluationMapper;
    private final ObjectMapper objectMapper;

    public EvaluationServiceImpl(EvaluationMapper evaluationMapper, ObjectMapper objectMapper) {
        this.evaluationMapper = evaluationMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> create(EvaluationSaveDTO evaluationSaveDTO) {
        if (evaluationSaveDTO.getProductId() == null) {
            throw new BusinessException("商品ID不能为空");
        }
        if (evaluationSaveDTO.getScore() == null || evaluationSaveDTO.getScore() < 1 || evaluationSaveDTO.getScore() > 5) {
            throw new BusinessException("评分范围应为1-5");
        }
        SessionUser user = CurrentUserUtil.getRequiredUser();
        Evaluation evaluation = new Evaluation();
        evaluation.setOrderId(evaluationSaveDTO.getOrderId());
        evaluation.setProductId(evaluationSaveDTO.getProductId());
        evaluation.setUserId(user.getUserId());
        evaluation.setNickName(maskName(user.getUsername()));
        evaluation.setAvatar("");
        evaluation.setScore(evaluationSaveDTO.getScore());
        evaluation.setContent(evaluationSaveDTO.getContent());
        evaluation.setImagesJson(toJson(evaluationSaveDTO.getImages()));
        evaluation.setTagsJson(toJson(evaluationSaveDTO.getTags()));
        evaluation.setCreateTime(LocalDateTime.now());
        evaluationMapper.insert(evaluation);
        return toView(evaluation);
    }

    @Override
    public Map<String, Object> list(Long productId) {
        List<Evaluation> list = evaluationMapper.selectList(new LambdaQueryWrapper<Evaluation>()
                .eq(productId != null, Evaluation::getProductId, productId)
                .orderByDesc(Evaluation::getCreateTime)
                .orderByDesc(Evaluation::getId));
        List<Map<String, Object>> views = list.stream().map(this::toView).toList();
        return Map.of("list", views, "total", views.size());
    }

    public Map<String, Object> list(Long productId, Integer page, Integer pageSize) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1 || pageSize > 50) {
            pageSize = 10;
        }
        
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Evaluation> mpPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Evaluation> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Evaluation>()
                .eq(productId != null, Evaluation::getProductId, productId)
                .orderByDesc(Evaluation::getCreateTime)
                .orderByDesc(Evaluation::getId);
        
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Evaluation> result = evaluationMapper.selectPage(mpPage, queryWrapper);
        
        List<Map<String, Object>> views = result.getRecords().stream().map(this::toView).toList();
        
        return Map.of(
                "list", views,
                "total", result.getTotal(),
                "page", page,
                "pageSize", pageSize,
                "hasMore", result.hasNext()
        );
    }

    private String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "用户";
        }
        if (name.length() <= 2) {
            return name.charAt(0) + "*";
        }
        return name.substring(0, 1) + "**" + name.substring(name.length() - 1);
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list == null ? Collections.emptyList() : list);
        } catch (Exception e) {
            throw new BusinessException("数据格式错误");
        }
    }

    private List<String> parseList(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> toView(Evaluation evaluation) {
        List<String> images = parseList(evaluation.getImagesJson()).stream()
                .map(UrlUtils::toFullUrl)
                .collect(java.util.stream.Collectors.toList());
        List<String> tags = parseList(evaluation.getTagsJson());
        return Map.of(
                "id", evaluation.getId(),
                "orderId", evaluation.getOrderId() == null ? 0L : evaluation.getOrderId(),
                "productId", evaluation.getProductId() == null ? 0L : evaluation.getProductId(),
                "nickName", evaluation.getNickName() == null ? "用户" : evaluation.getNickName(),
                "avatar", evaluation.getAvatar() == null ? "" : evaluation.getAvatar(),
                "content", evaluation.getContent() == null ? "" : evaluation.getContent(),
                "score", evaluation.getScore() == null ? 5 : evaluation.getScore(),
                "images", images,
                "tags", tags,
                "createTime", evaluation.getCreateTime() == null ? "" : evaluation.getCreateTime().toString()
        );
    }
}
