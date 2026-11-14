#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""停止本地开发服务：按 pid 文件结束后端与前端，可选停止 MySQL 容器。

用法：
  python scripts/dev-stop.py           # 只停前后端
  python scripts/dev-stop.py --all     # 同时停 MySQL 容器
"""

import json
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PID_FILE = os.path.join(ROOT, "logs", "dev-pids.json")


def kill_tree(pid):
    subprocess.run(["taskkill", "/PID", str(pid), "/T", "/F"],
                   capture_output=True, text=True)


def main():
    if not os.path.exists(PID_FILE):
        print("没有 %s，尝试按端口结束本项目进程" % PID_FILE)
        return 1
    with open(PID_FILE, encoding="utf-8") as fp:
        pids = json.load(fp)
    for name, pid in pids.items():
        print("停止 %s (pid=%s)" % (name, pid))
        kill_tree(pid)
    os.remove(PID_FILE)
    if "--all" in sys.argv:
        print("停止 MySQL 容器 yzz-mysql-dev")
        subprocess.run(["docker", "stop", "yzz-mysql-dev"], capture_output=True)
    print("已停止")
    return 0


if __name__ == "__main__":
    sys.exit(main())
