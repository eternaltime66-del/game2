@echo off
cd /d "%~dp0"

echo [dev] 检查 Java 版本...
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set JVER=%%v
echo [dev] 当前: %JVER%
echo %JVER% | findstr /r "\"1[7-9]\." "\"2[0-9]\." >nul
if errorlevel 1 (
  echo [dev] 警告: 本项目需要 JDK 17+，当前 Java 可能导致编译/热更新失败
  echo [dev] 请安装 JDK 17 并设置 JAVA_HOME，或在 Cursor 中用 "Spring Boot (dev 热更新)" 启动
)

if not exist ".reloadtrigger" echo.>.reloadtrigger

echo [dev] Spring Boot 开发模式
echo [dev] - 改 Java 后保存，等待编译完成即自动重启
echo [dev] - 改 static 下前端文件保存后浏览器自动刷新
echo [dev] - 若未自动重启，可修改 .reloadtrigger 文件触发
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=true
pause
