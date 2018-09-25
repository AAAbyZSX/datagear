#!/bin/sh

JAVA_HOME=$JAVA_HOME
JAVA_OPTS=$JAVA_OPTS

APP_NAME="Data Gear"
ECHO_PREFIX="[$APP_NAME]  :"
BORDER="========================================="

APP_MAIN="org.datagear.webembd.App"

APP_PID=0

echo "$BORDER"
echo "$ECHO_PREFIX using JAVA_HOME '$JAVA_HOME'"

readAppPID(){
	JAVAPS=`$JAVA_HOME/bin/jps -l | grep $APP_MAIN`

	if [ -n "$JAVAPS" ]; then
		APP_PID=`echo $JAVAPS | awk '{print $1}'`
	else
		APP_PID=0
	fi
}

readAppPID

if [ $APP_PID -ne 0 ]; then
	echo "$ECHO_PREFIX application is already running, PID is $APP_PID"
	echo "$ECHO_PREFIX starting [Failed]"
else
	echo "$ECHO_PREFIX starting..."
	nohup $JAVA_HOME/bin/java $JAVA_OPTS -cp .:lib/* $APP_MAIN >/dev/null 2>&1 &
	readAppPID
	if [ $APP_PID -ne 0 ]; then
		echo "$ECHO_PREFIX PID is $APP_PID"
		echo "$ECHO_PREFIX starting [OK]"
	else
		echo "$ECHO_PREFIX starting [Failed]"
	fi
fi

echo "$BORDER"