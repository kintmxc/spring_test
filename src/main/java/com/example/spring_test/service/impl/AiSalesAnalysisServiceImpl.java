package com.example.spring_test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.spring_test.entity.OrderItem;
import com.example.spring_test.entity.Orders;
import com.example.spring_test.entity.Product;
import com.example.spring_test.mapper.OrderItemMapper;
import com.example.spring_test.mapper.OrdersMapper;
import com.example.spring_test.mapper.ProductMapper;
import com.example.spring_test.service.AiSalesAnalysisService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AiSalesAnalysisServiceImpl implements AiSalesAnalysisService {

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;

    public AiSalesAnalysisServiceImpl(OrdersMapper ordersMapper,
                                      OrderItemMapper orderItemMapper,
                                      ProductMapper productMapper) {
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
    }

    @Override
    public Map<String, Object> analyze(Long farmerId, String dateType) {
        LocalDateTime start = calculateStartTime(dateType);
        List<Orders> orders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getFarmerId, farmerId)
                .ge(Orders::getCreateTime, start)
                .in(Orders::getOrderStatus, 1, 2, 3));

        List<OrderItem> orderItems = Collections.emptyList();
        if (!orders.isEmpty()) {
            Set<Long> orderIds = orders.stream().map(Orders::getId).collect(Collectors.toSet());
            orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .in(OrderItem::getOrderId, orderIds));
        }

        List<Product> farmerProducts = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getFarmerId, farmerId));

        Map<Long, Product> productMap = farmerProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        Map<Long, Integer> soldCountMap = new HashMap<>();
        for (OrderItem item : orderItems) {
            soldCountMap.merge(item.getProductId(), item.getQuantity() == null ? 0 : item.getQuantity(), Integer::sum);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("stagnantProduct", buildStagnantProductSection(productMap, soldCountMap));
        result.put("bestTime", buildBestTimeSection(orders, dateType));
        result.put("seasonalProduct", buildSeasonalSection());
        return result;
    }

    private Map<String, Object> buildStagnantProductSection(Map<Long, Product> productMap, Map<Long, Integer> soldCountMap) {
        Product bestProduct = null;
        Product stagnantProduct = null;
        int bestCount = -1;
        int stagnantCount = Integer.MAX_VALUE;

        for (Product product : productMap.values()) {
            int sold = soldCountMap.getOrDefault(product.getId(), 0);
            if (sold > bestCount) {
                bestCount = sold;
                bestProduct = product;
            }
            if (sold < stagnantCount) {
                stagnantCount = sold;
                stagnantProduct = product;
            }
        }

        String stagnantName = stagnantProduct == null ? "暂无商品" : stagnantProduct.getProductName();
        String bestName = bestProduct == null ? "暂无高销量商品" : bestProduct.getProductName();
        String priceText = stagnantProduct == null || stagnantProduct.getPrice() == null
                ? "价格待完善"
                : stagnantProduct.getPrice().stripTrailingZeros().toPlainString() + "元";

        String analysis = "发现\"" + stagnantName + "\"销量仅" + Math.max(stagnantCount, 0)
                + "件，与" + bestName + "形成明显落差";
        String suggestions = "您的【" + stagnantName + "】销量较低。可能原因：\n"
                + "1. 价格" + priceText + "是否合理？\n"
                + "2. 商品描述是否突出'新鲜/产地直发/品质保障'等卖点？\n"
                + "3. 可尝试与" + bestName + "做组合优惠，带动转化";

        return Map.of("analysis", analysis, "suggestions", suggestions);
    }

    private Map<String, Object> buildBestTimeSection(List<Orders> orders, String dateType) {
        if (orders.isEmpty()) {
            return Map.of(
                    "analysis", "当前统计周期内暂无成交订单，暂无法判断最佳销售时段",
                    "suggestions", "建议：\n1. 先做小规模促销获取基础数据\n2. 固定每周上新节奏，观察访问和成交变化\n3. 对比不同日期活动效果，逐步确定最佳周期"
            );
        }

        Map<LocalDate, Long> dayOrderCount = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getCreateTime().toLocalDate(), Collectors.counting()));

        List<Map.Entry<LocalDate, Long>> sorted = new ArrayList<>(dayOrderCount.entrySet());
        sorted.sort(Map.Entry.<LocalDate, Long>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()));

        Map.Entry<LocalDate, Long> bestDay = sorted.get(0);
        String dayText = formatDayLabel(bestDay.getKey(), dateType);

        String analysis = "从\"近" + toRangeLabel(dateType) + "销售趋势\"发现：销售集中在" + dayText;
        String suggestions = "您的销售集中在特定日期（" + dayText + "）。建议：\n"
                + "1. 在这些日子加大促销力度或发布新品\n"
                + "2. 尝试每周固定一天做'限时秒杀'，培养用户习惯\n"
                + "3. 记录每次促销后的数据，找到最佳销售周期";

        return Map.of("analysis", analysis, "suggestions", suggestions);
    }

    private Map<String, Object> buildSeasonalSection() {
        int month = LocalDate.now().getMonthValue();
        String season;
        String recommendation;

        if (month >= 3 && month <= 5) {
            season = "春末夏初";
            recommendation = "当季蔬菜（如：春笋、蚕豆、蒜薹）\n"
                    + "夏季水果（如：樱桃、枇杷、杨梅）\n"
                    + "季节性土特产（如：端午粽子礼盒）";
        } else if (month >= 6 && month <= 8) {
            season = "盛夏";
            recommendation = "清爽蔬菜（如：黄瓜、丝瓜、苦瓜）\n"
                    + "夏季水果（如：西瓜、葡萄、桃子）\n"
                    + "消暑食材（如：绿豆、莲子）";
        } else if (month >= 9 && month <= 11) {
            season = "秋季";
            recommendation = "应季蔬菜（如：南瓜、萝卜、山药）\n"
                    + "秋季水果（如：苹果、柿子、梨）\n"
                    + "滋补食材（如：银耳、百合）";
        } else {
            season = "冬季";
            recommendation = "耐储蔬菜（如：白菜、土豆、胡萝卜）\n"
                    + "冬令水果（如：橙子、橘子）\n"
                    + "进补食材（如：菌菇、杂粮）";
        }

        String analysis = "基于当前月份（" + month + "月，" + season + "），推荐应季商品";
        String suggestions = "现在是" + season + "，建议补充以下应季商品：\n"
                + recommendation + "\n提前布局，抢占流量！";

        return Map.of("analysis", analysis, "suggestions", suggestions);
    }

    private LocalDateTime calculateStartTime(String dateType) {
        LocalDateTime now = LocalDateTime.now();
        return switch (dateType.toLowerCase(Locale.ROOT)) {
            case "week" -> now.minusDays(7);
            case "year" -> now.minusYears(1);
            case "month" -> now.minusMonths(1);
            default -> now.minusMonths(1);
        };
    }

    private String toRangeLabel(String dateType) {
        return switch (dateType.toLowerCase(Locale.ROOT)) {
            case "week" -> "7天";
            case "year" -> "1年";
            case "month" -> "30天";
            default -> "30天";
        };
    }

    private String formatDayLabel(LocalDate date, String dateType) {
        if ("week".equalsIgnoreCase(dateType)) {
            LocalDate today = LocalDate.now();
            if (date.equals(today)) {
                return "今天";
            }
            if (date.equals(today.minusDays(1))) {
                return "昨天";
            }
            if (date.equals(today.minusDays(2))) {
                return "前天";
            }
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            return switch (dayOfWeek) {
                case MONDAY -> "周一";
                case TUESDAY -> "周二";
                case WEDNESDAY -> "周三";
                case THURSDAY -> "周四";
                case FRIDAY -> "周五";
                case SATURDAY -> "周六";
                case SUNDAY -> "周日";
            };
        }
        if ("month".equalsIgnoreCase(dateType)) {
            return date.format(DateTimeFormatter.ofPattern("M月d日"));
        }
        if ("year".equalsIgnoreCase(dateType)) {
            YearMonth ym = YearMonth.from(date.with(TemporalAdjusters.firstDayOfMonth()));
            return ym.format(DateTimeFormatter.ofPattern("yyyy年M月"));
        }
        return date.format(DateTimeFormatter.ISO_DATE);
    }
}
