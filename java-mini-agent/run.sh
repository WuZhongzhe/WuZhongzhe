#!/usr/bin/env bash
# 启动智能体服务
set -e
cd "$(dirname "$0")"
if [ ! -d out ]; then
  ./build.sh
fi
java -Dfile.encoding=UTF-8 -cp out com.team.agent.Main
