## 评价模块API

### 已实现的API接口

**1. 提交评价接口**
- **API路径**: `POST /api/evaluations`
- **请求体**:
  ```json
  {
    "orderId": "24",
    "productId": 29,
    "score": 5,
    "content": "新鲜牛肉，价格实惠，会再次购买",
    "images": ["https://example.com/image1.png"],
    "tags": ["包装精美"]
  }
  ```
- **响应格式**:
  ```json
  {
    "id": 6,
    "productId": 29,
    "orderId": 24,
    "score": 5,
    "content": "新鲜牛肉，价格实惠，会再次购买",
    "images": ["https://example.com/image1.png"],
    "tags": ["包装精美"],
    "nickName": "1**1",
    "createTime": "2026-04-07T18:39:29.689116400"
  }
  ```

**2. 获取评价列表接口**
- **API路径**: `GET /api/evaluations`
- **查询参数**:
  - `productId`: 商品ID（必填）
  - `page`: 页码（可选，默认1）
  - `pageSize`: 每页数量（可选，默认10）
- **响应格式**:
  ```json
  {
    "code": 0,
    "success": true,
    "message": "获取评价列表成功",
    "data": {
      "list": [
        {
          "id": 6,
          "productId": 29,
          "score": 5,
          "content": "新鲜牛肉，价格实惠，会再次购买",
          "images": ["https://example.com/image1.png"],
          "tags": ["包装精美"],
          "nickName": "1**1",
          "createTime": "2026-04-07T18:39:29.689116400"
        }
      ],
      "total": 2,
      "page": 1,
      "pageSize": 10,
      "hasMore": false
    }
  }
  ```
