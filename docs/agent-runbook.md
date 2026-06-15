# Agent Runbook

本文件用于沉淀 AI / agent 在本项目中遇到的可复用问题和已验证解法。后续遇到同类场景时，应优先查阅并尝试这里记录的方案，避免重复排查。

## 记录规则

- 只记录项目相关、可复用且已经验证成功的问题解法。
- 每条记录应包含触发场景、症状、根因、已验证解法、下次优先动作和验证方式。
- 不记录未验证猜测、一次性临时问题、长篇推理过程或与本项目无关的通用知识。
- 如果后续发现更准确或更简单的解法，应更新已有记录，而不是新增重复条目。

## 适合记录的问题类型

已经在本项目中遇到、验证出稳定解法，并且后续可能重复出现的问题，适合沉淀到“已知问题与解法”。常见包括：

- 本机环境与项目要求不一致，例如语言版本、包管理器版本、固定路径或环境变量。
- 项目运行约束，例如端口、服务启动边界、模块特殊运行或测试方式。
- 命令执行问题，例如构建、测试、打包、依赖安装、缓存、权限或沙箱导致的失败及已验证解法。

仅有问题类型但没有具体验证结果时，不要新增到“已知问题与解法”。

## 使用流程

```text
执行前遇到环境、依赖、构建、测试、权限、端口或命令风险
  ↓
先查阅本文件是否已有同类记录
  ↓
优先尝试已验证解法
  ↓
如果命令失败或行为异常，定位原因并尝试最小修复
  ↓
修复成功后判断是否项目相关、可复用且已验证
  ↓
是：按模板新增或更新记录
  ↓
回复用户时说明沉淀或更新了哪条经验
```

## 记录模板

````md
### 问题标题

- 触发场景：
- 症状：
- 根因：
- 已验证解法：

```bash
# 如有需要，写入可直接执行或参考的命令
```

- 下次优先动作：
- 验证方式：
- 适用范围 / 注意事项：
- 记录时间：
````

## 已知问题与解法

### 后端命令需要使用 JDK 17

- 触发场景：执行 Maven、运行 Java 命令、运行后端测试或处理后端 application 相关验证时。
- 症状：本机默认 Java 环境可能是 Java 8，但本项目后端实际需要 Java 17。
- 根因：项目后端运行和验证依赖 JDK 17，而本机默认 Java 版本不一定符合项目要求。
- 已验证解法：

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
java -version
```

- 下次优先动作：执行后端相关 Maven / Java 命令前，先临时切换到 JDK 17。
- 验证方式：`java -version` 显示 Java 17，相关 Maven / Java 命令通过。
- 适用范围 / 注意事项：适用于后端 Maven、Java、测试和 application 相关命令；不要因此自行启动或重启后端服务，后端默认由用户启动。
- 记录时间：2026-05-23

### Reactor 指定测试需要允许依赖模块无匹配测试

- 触发场景：使用 Maven Reactor 针对下游模块运行指定测试，例如 `mvn -pl mall-trigger -am -Dtest=ApiAccessLogFilterTest test`。
- 症状：依赖模块没有匹配 `-Dtest` 的测试时，Surefire 在依赖模块提前失败，报 `No tests matching pattern ... were executed`，目标模块测试尚未执行。
- 根因：`-am` 会先构建依赖模块，`-Dtest` 同时传给每个模块；没有匹配测试的模块默认视为失败。
- 已验证解法：

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
mvn -pl mall-trigger -am -Dtest=ApiAccessLogFilterTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- 下次优先动作：运行指定模块测试且带 `-am` 时，追加 `-Dsurefire.failIfNoSpecifiedTests=false`。
- 验证方式：`ApiAccessLogFilterTest` 相关单元测试执行并通过。
- 适用范围 / 注意事项：适用于指定测试类且依赖模块可能没有该测试类的 Maven Reactor 命令；不要用它掩盖目标模块测试未执行的问题，需要确认日志中目标测试类实际运行。
- 记录时间：2026-05-23
