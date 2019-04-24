#! /bin/bash

conf_file="/etc/serposcope/serposcope.conf"

# replace db options
sed -i "s/%SERPOSCOPE_DB_HOST%/${SERPOSCOPE_DB_HOST}/" ${conf_file}
sed -i "s/%SERPOSCOPE_DB_PORT%/${SERPOSCOPE_DB_PORT}/" ${conf_file}
sed -i "s/%SERPOSCOPE_DB_USER%/${SERPOSCOPE_DB_USER}/" ${conf_file}
sed -i "s/%SERPOSCOPE_DB_PASSWORD%/${SERPOSCOPE_DB_PASSWORD}/" ${conf_file}

export MALLOC_ARENA_MAX=4

exec java -server \
          -Dserposcope.conf=${conf_file} \
          -Xmx${HEAP_SIZE} \
          -XX:MetaspaceSize=512m \
          -XX:+UseCompressedOops \
          -XX:CompressedClassSpaceSize=128m \
          -Xloggc:/var/log/serposcope/gc.log \
          -XX:+PrintGCDetails \
          -XX:+PrintGCDateStamps \
          -XX:+UseGCLogFileRotation \
          -XX:NumberOfGCLogFiles=5 \
          -XX:GCLogFileSize=10m \
          -XX:+HeapDumpOnOutOfMemoryError \
          -XX:HeapDumpPath=/var/log/serposcope/ \
          -XX:ErrorFile=/var/log/serposcope/log/hs_err_pid%p.log \
          -Djava.rmi.server.hostname=0.0.0.0 \
          -Dcom.sun.management.jmxremote=true \
          -Dcom.sun.management.jmxremote.port=1099 \
          -Dcom.sun.management.jmxremote.rmi.port=1099 \
          -Dcom.sun.management.jmxremote.ssl=false \
          -Dcom.sun.management.jmxremote.authenticate=false \
          -agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n \
          -Dorg.eclipse.jetty.server.Request.maxFormContentSize=4194304 \
          -jar /var/lib/serposcope/serposcope.jar
