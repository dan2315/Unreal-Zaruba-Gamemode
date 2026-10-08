P=$(sed -n "s/^rcon.password=//p" /data/server.properties | tr -d "\r"); n=0
while IFS= read -r line; do n=$((n+1)); r=$(rcon-cli --port 25575 --password "$P" "$line" 2>&1 | tr -d "\033" | sed "s/\[0m//g" | tr "\n" " "); echo "$n|$r"; done < /tmp/run.txt
