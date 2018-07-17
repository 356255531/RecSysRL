package com.zhiwei.types.dbtypes

import org.bson.Document
import org.bson.types.ObjectId

trait DBBaseTypeT {
  type InsertElement = Map[String, String]
  type InsertElements = List[InsertElement]
  type InsertValues = List[String]
  type InsertValuesBatch = List[InsertValues]

  type Query = Document
  type Documents = List[Document]
  type DocumentIds = List[ObjectId]
  type MongoDBManyDocuments = java.util.List[Document]
}
