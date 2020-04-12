#! /usr/bin/env bash

set -e

SCRIPT_DIR=`dirname $0 | sed -e "s|^\./|$PWD/|"`

# OpenSSL
if [ ! -L "$HOME/ssl/lib/libssl.so.1.0.0" ] && [ ! -f "$HOME/ssl/lib/libcrypto.so.1.0.0" ]; then
  echo "[INFO] Building OpenSSL"

  cd /tmp
  curl -s -o - https://www.openssl.org/source/old/1.0.1/openssl-1.0.1u.tar.gz | tar -xzf -
  cd openssl-1.0.1u
  rm -rf "$HOME/ssl" && mkdir "$HOME/ssl"
  ./config -shared enable-ssl2 --prefix="$HOME/ssl" > /dev/null
  make depend > /dev/null
  make install > /dev/null

  ln -s "$HOME/ssl/lib/libssl.so.1.0.0" "$HOME/ssl/lib/libssl.so.10"
  ln -s "$HOME/ssl/lib/libcrypto.so.1.0.0" "$HOME/ssl/lib/libcrypto.so.10"
fi

export LD_LIBRARY_PATH="$HOME/ssl/lib:$LD_LIBRARY_PATH"

# Build MongoDB
MONGO_MINOR="3.6.6"

# Build MongoDB
echo "[INFO] Building MongoDB ${MONGO_MINOR} ..."

cd "$HOME"

MONGO_ARCH="x86_64-amazon"
MONGO_HOME="$HOME/mongodb-linux-$MONGO_ARCH-$MONGO_MINOR"

if [ ! -x "$MONGO_HOME/bin/mongod" ]; then
    if [ -d "$MONGO_HOME" ]; then
      rm -rf "$MONGO_HOME"
    fi

    curl -s -o - "https://fastdl.mongodb.org/linux/mongodb-linux-$MONGO_ARCH-$MONGO_MINOR.tgz" | tar -xzf -
    chmod u+x "$MONGO_HOME/bin/mongod"
fi

echo "[INFO] MongoDB available at $MONGO_HOME"

PATH="$MONGO_HOME/bin:$PATH"

mkdir /tmp/mongodb

# MongoDB setup
MAX_CON=`ulimit -n`

if [ $MAX_CON -gt 1024 ]; then
    MAX_CON=`expr $MAX_CON - 1024`
fi

echo "[INFO] Max connection: $MAX_CON"

cp "$SCRIPT_DIR/mongod3.conf" /tmp/mongod.conf

echo "  maxIncomingConnections: $MAX_CON" >> /tmp/mongod.conf

echo "# MongoDB Configuration:"
cat /tmp/mongod.conf

cat > /tmp/validate-env.sh <<EOF
PATH="$PATH"
LD_LIBRARY_PATH="$LD_LIBRARY_PATH"
EOF
