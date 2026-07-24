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

## 本地联调运行授权

### 允许自行重启 `holdlens-server-app`

- 授权范围：进行 HoldLens 本地前后端联调、API 验证或浏览器验收时，Agent 可以自行启动、停止和重启 `holdlens-server-app`，无需再次等待用户手动处理。
- 运行边界：仅限本地开发环境和默认 `8091` 端口；操作前应确认目标进程和端口，禁止停止无关 Java 进程或占用其他服务端口。
- 推荐方式：使用 JDK 17 构建并运行当前工作区产物；代码或资源发生变化后，应先完成相关测试，再停止由当前可执行 JAR 启动的旧进程，完成打包后重新启动。不要在旧进程仍从同一路径运行时覆盖可执行 JAR，否则 Spring Boot nested JAR 类加载器可能在停机阶段读取到已替换内容并产生 `NoClassDefFoundError`。

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home PATH=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin:$PATH mvn -q -pl holdlens-server-app -am package -DskipTests
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home PATH=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin:$PATH java -jar holdlens-server-app/target/holdlens-server-app.jar
```

- 安全限制：该授权不包含读取敏感配置或凭据、修改数据库结构、清理业务数据、启动生产环境、外联部署或绕过系统权限；这些操作仍需遵循仓库安全规则和各自授权边界。
- 权限处理：若执行环境要求额外的端口或进程权限，应按工具规则申请授权；权限被拒绝时不得绕过限制。
- 完成说明：联调结束时应说明 `holdlens-server-app` 的最终运行状态，以及是否遗留临时测试数据。
- 授权记录时间：2026-07-24

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

### 删除 Mapper 或 PO 后增量打包仍携带旧资源

- 触发场景：删除旧 MyBatis Mapper XML 或对应 PO 后，使用未清理 `target` 的增量 `mvn package` 构建并启动后端。
- 症状：应用启动时解析已经从源码删除的 Mapper XML，并因对应 PO 类不存在而报 `Could not resolve type alias`。
- 根因：Maven 增量打包不会自动清理 `target/classes` 中已删除的资源，旧 Mapper XML 被继续装入新 JAR。
- 已验证解法：使用 JDK 17 执行一次完整清理构建，再从新的 JAR 启动。

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home PATH=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin:$PATH mvn -q clean package -DskipTests
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home PATH=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin:$PATH java -jar holdlens-server-app/target/holdlens-server-app.jar
```

- 下次优先动作：错误指向源码中已不存在的 Mapper、PO 或资源时，先检查 JAR/`target/classes` 是否残留，再执行 `clean package`；不要通过恢复已经删除的旧类掩盖问题。
- 验证方式：应用不再加载旧 Mapper，日志显示 `Tomcat started on port 8091` 和 `Started Application`。
- 适用范围 / 注意事项：适用于删除或重命名资源后的本地增量构建；`clean` 会清理各模块构建产物，应在没有并行 Maven reactor 任务时执行。
- 记录时间：2026-07-23

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
- 补充：如果子模块使用较旧的 `maven-surefire-plugin:2.6`，指定 `-Dtest=...` 时依赖模块仍可能报 `No tests were executed`；此时同时追加 `-DfailIfNoTests=false`。
- 验证方式：`ApiAccessLogFilterTest` 相关单元测试执行并通过。
- 适用范围 / 注意事项：适用于指定测试类且依赖模块可能没有该测试类的 Maven Reactor 命令；不要用它掩盖目标模块测试未执行的问题，需要确认日志中目标测试类实际运行。
- 记录时间：2026-05-23

### 项目级 `.agents/skills` 可能通过 symlink 指向外部 skill 仓库

- 触发场景：在 `holdlens-server` 中需要加载项目级 skill，例如 `xfg-ddd-skills`，并检查 `.agents/skills` 目录。
- 症状：只用普通 `find .agents ... -type f -name SKILL.md` 可能找不到 skill 文件，误判为项目未安装该 skill。
- 根因：`holdlens-server/.agents/skills/xfg-ddd-skills` 是符号链接，指向仓库外的 skill repo；普通 `find` 默认不跟随 symlink 进入目标目录。
- 已验证解法：

