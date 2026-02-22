#!/bin/bash
echo "Content-Type: text/html"
echo ""

# sleep 10
# echo "done 10s"

if [ "$REQUEST_METHOD" = "POST" ]; then
    read -n "$CONTENT_LENGTH" POST_DATA 2>/dev/null
    if [ -z "$POST_DATA" ]; then
        echo "<p>No POST data received.</p>"
    else
        echo "<p>POST Data received:</p>"
        echo "<pre>$POST_DATA</pre>"
    fi
else
    echo "<p>GET Request received.</p>"
fi


# echo "Content-Type: text/html"
# echo ""

# USERNAME="laidi"

# cat www/dashboard.html | sed "s/{{username}}/$USERNAME/g"
