# Test MEMO

## how to use
```text
docker-compose -f docker/test/docker-compose.yml -p cantaloupe up --build -d

# all
docker exec test_cantaloupe /bin/sh -c "mvn clean test -Pfreedeps"

# ping
docker exec test_cantaloupe /bin/sh -c "curl 'http://minio-host:9001'"
docker exec test_cantaloupe /bin/sh -c "curl 'http://redis-host:6379'" -> pingとしては不完全

# ps
docker exec -ti test_cantaloupe /bin/sh -c "ps aux |grep minio"
docker exec -ti test_cantaloupe /bin/sh -c "ps aux |grep redis"
```
check  
[actions.yml](..%2F..%2F.github%2Fworkflows%2Factions.yml)  
[README.md#Test](../../README.md)

## for mac
```text
docker pull --platform linux/arm64 openjdk:16.0.2-jdk-buster
or
docker pull --platform linux/arm64/v8 openjdk:16.0.2-jdk-buster
```

### extra
```text
mvn clean test -Pnodeps
mvn clean test -Ponlydeps
mvn clean test -Pfreedeps
```

## issue
- USERの扱い
  - github actionsはdockerfile内でUSERを使うことを推奨していない。
    - [https://docs.github.com/ja/actions/creating-actions/dockerfile-support-for-github-actions#user](https://docs.github.com/ja/actions/creating-actions/dockerfile-support-for-github-actions#user)
  - 一部のテストはユーザ設定をしないとパスできない（らしい）
    - dockerfileには記載と設定があった
      - `# A non-root user is needed for some FilesystemSourceTest tests to work.`
  - resolved
    - dockerでUSERとWORKDIRの設定をしない


