@echo off
cd /d "%~dp0"
echo [dev] Spring Boot 开发模式 - 改 Java 后自动编译即可触发热重启
echo [dev] 静态资源改完保存后浏览器会自动刷新 (LiveReload)
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
pause
