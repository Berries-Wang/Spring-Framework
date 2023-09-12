### 1. application/json

    ajax请求中   content-type:application/json,这样也能在后台接受前台提交的数据，其实这个时候前端提交的数据是 **json格式的字符串**，后端要用@requestbody注解来接收

- 因此，在传递参数时需要将参数转换为JSON格式的字符串(JSONObject.toJSONString(params);)
- ![](HTTP%E4%B9%8BContentType.resources/DE4FDB44-4957-4968-B235-6CD5320CE94C.png)

### 2. application/x-www-form-urlencoded

- ![](HTTP%E4%B9%8BContentType.resources/1919599D-C2C7-491B-B74B-B55C67AC0494.png)

  - 使用这样的格式之后，在Controller中就不需要使用@RequestBody了，即

    - ![](HTTP%E4%B9%8BContentType.resources/5D1FF333-159C-4346-A689-EFCC1D829760.png)