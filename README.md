# Reson 

Zero-configuration REST API for MySQL.
Basically a clone of [PostgREST](https://github.com/begriffs/postgrest), but for MySQL.

*Contributors are welcome!* 

## Read API

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