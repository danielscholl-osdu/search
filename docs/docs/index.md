# Search service
## Introduction

The Search API provides a mechanism for searching indexes.

[Indexer service](https://osdu.pages.opengroup.org/platform/system/indexer-service/) provides a mechanism for indexing documents that contain structured data. Documents and indexes are saved in a separate persistent store optimized for search operations. It can index any number of documents.
Once indexed, you can search an index, organize and present search results via [Search Service API](api.md). It provides rich query capabilities on indexed data.


The [Search Service API](api.md) supports full-text search on string fields, range queries on dates, numeric or string fields, etc., along with geo-spatial search.