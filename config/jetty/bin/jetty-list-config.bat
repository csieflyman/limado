set JAVA_HOME=@java_home@
set JETTY_HOME=@jetty_home@
set JETTY_BASE=@jetty_base_dir@
set JETTY_LOGS=@jetty_base_dir@/logs

"%JAVA_HOME%/bin/java" -jar "%JETTY_HOME%/start.jar" --list-config --list-modules -Djetty.home="%JETTY_HOME%" -Djetty.base="%JETTY_BASE%"
pause
