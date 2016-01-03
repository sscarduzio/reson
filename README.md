[![ghit.me](https://ghit.me/badge.svg?repo=sscarduzio/reson)](https://ghit.me/repo/sscarduzio/reson)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/d0d150cb90df40db99b2863660c2399b)](https://www.codacy.com/app/scarduzio/reson)
# Reson
Bridging **RE**lational databases to J**SON** based (REST) APIs.

> Reson infers a REST API from any MySQL database schema.
Basically a clone of [PostgREST](https://github.com/begriffs/postgrest), but for MySQL.

## Status of the project
The read API is done, the MySQL protocol JSON serializer works as of MySQL 5.6, but needs some love: for example DATETIME, DATE SQL types do work, but TIME doesn't.
The reason is that - since Finagle MySQL connector is not yet updated to support MySQL 5.6 - I made it work with some workaround. But not entirely.

> __Contributors are welcome!__ 


## Read API
Please refer to the following bullet points to see what is implemented.

* ✓ Get / -> all the tables and fields
* ✓ Get /table_name -> select *
* ✓ "Range" header -> SQL LIMIT x, y
* ✓ select, order, in, like, comparators
*  testing

## Write API

* Unknown schema Json body -> SQL Insert
* Single Object POST Insert 
* Array POST Insert
* Make it work with PATCH
* Deletion Query (using the Read API)

## Authentication

* JWT token validation w/secret
