package spinoco.fs2.cassandra.builder

import shapeless.labelled._
import shapeless.ops.hlist.Prepend
import shapeless.ops.record.Selector
import shapeless.{::, HList, HNil, Witness}
import spinoco.fs2.cassandra.internal
import spinoco.fs2.cassandra.internal.TableInstance
import spinoco.fs2.cassandra.{CType, KeySpace, Table}

/**
  * Helper to build type safe definition of the table
  */
case class TableBuilder[R <: HList, PK <: HList, CK <: HList, IDX <: HList](
  ks:KeySpace
  , indexes: Seq[IndexEntry]
  , partitionKeys: Seq[String]
  , clusterKeys: Seq[String]
) { self =>

  /** Register supplied name as partitioning key for this table **/
  def partition[K, V](name:Witness.Aux[K])( implicit S: Selector.Aux[R,K,V], P:Prepend[PK,FieldType[K,V] :: HNil])
  :TableBuilder[R, P.Out, CK, IDX] = TableBuilder(ks, indexes,  partitionKeys :+ internal.keyOf(name), clusterKeys)

  /** Register supplied name as clustering key for this table **/
  def cluster[K,V](name:Witness.Aux[K])( implicit S: Selector.Aux[R,K,V], P:Prepend[CK,FieldType[K,V] :: HNil])
  :TableBuilder[R,  PK, P.Out, IDX] = TableBuilder(ks, indexes, partitionKeys, clusterKeys :+  internal.keyOf(name) )

  /** registers given `V` as column of this table with name `name` **/
  def column[K,V](name:Witness.Aux[K])(implicit ev:CType[V])
  : TableBuilder[FieldType[K,V] :: R, PK, CK, IDX] = TableBuilder(ks, indexes, partitionKeys, clusterKeys)

  def indexBy[K,V](column:Witness.Aux[K])( implicit S: Selector.Aux[R,K,V])
  :TableBuilder[R,PK,CK,FieldType[K,V] :: IDX] =
    indexBy(column,internal.keyOf(column)+"_idx")

  def indexBy[K,V](column:Witness.Aux[K], name:String, clazz:Option[String] = None, options:Map[String, String] = Map.empty)(
    implicit S: Selector.Aux[R,K,V]
  ): TableBuilder[R,PK,CK,FieldType[K,V] :: IDX] = {
    TableBuilder(ks, IndexEntry(name,internal.keyOf(column),clazz,options) +: indexes, partitionKeys, clusterKeys )
  }

  def build(name:String, options:Map[String,String] = Map.empty)(
    implicit T:TableInstance[R,PK,CK, IDX]
  ): Table[R, PK, CK, IDX] = T.table(ks,name,options, self.indexes, self.partitionKeys, self.clusterKeys)

}
