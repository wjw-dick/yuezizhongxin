@echo off
rem 后台前端启动器：由 scripts/dev-start.py 或计划任务调用，独立于父进程运行
cd /d D:\ai\yuezizhongxin\admin-web
if not exist ..\logs mkdir ..\logs
set VITE_API_TARGET=http://127.0.0.1:18090
call npm run dev >> ..\logs\admin-web.out 2>&1
