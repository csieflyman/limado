set JAVA_HOME=@java_home@
set JETTY_HOME=@jetty_home@
set JETTY_BASE=@jetty_base_dir@
set JETTY_LOGS=@jetty_base_dir@/logs

"%JAVA_HOME%/bin/java" -jar "%JETTY_HOME%/start.jar" --exec -Djetty.home="%JETTY_HOME%" -Djetty.base="%JETTY_BASE%" -Djetty.logging.dir="%JETTY_LOGS%" -DSTOP.PORT=@jetty_stop_port@ -DSTOP.KEY=STOP @jvm_options@
