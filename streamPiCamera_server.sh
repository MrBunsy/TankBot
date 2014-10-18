#raspivid -w 640 -h 480 -t 999999999 -fps 10 -b 500000 -o - | nc 192.168.1.50 5001

#raspivid -hf -w 1280 -h 1024 -t 999999999 -fps 20 -b 5000000 -o - | nc -l  -p 5001

raspivid -hf -w 800 -h 600 -t 999999999 -fps 20 -b 5000000 -o - | nc -l  -p 5001

#raspivid -hf -w 1680 -h 1050  -t 999999999 -fps 20 -b 15000000 -o - | nc -l  -p 5001



