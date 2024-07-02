# Test MEMO

## how to use
```text
docker-compose -f docker/test/docker-compose.yml -p cantaloupe up --build -d
docker exec test_cantaloupe /bin/sh -c "mvn clean test -Pfreedeps"

# ping
docker exec test_cantaloupe /bin/sh -c "curl 'http://minio-host:9001'"
docker exec test_cantaloupe /bin/sh -c "curl 'http://redis-host:6379'" -> pingとしては不完全
```
check [ci.yml](..%2F..%2F.github%2Fworkflows%2Fci.yml)

### extra
```text
mvn clean test -Pnodeps
mvn clean test -Pfreedeps

docker exec test_cantaloupe /bin/sh -c "mvn clean test -Dtest=edu.illinois.library.cantaloupe.cache.S3CacheTest"
docker exec test_cantaloupe /bin/sh -c "mvn clean test -Dtest=edu.illinois.library.cantaloupe.cache.RedisCacheTest"

# exec minio, redis local
docker exec -d test_cantaloupe /bin/sh -c "./minio server --console-address ":9001" /s3"
docker exec -d test_cantaloupe /bin/sh -c "redis-server /redis.conf"
# exec minio, redis ci
docker exec -d cantaloupe_test_1 /bin/sh -c "./minio server --console-address ":9001" /s3"
docker exec -d cantaloupe_test_1 /bin/sh -c "redis-server /redis.conf"

docker exec -ti test_cantaloupe /bin/sh -c "ps aux |grep minio"
docker exec -ti test_cantaloupe /bin/sh -c "ps aux |grep redis"

curl 'http://minio-host:9001'
curl 'http://redis-host:6379'

```

## issue
- resolved USERの扱い
  - github actionsはdockerfile内でUSERを使うことを推奨していない。
    - [https://docs.github.com/ja/actions/creating-actions/dockerfile-support-for-github-actions#user](https://docs.github.com/ja/actions/creating-actions/dockerfile-support-for-github-actions#user)
  - 一部のテストはユーザ設定をしないとパスできない（らしい）
    - dockerfileには記載と設定があった
      - `# A non-root user is needed for some FilesystemSourceTest tests to work.`

## memo
```text
cd /home/cantaloupe/src/test/java/edu/illinois/library/cantaloupe
rm -fr {async,auth,cache,config,http,image,operation,processor,resource,script,status,util}
rm -fr {async,auth,cache,config,http}
rm -fr {source/stream,source/Az*,source/F*,source/H*,source/J*,source/L*,source/O*,source/P*}

/bin/sh -c sudo ./minio server --console-address :9001 /home/cantaloupe/s3


cd /home/cantaloupe

docker run --net host cantaloupe/tests /bin/sh -c "mvn clean test"

docker exec test_cantaloupe /bin/sh -c "mvn clean test -Dtest=edu.illinois.library.cantaloupe.cache.S3CacheTest"
docker exec test_cantaloupe /bin/sh -c "mvn clean test -Dtest=edu.illinois.library.cantaloupe.cache.RedisCacheTest"
docker run test-test /bin/sh -c "mvn clean test -Dtest=edu.illinois.library.cantaloupe.cache.S3CacheTest"
docker run test-test /bin/sh -c "mvn clean test -Dtest=edu.illinois.library.cantaloupe.cache.RedisCacheTest"
mvn clean test -Dtest=edu.illinois.library.cantaloupe.cache.S3CacheTest
mvn clean test -Dtest=edu.illinois.library.cantaloupe.cache.RedisCacheTest
mvn clean test -Pnodeps
mvn clean test -Pfreedeps

docker cp src 00:/home/cantaloupe/
docker cp docker/test/image_files/minio_config.json  minio:s3/.minio.sys/config/config.json

docker cp [OPTIONS] SRC_PATH|- CONTAINER:DEST_PATH

curl 'http://localhost:7229'

curl 'http://localhost:9000'
curl 'http://localhost:9001'

./minio server /data

sed -i 's/172.30.0.5/127.0.0.1/' test.properties
sed -i 's/localhost/127.0.0.1/' test.properties
sed -i 's/7230/6379/' test.properties

redis-server /home/cantaloupe/redis.conf

```
