# AI 推荐功能接口文档

## 接口概览

| 接口 | 路径 | 方法 | 说明 |
|------|------|------|------|
| 智能推荐 | `/api/ai/recommendation` | GET | 基于用户行为的个性化推荐 |
| 搭配推荐 | `/api/ai/collocation` | GET | 商品搭配推荐 |
| 记录行为 | `/api/ai/behavior` | POST | 记录用户浏览/购买等行为 |
| AI 聊天 | `/api/ai/chat` | POST | AI 智能对话 |
| AI 建议 | `/api/ai/advice` | POST | AI 农业建议 |

---

## 1. 智能推荐接口

### 基本信息
- **接口路径**：`/api/ai/recommendation`
- **请求方法**：GET
- **接口说明**：根据用户浏览历史和行为，智能推荐相关商品

### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| - | - | - | 从 Session 中获取当前用户ID |

### 响应数据
```json
{
  "code": 0,
  "success": true,
  "message": "获取智能推荐成功",
  "data": {
    "reason": "根据您的浏览历史",
    "products": [
      {
        "id": 1,
        "name": "散养土山鸡",
        "price": 19.8,
        "unit": "斤",
        "image": "https://your-domain.com/uploads/products/chicken.jpg"
      }
    ]
  }
}
```

### 字段说明
| 字段 | 类型 | 说明 |
|------|------|------|
| reason | String | 推荐原因（"根据您的浏览历史" / "热门推荐"） |
| products | Array | 推荐商品列表 |
| products[].id | Long | 商品ID |
| products[].name | String | 商品名称 |
| products[].price | BigDecimal | 商品价格 |
| products[].unit | String | 商品单位 |
| products[].image | String | 商品图片URL |

### 业务逻辑
1. 查询用户最近浏览的商品
2. 基于浏览商品推荐相似商品（同分类）
3. 如果推荐数量不足，补充热门商品
4. 结果缓存 5 分钟

---

## 2. 搭配推荐接口

### 基本信息
- **接口路径**：`/api/ai/collocation`
- **请求方法**：GET
- **接口说明**：推荐与商品搭配的其他商品

### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| - | - | - | 从 Session 中获取当前用户ID |

### 响应数据
```json
{
  "code": 0,
  "success": true,
  "message": "获取搭配推荐成功",
  "data": [
    {
      "id": 2,
      "name": "有机白萝卜",
      "price": 2.5,
      "unit": "斤",
      "image": "https://your-domain.com/uploads/products/radish.jpg",
      "collocationDescription": "鸡肉炖萝卜 - 经典搭配"
    }
  ]
}
```

### 字段说明
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 商品ID |
| name | String | 商品名称 |
| price | BigDecimal | 商品价格 |
| unit | String | 商品单位 |
| image | String | 商品图片URL |
| collocationDescription | String | 搭配描述（如"牛肉炖萝卜"） |

### 业务逻辑
1. 查询用户最近浏览的商品
2. 基于搭配表推荐相关商品
3. 如果推荐数量不足，补充高分搭配
4. 结果缓存 10 分钟

---

## 3. 记录用户行为接口

### 基本信息
- **接口路径**：`/api/ai/behavior`
- **请求方法**：POST
- **接口说明**：记录用户的浏览、购买等行为，用于优化推荐

### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| productId | Long | 是 | 商品ID |
| behaviorType | Integer | 是 | 行为类型：1-浏览，2-购买，3-加入购物车，4-收藏 |
| duration | Integer | 否 | 浏览时长（秒），默认为0 |

### 请求示例
```json
{
  "productId": 1,
  "behaviorType": 1,
  "duration": 30
}
```

### 响应数据
```json
{
  "code": 0,
  "success": true,
  "message": "记录用户行为成功",
  "data": null
}
```

### 业务逻辑
1. 保存用户行为到数据库
2. 清除该用户的推荐缓存（使下次推荐基于最新行为）

---

## 4. AI 聊天接口

### 基本信息
- **接口路径**：`/api/ai/chat`
- **请求方法**：POST
- **接口说明**：与 AI 进行对话，获取智能回复

### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| content | String | 是 | 用户输入内容 |
| history | Array | 否 | 历史对话记录，默认为空数组 |
| type | String | 否 | 用户类型：consumer/farmer，默认为 consumer |

### 请求示例
```json
{
  "content": "推荐一些新鲜蔬菜",
  "history": [],
  "type": "consumer"
}
```

### 响应数据
```json
{
  "code": 0,
  "success": true,
  "message": "success",
  "data": {
    "content": "我为您推荐以下新鲜蔬菜：西红柿、黄瓜、菠菜..."
  }
}
```

---

## 5. AI 建议接口

### 基本信息
- **接口路径**：`/api/ai/advice`
- **请求方法**：POST
- **接口说明**：获取 AI 的农业种植/养殖建议

### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| content | String | 是 | 咨询内容 |

### 请求示例
```json
{
  "content": "如何种植西红柿"
}
```

### 响应数据
```json
{
  "code": 0,
  "success": true,
  "message": "success",
  "data": {
    "content": "西红柿种植需要注意以下几点：1. 土壤选择..."
  }
}
```

---

## 数据库表结构

### 用户行为表（user_behavior）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| product_id | BIGINT | 商品ID |
| behavior_type | TINYINT | 行为类型：1-浏览，2-购买，3-加购，4-收藏 |
| behavior_time | DATETIME | 行为时间 |
| duration | INT | 浏览时长（秒）|

### 商品搭配表（product_collocation）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| product_id | BIGINT | 商品ID |
| collocation_product_id | BIGINT | 搭配商品ID |
| score | INT | 搭配分数（0-100）|
| description | VARCHAR | 搭配描述 |

### 推荐日志表（recommendation_log）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| recommendation_type | TINYINT | 推荐类型：1-智能，2-搭配 |
| recommendation_data | JSON | 推荐数据 |
| recommendation_time | DATETIME | 推荐时间 |
| click_count | INT | 点击次数 |
| purchase_count | INT | 购买次数 |

---