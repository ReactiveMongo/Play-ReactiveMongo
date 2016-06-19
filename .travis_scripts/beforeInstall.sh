#! /bin/bash

SCRIPT_DIR=`dirname $0 | sed -e "s|^\./|$PWD/|"`

# Install MongoDB
if [ ! -x "$HOME/mongodb-linux-x86_64-amazon-3.2.4/bin/mongod" ]; then
    curl -s -o /tmp/mongodb.tgz https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-amazon-3.2.4.tgz
    cd "$HOME" && rm -rf mongodb-linux-x86_64-amazon-3.2.4
    tar -xzf /tmp/mongodb.tgz && rm -f /tmp/mongodb.tgz
    chmod u+x mongodb-linux-x86_64-amazon-3.2.4/bin/mongod
fi

# OpenSSL
if [ ! -L "$HOME/ssl/lib/libssl.so.1.0.0" ]; then
  cd /tmp
  curl -s -o - https://www.openssl.org/source/openssl-1.0.1s.tar.gz | tar -xzf -
  cd openssl-1.0.1s
  rm -rf "$HOME/ssl" && mkdir "$HOME/ssl"
  ./config -shared enable-ssl2 --prefix="$HOME/ssl" > /dev/null
  make depend > /dev/null
  make install > /dev/null
else
  #find "$HOME/ssl" -ls
  rm -f "$HOME/ssl/lib/libssl.so.1.0.0" "libcrypto.so.1.0.0"
fi

ln -s "$HOME/ssl/lib/libssl.so.1.0.0" "$HOME/ssl/lib/libssl.so.10"
ln -s "$HOME/ssl/lib/libcrypto.so.1.0.0" "$HOME/ssl/lib/libcrypto.so.10"

export LD_LIBRARY_PATH="$HOME/ssl/lib:$LD_LIBRARY_PATH"

# MongoDB configuration
export PATH="$HOME/mongodb-linux-x86_64-amazon-3.2.4/bin:$PATH"
MONGO_CONF="$SCRIPT_DIR/mongod3.conf"

mkdir /tmp/mongodb
cp "$MONGO_CONF" /tmp/mongod.conf

MAX_CON=`ulimit -n`

if [ $MAX_CON -gt 1024 ]; then
    MAX_CON=`expr $MAX_CON - 1024`
fi

echo "  maxIncomingConnections: $MAX_CON" >> /tmp/mongod.conf

echo "# Configuration:"
cat /tmp/mongod.conf

# MongoDB startup

cat > /tmp/validate-env.sh <<EOF
PATH="$PATH"
LD_LIBRARY_PATH="$LD_LIBRARY_PATH"
EOF

mongod -f /tmp/mongod.conf --fork
