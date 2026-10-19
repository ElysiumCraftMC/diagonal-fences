#!/bin/bash
# usage: srv.sh <dir> <log> <cmdfile>  -- start server, wait for Done, run commands, save, stop
dir=$1; log=$2; cmds=$3; fifo=/tmp/srvin.$$
# needs java 17 on PATH
rm -f $fifo; mkfifo $fifo
( sleep 3000 > $fifo ) & holder=$!
( cd $dir && java -Xmx1G -jar server.jar nogui < $fifo > $log 2>&1; echo "EXIT $?" >> $log ) &
for i in $(seq 1 90); do grep -qE "Done \(|EXIT" $log && break; sleep 2; done
if [ -n "$cmds" ]; then while IFS= read -r l; do grep -q EXIT $log && break; [ -n "$l" ] && echo "$l" > $fifo; sleep 0.35; done < $cmds; fi
sleep 1; if ! grep -q EXIT $log; then echo "save-all flush" > $fifo; sleep 3; echo stop > $fifo; fi
for i in $(seq 1 60); do grep -q EXIT $log && break; sleep 2; done
kill $holder 2>/dev/null; rm -f $fifo
