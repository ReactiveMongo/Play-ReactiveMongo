#! /bin/bash

sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10

echo "deb http://repo.mongodb.org/apt/ubuntu "$(lsb_release -sc)"/mongodb-org/3.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-3.0.list

apt-get update
apt-get install mongodb-org-server
apt-get install mongodb-org-shell
service mongod stop

# WiredTiger
mkdir /tmp/mongo3wt
chown -R mongodb:mongodb /tmp/mongo3wt
chmod -R ug+r /tmp/mongo3wt
chmod -R u+w /tmp/mongo3wt

sed -e 's|dbpath=/var/lib/mongodb|dbpath=/tmp/mongo3wt|' < /etc/mongod.conf > /tmp/mongod.conf && (cat /tmp/mongod.conf > /etc/mongod.conf)

service mongod start