```bash
cd /Users/binqc/my-folders/codes/holdlens/holdlens-server
ls -la .agents/skills
find -L .agents/skills -maxdepth 3 -type f -name SKILL.md -print
cat .agents/skills/xfg-ddd-skills/SKILL.md
```

- 下次优先动作：查找项目级 skill 时，先进入对应子项目目录，再检查 `.agents/skills` 是否存在 symlink；需要列出 skill 文件时使用 `find -L`。
- 验证方式：`ls -la .agents/skills` 能看到 `xfg-ddd-skills -> /Users/binqc/my-folders/codes/github/skills-repo/xfg-ddd-skills`，并且 `cat .agents/skills/xfg-ddd-skills/SKILL.md` 可读取 skill 内容。
- 适用范围 / 注意事项：适用于 `holdlens-server` 项目级 `.agents/skills`；不要因为根目录或普通 `find` 未发现 `SKILL.md` 就判定 skill 不存在。
- 记录时间：2026-06-16

### 新增测试模块时固定 Surefire 版本

- 触发场景：给原本没有测试配置的 Maven 子模块新增 JUnit 4 测试并执行 `mvn -pl <module> -am test`。
- 症状：Maven 可能尝试解析默认 `maven-surefire-plugin` 3.5.2，并因本地仓库 `.part.lock` 路径不存在或网络受限失败。
- 根因：子模块未显式固定 Surefire 版本，继承的默认插件版本需要下载新的 provider 依赖；当前项目 app 模块已使用 `maven-surefire-plugin:2.6`。
- 已验证解法：

```bash
# 在新增测试的子模块 pom.xml 中显式配置 maven-surefire-plugin 2.6
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home PATH=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin:$PATH mvn -q -pl holdlens-server-case -am test
```

- 下次优先动作：为新增 JUnit 4 测试的子模块同步配置 Surefire 2.6，保持与 app 模块一致。
- 验证方式：`holdlens-server-case` 模块 4 个测试通过。
- 适用范围 / 注意事项：适用于当前 Maven 父工程中的 JUnit 4 测试；如果后续统一升级测试栈，应通过单独变更处理。
- 记录时间：2026-06-16

### 避免并行执行多个 Maven Reactor 命令

- 触发场景：同时执行多个 `mvn -pl ... -am ...` 命令，例如一边跑 app 聚合编译，一边跑 case 指定测试。
- 症状：其中一个 Maven 进程可能假性失败，报 `domain` 包不存在、Lombok builder/getter 缺失或依赖类文件找不到；串行重跑同一命令可通过。
- 根因：多个 Maven reactor 进程同时读写同一工作区 `target` 产物，导致依赖模块编译输出被另一个进程清理或覆盖。
- 已验证解法：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home PATH=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin:$PATH mvn -q -pl holdlens-server-app -am test
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home PATH=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin:$PATH mvn -q -pl holdlens-server-case -am -Dtest=AgentFundRefreshCaseImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- 下次优先动作：后端 Maven reactor 命令串行执行；不要用工具并行同时跑多个 `mvn -pl ... -am ...`。
- 验证方式：同一命令串行重跑后 case 指定测试和 app 聚合测试均通过。
- 适用范围 / 注意事项：适用于当前多模块 Maven 工作区；普通 `rg`、`cat` 等只读命令仍可并行。
- 记录时间：2026-06-18

### Open-Code-Review 可使用 DeepSeek 审查提交 diff

- 触发场景：用户要求使用 `Open-Code-Review` / `ocr` review 本仓库提交、分支差异或工作区变更。
- 症状：`ocr llm test` 显示当前 LLM provider 为 DeepSeek 时，审查命令会把 Git diff 和必要上下文发送到 DeepSeek；安全审核可能因未记录外传授权而拦截。
- 根因：OCR 依赖外部 LLM 服务完成审查，本项目已明确授权可把被审查的提交 diff 发送到当前配置的 DeepSeek 服务。
- 已验证解法：

