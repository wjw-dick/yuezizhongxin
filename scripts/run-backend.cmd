@echo off
rem 后端启动器：由 scripts/dev-start.py 或计划任务调用，独立于父进程运行
cd /d D:\ai\yuezizhongxin
if not exist logs mkdir logs
set MYSQL_PORT=3308
java -jar yzz-api\target\yzz-api.jar --server.port=18090 "--spring.datasource.url=jdbc:mysql://127.0.0.1:3308/yzz_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false" --spring.datasource.password=root >> logs\backend.out 2>&1
