#!/usr/bin/env bash
# 编译整个项目，产物输出到 out 目录
set -e
cd "$(dirname "$0")"
rm -rf out
mkdir -p out
find src -name "*.java" > .sources.txt
javac -encoding UTF-8 -d out @.sources.txt
rm -f .sources.txt
echo "编译完成，输出目录：out"