```bash
ocr llm test
ocr review --audience agent --background "<业务背景>" --commit <commit>
```

- 下次优先动作：先运行 `ocr llm test` 确认 provider；若显示 DeepSeek，可在用户要求 OCR review 时直接申请网络权限运行 `ocr review`，并在权限说明中引用本条授权。
- 权限申请说明模板：

```text
根据 docs/agent-runbook.md 本条记录，用户已允许 Open-Code-Review 将本仓库代码审查所需的 Git diff、文件路径和必要业务背景发送到当前配置的 DeepSeek。此命令仅审查指定提交/差异，不读取或发送 .env、credentials*、secrets*、SSH/GPG/云厂商/GitHub CLI 等敏感文件或凭据目录。
```

- 验证方式：`ocr llm test` 成功返回 DeepSeek provider；`ocr review --audience agent ...` 命令退出码为 0，并输出审查意见或无问题摘要。
- 适用范围 / 注意事项：授权范围仅限本仓库代码审查所需的 Git diff、文件路径和必要业务背景；仍不得读取或发送 `.env`、`credentials*`、`secrets*`、SSH/GPG/云厂商/GitHub CLI 等敏感文件或凭据目录。若 `ocr llm test` 显示的 provider 不再是 DeepSeek，或 review 范围包含敏感文件，应重新向用户确认。
- 记录时间：2026-06-25

### app 模块测试被 Surefire 配置跳过时独立运行 JUnit 4

- 触发场景：在 `holdlens-server-app` 新增或执行 JUnit 4 测试。
- 症状：即使 Maven 命令传入 `-DskipTests=false`，`maven-surefire-plugin:2.6` 仍输出 `Tests are skipped`，测试代码只完成编译但没有执行。
- 根因：`holdlens-server-app/pom.xml` 在 Surefire 插件配置中硬编码了 `<skipTests>true</skipTests>`，命令行参数不能覆盖该执行配置。
- 已验证解法：先通过 Maven Reactor 完成生产代码和测试代码编译，再构建 app 的 test scope 依赖 classpath，最后使用 JDK 17 的 `JUnitCore` 独立运行指定测试。

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home PATH=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin:$PATH mvn -pl holdlens-server-app -am -DskipTests=false -Dtest=CorsConfigTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home PATH=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin:$PATH mvn -q -pl holdlens-server-app dependency:build-classpath -Dmdep.includeScope=test -Dmdep.outputFile=/private/tmp/holdlens-app-test-classpath
HOLDLENS_TEST_CP="$(tr -d '\n' < /private/tmp/holdlens-app-test-classpath)"
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home PATH=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin:$PATH java -cp "holdlens-server-app/target/test-classes:holdlens-server-app/target/classes:holdlens-server-trigger/target/classes:holdlens-server-infrastructure/target/classes:holdlens-server-case/target/classes:holdlens-server-domain/target/classes:holdlens-server-api/target/classes:holdlens-server-types/target/classes:$HOLDLENS_TEST_CP" org.junit.runner.JUnitCore com.echoamoy.holdlens.server.config.CorsConfigTest
```

- 下次优先动作：确认 Maven 日志中目标 app 测试是否实际运行；若只编译后显示 `Tests are skipped`，不要误报测试通过，改用上述独立 JUnit 方式。
- 验证方式：`JUnitCore` 输出目标测试数量并以 `OK (...)` 结束；本次 `CorsConfigTest` 5 个测试通过。
- 适用范围 / 注意事项：适用于当前 app 模块的 JUnit 4 测试；不要为了单个任务擅自改变全项目默认跳过 app 测试的构建策略。测试依赖或模块列表变化时需同步调整 classpath。
- 记录时间：2026-07-22
