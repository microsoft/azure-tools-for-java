echo "127.0.0.1 $(hostname)" > /etc/hosts # without this spark-tools tests will fail in docker
./BuildDevint -B