Redis Transport for ElasticSearch
==================================

[![Build Status](https://travis-ci.org/kzwang/elasticsearch-transport-redis.png?branch=master)](https://travis-ci.org/kzwang/elasticsearch-transport-redis)

The Redis transport plugin allows to use the REST interface over Redis (though with limitations).
Modified from [Memcached Transport for ElasticSearch](https://github.com/elasticsearch/elasticsearch-transport-memcached/)

In order to install the plugin, simply run: `bin/plugin -install com.github.kzwang/elasticsearch-transport-redis/2.0.0.RC2`.

|      Redis Plugin           | elasticsearch         | Release date |
|-----------------------------|-----------------------|:------------:|
| 2.0.0-SNAPSHOT (master)     | 1.0.0.RC2             |              |
| 2.0.0.RC2                   | 1.0.0.RC2             | 2014-02-07   |
| 2.0.0.RC1                   | 1.0.0.RC2             | 2014-02-04   |
| 1.1.0-SNAPSHOT (1.x)        | 0.90.11               |              |
| 1.0.0                       | 0.90.10               | 2014-02-03   |


## Supported Commands
### GET
Mapped to REST **GET** request, key is the URI (with parameters), returns same JSON result as REST

### SET
Mapped to REST **POST** request, key is the URI (with parameters), value is REST body, by default returns `OK` for success and `Error` for fail, can be configured to return same JSON response as REST
Can optionally add `PUT` keywords before REST body to use **PUT** request. e.g. `PUT{"test":true}`

### DELETE
Mapped to REST **DELETE** request, key is the URI (with parameters), by default returns 1 for success and 0 for fail, can be configured to return same JSON response as REST

### EXISTS
Mapped to REST **HEAD** request, key is the URI (with parameters), returns 1 for exists and 0 for not exists or fail

### QUIT
Disconnect the client


## Settings
|  Setting               |   Description
|------------------------|------------------------------------------------------------------------------------------------------------------------------------------------
| redis.enabled          | set to `false` to disable redis transport
| redis.port             | A bind port range, default to `6379-6479`
| redis.response.set     | `standard` or `json`, controls response for `SET` command, `standard` will return `OK` or `Error`, `json` will return same JSON response as REST, default to `standard`, can be changed dynamically
| redis.response.del     | `standard` or `json`, controls response for `DEL` command, `standard` will return `1` or `0`, `json` will return same JSON response as REST, default to `standard`, can be changed dynamically