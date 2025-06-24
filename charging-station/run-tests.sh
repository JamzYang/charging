#!/bin/bash

# 充电站管理模块测试运行脚本
# 
# 该脚本用于运行完整的测试套件，包括单元测试、集成测试和端到端测试
# 
# 使用方法:
#   ./run-tests.sh [test-type]
# 
# test-type 可选值:
#   basic       - 只运行基础逻辑测试（轻量级，无依赖）
#   unit        - 只运行单元测试
#   integration - 只运行集成测试
#   e2e         - 只运行端到端测试
#   all         - 运行所有测试 (默认)
#   coverage    - 运行测试并生成覆盖率报告

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# 打印标题
print_title() {
    echo
    print_message $BLUE "=================================================="
    print_message $BLUE "$1"
    print_message $BLUE "=================================================="
    echo
}

# 检查 Java 版本
check_java_version() {
    print_title "检查 Java 版本"
    
    if ! command -v java &> /dev/null; then
        print_message $RED "错误: 未找到 Java 命令"
        exit 1
    fi
    
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt 21 ]; then
        print_message $RED "错误: 需要 Java 21 或更高版本，当前版本: $java_version"
        exit 1
    fi
    
    print_message $GREEN "Java 版本检查通过: $(java -version 2>&1 | head -n 1)"
}

# 启动测试数据库
start_test_database() {
    print_title "启动测试数据库"
    
    # 检查是否有 Docker
    if command -v docker &> /dev/null; then
        print_message $YELLOW "使用 Docker 启动测试数据库..."
        
        # 启动 H2 数据库容器（如果需要）
        # docker run -d --name test-h2 -p 9092:9092 -p 8082:8082 oscarfonts/h2:latest
        
        print_message $GREEN "测试数据库已启动"
    else
        print_message $YELLOW "未找到 Docker，使用内存数据库"
    fi
}

# 运行基础逻辑测试（轻量级，无依赖）
run_basic_tests() {
    print_title "运行基础逻辑测试"

    print_message $YELLOW "编译基础测试类..."
    javac src/test/java/com/ys/charging/station/TestRunner.java \
          src/test/java/com/ys/charging/station/BusinessLogicTest.java \
          src/test/java/com/ys/charging/station/AllTestsRunner.java

    if [ $? -eq 0 ]; then
        print_message $YELLOW "运行完整测试套件..."
        java -cp src/test/java com.ys.charging.station.AllTestsRunner

        if [ $? -eq 0 ]; then
            print_message $GREEN "基础逻辑测试完成 - 所有测试通过！"
        else
            print_message $RED "基础逻辑测试失败"
            exit 1
        fi
    else
        print_message $RED "基础测试编译失败"
        exit 1
    fi
}

# 运行单元测试
run_unit_tests() {
    print_title "运行单元测试"

    print_message $YELLOW "运行领域模型单元测试..."
    ./gradlew test --tests "**/domain/model/**/*Test" -Dspring.profiles.active=test

    print_message $YELLOW "运行应用服务单元测试..."
    ./gradlew test --tests "**/application/service/**/*Test" -Dspring.profiles.active=test

    print_message $YELLOW "运行领域服务单元测试..."
    ./gradlew test --tests "**/domain/service/**/*Test" -Dspring.profiles.active=test

    print_message $GREEN "单元测试完成"
}

# 运行集成测试
run_integration_tests() {
    print_title "运行集成测试"

    print_message $YELLOW "运行仓储层集成测试..."
    ./gradlew test --tests "**/infrastructure/persistence/**/*IntegrationTest" -Dspring.profiles.active=test

    print_message $YELLOW "运行缓存服务集成测试..."
    ./gradlew test --tests "**/infrastructure/cache/**/*IntegrationTest" -Dspring.profiles.active=test

    print_message $YELLOW "运行 REST API 集成测试..."
    ./gradlew test --tests "**/interfaces/rest/**/*IntegrationTest" -Dspring.profiles.active=test

    print_message $GREEN "集成测试完成"
}

# 运行端到端测试
run_e2e_tests() {
    print_title "运行端到端测试"

    print_message $YELLOW "运行完整业务流程测试..."
    ./gradlew test --tests "**/*EndToEndTest" -Dspring.profiles.active=test

    print_message $GREEN "端到端测试完成"
}

# 运行所有测试
run_all_tests() {
    print_title "运行所有测试"

    ./gradlew test -Dspring.profiles.active=test

    print_message $GREEN "所有测试完成"
}

# 生成测试覆盖率报告
generate_coverage_report() {
    print_title "生成测试覆盖率报告"

    print_message $YELLOW "运行测试并生成覆盖率报告..."
    ./gradlew clean test jacocoTestReport -Dspring.profiles.active=test

    if [ -f "build/reports/jacoco/test/html/index.html" ]; then
        print_message $GREEN "覆盖率报告已生成: build/reports/jacoco/test/html/index.html"

        # 尝试打开报告
        if command -v open &> /dev/null; then
            open build/reports/jacoco/test/html/index.html
        elif command -v xdg-open &> /dev/null; then
            xdg-open build/reports/jacoco/test/html/index.html
        fi
    else
        print_message $RED "覆盖率报告生成失败"
    fi
}

# 清理测试环境
cleanup() {
    print_title "清理测试环境"
    
    # 停止测试数据库容器（如果有）
    if command -v docker &> /dev/null; then
        docker stop test-h2 2>/dev/null || true
        docker rm test-h2 2>/dev/null || true
    fi
    
    print_message $GREEN "清理完成"
}

# 显示帮助信息
show_help() {
    echo "充电站管理模块测试运行脚本"
    echo
    echo "使用方法:"
    echo "  $0 [test-type]"
    echo
    echo "test-type 可选值:"
    echo "  basic       - 只运行基础逻辑测试（轻量级，无依赖）"
    echo "  unit        - 只运行单元测试"
    echo "  integration - 只运行集成测试"
    echo "  e2e         - 只运行端到端测试"
    echo "  all         - 运行所有测试 (默认)"
    echo "  coverage    - 运行测试并生成覆盖率报告"
    echo "  help        - 显示此帮助信息"
    echo
    echo "示例:"
    echo "  $0 basic     # 只运行基础逻辑测试（快速验证）"
    echo "  $0 unit      # 只运行单元测试"
    echo "  $0 coverage  # 生成覆盖率报告"
    echo "  $0           # 运行所有测试"
}

# 主函数
main() {
    local test_type=${1:-all}
    
    case $test_type in
        basic)
            check_java_version
            run_basic_tests
            ;;
        unit)
            check_java_version
            start_test_database
            run_unit_tests
            ;;
        integration)
            check_java_version
            start_test_database
            run_integration_tests
            ;;
        e2e)
            check_java_version
            start_test_database
            run_e2e_tests
            ;;
        all)
            check_java_version
            start_test_database
            run_all_tests
            ;;
        coverage)
            check_java_version
            start_test_database
            generate_coverage_report
            ;;
        help|--help|-h)
            show_help
            exit 0
            ;;
        *)
            print_message $RED "错误: 未知的测试类型 '$test_type'"
            echo
            show_help
            exit 1
            ;;
    esac
    
    # 清理（可选）
    # cleanup
    
    print_title "测试运行完成"
    print_message $GREEN "所有测试已成功完成！"
}

# 捕获中断信号，确保清理
trap cleanup EXIT

# 运行主函数
main "$@"
