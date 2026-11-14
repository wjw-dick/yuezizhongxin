#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""本地一键启动：MySQL 容器自检 → 后端 18080 → 后台前端 5173。

使用 DETACHED_PROCESS 启动，脱离父 shell，关闭终端后服务仍在运行。
用法： python scripts/dev-start.py
"""

import json
import os
import socket
import subprocess
import sys
import time
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LOGS = os.path.join(ROOT, "logs")
PID_FILE = os.path.join(LOGS, "dev-pids.json")
MYSQL_CONTAINER = "yzz-mysql-dev"
BACKEND_PORT = int(os.environ.get("YZZ_PORT", "18090"))
FRONTEND_PORT = 5173

DETACHED_PROCESS = 0x00000008
CREATE_NEW_PROCESS_GROUP = 0x00000200
CREATE_NO_WINDOW = 0x08000000


def port_open(port, host="127.0.0.1"):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.settimeout(0.5)
        if sock.connect_ex((host, port)) == 0:
            return True
    with socket.socket(socket.AF_INET6, socket.SOCK_STREAM) as sock:
        sock.settimeout(0.5)
        return sock.connect_ex(("::1", port, 0, 0)) == 0


def wait_port(port, seconds, label):
    for _ in range(seconds):
        if port_open(port):
            print("  [ok] %s 已监听 %s" % (label, port))
            return True
        time.sleep(1)
    print("  [fail] %s 未在 %ss 内监听 %s" % (label, seconds, port))
    return False


def http_ok(url):
    try:
        with urllib.request.urlopen(url, timeout=3) as resp:
            return resp.status == 200
    except Exception:
        return False


def login_ok(port):
    """用演示账号真实登录一次，确认库连得上、令牌签得出来。"""
    body = json.dumps({"username": "admin", "password": "Admin@123456"}).encode()
    req = urllib.request.Request(
        "http://127.0.0.1:%d/api/tenant/auth/login" % port,
        data=body, method="POST",
        headers={"Content-Type": "application/json", "X-Tenant-Code": "demo"},
    )
    try:
        with urllib.request.urlopen(req, timeout=8) as resp:
            payload = json.loads(resp.read().decode())
            return payload.get("code") == 0 and bool(payload.get("data", {}).get("token"))
    except Exception as exc:
        print("  [warn] 登录自检异常: %s" % exc)
        return False


def start(name, args, cwd, log_name, env=None):
    log_path = os.path.join(LOGS, log_name)
    handle = open(log_path, "w", encoding="utf-8", errors="replace")
    proc = subprocess.Popen(
        args,
        cwd=cwd,
        stdout=handle,
        stderr=subprocess.STDOUT,
        stdin=subprocess.DEVNULL,
        creationflags=DETACHED_PROCESS | CREATE_NEW_PROCESS_GROUP | CREATE_NO_WINDOW,
        env=env,
        close_fds=True,
    )
    print("  [start] %s pid=%s log=%s" % (name, proc.pid, log_path))
    return proc.pid


def main():
    os.makedirs(LOGS, exist_ok=True)
    pids = {}

    print("[1/4] 检查 MySQL 容器 %s" % MYSQL_CONTAINER)
    try:
        running = subprocess.run(
            ["docker", "inspect", "-f", "{{.State.Running}}", MYSQL_CONTAINER],
            capture_output=True, text=True, timeout=20,
        ).stdout.strip()
    except Exception as exc:
        print("  [fail] 无法调用 docker: %s" % exc)
        return 1
    if running != "true":
        print("  [start] 启动容器")
        subprocess.run(["docker", "start", MYSQL_CONTAINER], capture_output=True, timeout=60)
        time.sleep(6)
    wait_port(3308, 40, "MySQL")

    print("[2/4] 启动后端")
    if port_open(BACKEND_PORT):
        print("  [skip] %s 已被占用，先执行 python scripts/dev-stop.py" % BACKEND_PORT)
        return 1
    java = os.environ.get("JAVA_HOME")
    java_bin = os.path.join(java, "bin", "java.exe") if java else "java"
    url = ("jdbc:mysql://127.0.0.1:3308/yzz_platform?useUnicode=true&characterEncoding=utf8"
           "&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false")
    pids["backend"] = start(
        "backend",
        [java_bin, "-jar", os.path.join("yzz-api", "target", "yzz-api.jar"),
         "--server.port=%d" % BACKEND_PORT,
         "--spring.datasource.url=" + url,
         "--spring.datasource.password=root"],
        ROOT, "backend.out",
    )
    if not wait_port(BACKEND_PORT, 90, "后端"):
        return 1

    print("[3/4] 启动后台前端")
    if port_open(FRONTEND_PORT):
        print("  [skip] %s 已被占用，先执行 python scripts/dev-stop.py" % FRONTEND_PORT)
        return 1
    front_env = dict(os.environ)
    front_env["VITE_API_TARGET"] = "http://127.0.0.1:%d" % BACKEND_PORT
    pids["frontend"] = start(
        "frontend",
        ["cmd", "/c", "npm run dev"],
        os.path.join(ROOT, "admin-web"), "admin-web.out", env=front_env,
    )
    if not wait_port(FRONTEND_PORT, 60, "前端"):
        return 1

    print("[4/4] 健康检查")
    print("  后端登录自检: %s" % ("ok" if login_ok(BACKEND_PORT) else "fail"))
    print("  前端首页: %s" % ("ok" if http_ok("http://localhost:%d/" % FRONTEND_PORT) else "fail"))

    with open(PID_FILE, "w", encoding="utf-8") as fp:
        json.dump(pids, fp, indent=2)
    print("\n启动完成：")
    print("  后台前端  http://localhost:%d  （租户 demo / admin / Admin@123456）" % FRONTEND_PORT)
    print("  后端接口  http://127.0.0.1:%d/api" % BACKEND_PORT)
    print("  接口文档  http://127.0.0.1:%d/swagger-ui.html" % BACKEND_PORT)
    print("  停止服务  python scripts/dev-stop.py")
    return 0


if __name__ == "__main__":
    sys.exit(main())
