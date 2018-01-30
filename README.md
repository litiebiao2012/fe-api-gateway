# fe-api-gateway 基于dubbo后端服务的统一网关服务

##功能介绍
###1.动态发现后端服务
###2.新增服务网关无需重启
###3.统一包装协议
###4.集群部署
###5.接口性能监控
###6.统一入口
###7.授权统一认证



##使用方式
### 1. 统一网关入口url使用方式
#### ur: http://host:port/api
#### body:method=app.service.method&params=jsonrpc协议
#### app:应用名称 service:服务名称 method:方法名称

### 2. 打印实时提供作者信息方便排查错误
#### ur: http://host:port/echoProviders

### 3. 接口调用查看

