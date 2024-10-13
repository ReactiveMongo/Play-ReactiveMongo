#! /usr/bin/env bash

set -e

SCRIPT_DIR=`dirname $0 | sed -e "s|^\./|$PWD/|"`

# OpenSSL
SSL_MAJOR="1.0.0"
SSL_SUFFIX="10"
SSL_RELEASE="1.0.2"
SSL_FULL_RELEASE="1.0.2u"

if [ ! -L "$HOME/ssl/lib/libssl.so.$SSL_MAJOR" ] && [ ! -f "$HOME/ssl/lib/libcrypto.so.$SSL_MAJOR" ]; then
  echo "[INFO] Building OpenSSL $SSL_MAJOR ..."

  cd /tmp
  curl -L -s -o - "https://www.openssl.org/source/old/$SSL_RELEASE/openssl-$SSL_FULL_RELEASE.tar.gz" | tar -xzf -
  cd openssl-$SSL_FULL_RELEASE
  rm -rf "$HOME/ssl" && mkdir "$HOME/ssl"
  ./config -shared enable-ssl2 --prefix="$HOME/ssl" > /dev/null
  make depend > /dev/null
  make install > /dev/null

  ln -s "$HOME/ssl/lib/libssl.so.$SSL_MAJOR" "$HOME/ssl/lib/libssl.so.$SSL_SUFFIX"
  ln -s "$HOME/ssl/lib/libcrypto.so.$SSL_MAJOR" "$HOME/ssl/lib/libcrypto.so.$SSL_SUFFIX"
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
