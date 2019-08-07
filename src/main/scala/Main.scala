package com.selmank

import java.nio.charset.StandardCharsets

import scala.xml.XML
import scala.xml.Elem
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date

import scala.xml.{Elem, XML}
import io.circe.{Json, Printer, parser}
import io.circe.optics.JsonPath.root
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.jawn.decode
import scalaj.http.{Http, HttpOptions, HttpResponse, Token}
import com.selmank._
import comp._
import gnieh.diffson.circe._

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.concurrent.duration._

import com.norbitltd.spoiwo.model.{Row, Sheet}
import com.norbitltd.spoiwo.natures.xlsx.Model2XlsxConversions._


object Main {
  println("Hello, World!")




}

object comp {
  val categoryNumGenel = 99

  def sendItemJson(json: Json): HttpResponse[String] = {
    val post_url = ""
    val post = Http(post_url)
      .postData(json.toString())
      .header("content-type", "application/json")
      .header("Charset", "UTF-8")
      .option(HttpOptions.connTimeout(10000))
      .option(HttpOptions.readTimeout(50000))
      .auth(Woo.CONSUMER_KEY, Woo.CONSUMER_SECRET).asString
    post
  }

  def sendRecordToWeb(item: RECORD): String = {
    if (!isRecordExistOnWebBySku(item)) {
      println("Sending: " + item.STOK_ADI)
      sendItemRecord(item)
      "Sending: " + item.STOK_ADI
    }
    else {
      updateItemRecordBySku(item)
      println("Updating: " + item.STOK_ADI)
      "Updating: " + item.STOK_ADI
    }
  }

  def sendRecordsToWeb(item: List[RECORD]) = {
    val itemOnWeb = item.filter(isRecordExistOnWebBySku(_))
    val newItems = item diff itemOnWeb
    newItems.map(sendRecordToWeb(_))
    println("Updating: " + item(0).STOK_ADI)
    batchUpdateRecords(itemOnWeb)
    "Updating: " + item(0).STOK_ADI
  }

  def deleteRecordOnWeb(item: RECORD): String = {
    if (isRecordExistOnWebBySku(item)) {
      println("Deleting: " + item.STOK_ADI)
      deleteItemRecordBySku(item)
      "Deleting: " + item.STOK_ADI
    }
    else {
      println("Item is not existed already: " + item.STOK_ADI)
      "ITEM HAS NOT EXISTED ON WEB ALREADY: " + item.STOK_ADI
    }
  }

  private def sendItemRecord(item: RECORD): HttpResponse[String] = {
    val json = recordNewToJSON(item)
    val post_url = ""
    val post = Http(post_url)
      .postData(json.toString())
      .header("content-type", "application/json")
      .header("Charset", "UTF-8")
      .option(HttpOptions.connTimeout(10000))
      .option(HttpOptions.readTimeout(50000))
      .auth(Woo.CONSUMER_KEY, Woo.CONSUMER_SECRET).asString
    post
  }

  private def updateItemRecordBySku(item: RECORD): HttpResponse[String] = {
    val json: Json = recordExistedToJSON(item)
    val id: Int = getWooID(item)
    val post_url = "" + id.toString
    val jsonStringWithoutNulls: String = removeNullsFromJson(json)
    val post = Http(post_url)
      .postData(jsonStringWithoutNulls)
      .header("content-type", "application/json")
      .header("Charset", "UTF-8")
      .option(HttpOptions.connTimeout(10000))
      .option(HttpOptions.readTimeout(50000))
      .auth(Woo.CONSUMER_KEY, Woo.CONSUMER_SECRET).asString
    post
  }


  private def batchUpdateRecords(item: List[RECORD]) = {
    val woos: List[WooItem] = item.map(recordExistedToBatchWoo(_))
    val map: Map[WooItem, RECORD] = (woos zip item) toMap
    val futures = woos map (p => getWooID(map(p)))
    //val futureSeq:Future[List[Int]] = Future.sequence(futures)
    //val t:List[Int]= Await.result(futureSeq, 1000 seconds)
    val woosWithIds = (woos zip futures).toMap
    val list = woos.map(p => p.copy(id = Some(woosWithIds(p).toString)))
    val updateBatch = new UpdateJsonObject(list)
    val json: Json = updateBatch.asJson
    val post_url = ""
    val jsonStringWithoutNulls: String = removeNullsFromJson(json)
    val post = Http(post_url)
      .postData(jsonStringWithoutNulls)
      .header("content-type", "application/json")
      .header("Charset", "UTF-8")
      .option(HttpOptions.connTimeout(10000))
      .option(HttpOptions.readTimeout(50000))
      .auth(Woo.CONSUMER_KEY, Woo.CONSUMER_SECRET).asString
    post
  }

  private def deleteItemRecordBySku(item: RECORD): HttpResponse[String] = {
    val id: Int = getWooID(item)
    val post_url = "" + id.toString + "?force=true"
    val post = Http(post_url)
      .method("DELETE")
      .option(HttpOptions.connTimeout(10000))
      .option(HttpOptions.readTimeout(50000))
      .auth(Woo.CONSUMER_KEY, Woo.CONSUMER_SECRET).asString
    post
  }

  def isRecordBox(item: RECORD): Boolean = {
    item.BIRIM2 == "KT"
  }

  def getWooID(item: RECORD): Int = {
    val response = getItemRecordBySku(item.STOK_KODU)
    val itemsListJson: List[Json] = rootJsonToList(responseToJSON(response))
    val list = itemsListJson.map(p => toWooJsonObject(p.toString()))
    val woo = list(0).toSeq(0)
    woo.id toInt
  }

  def getItemRecordBySku(sku: String): HttpResponse[String] = {
    val get_url = "" + sku
    val get = Http(get_url)
      .header("content-type", "application/json")
      .header("Charset", "UTF-8")
      .option(HttpOptions.connTimeout(10000))
      .option(HttpOptions.readTimeout(50000))
      .auth(Woo.CONSUMER_KEY, Woo.CONSUMER_SECRET).asString
    get
  }

  def getOrders(): HttpResponse[String] = {
    val orders = Http("").auth(Woo.CONSUMER_KEY, Woo.CONSUMER_SECRET).asString
    orders
  }

  def getAllProducts() = {
    val totalPages = getProductPageCount()
    val futures: IndexedSeq[Future[List[Json]]] = (1 to totalPages) map (i => {
      Future {
        getProductsAsJsonList(i)
      }
    })
    val futureSeq: Future[IndexedSeq[List[Json]]] = Future.sequence(futures)
    val t: IndexedSeq[List[Json]] = Await.result(futureSeq, 1000 seconds)
    val merge: List[Json] = t.reduceLeft(_ ++ _)
    val woos: List[WooJsonObject] = merge.map(p => toWooJsonObject(p.toString).toSeq(0))
    woos
  }

  def getAllProducts2() = {
    val totalPages = getProductPageCount()
    val futures: IndexedSeq[Future[List[Json]]] = (1 to totalPages) map (i => {
      Future {
        getProductsAsJsonList(i)
      }
    })
    futures
  }

  def getProductsAsJsonList(page: Int = 1, per_page: Int = 100): List[Json] = {
    val products = Http(s"")
      .header("content-type", "application/json")
      .header("Charset", "UTF-8")
      .option(HttpOptions.connTimeout(10000))
      .option(HttpOptions.readTimeout(50000))
      .auth(Woo.CONSUMER_KEY, Woo.CONSUMER_SECRET).asString
    val json = responseToJSON(products)
    val list = rootJsonToList(json)
    list
  }

  def getProductCount(): Int = {
    val firstProduct = Http("")
      .header("content-type", "application/json")
      .header("Charset", "UTF-8")
      .option(HttpOptions.connTimeout(10000))
      .option(HttpOptions.readTimeout(50000))
      .auth(Woo.CONSUMER_KEY, Woo.CONSUMER_SECRET).asString
    val header = firstProduct.headers.filter(_._1 == "X-WP-Total")
    toInt(header.head._2(0)).getOrElse(0)
  }

  def getProductPageCount(perPage: Int = 100): Int = {
    val per_page = perPage
    val total = getProductCount()
    val totalPage = (total / per_page) + 1
    totalPage
  }

  def toInt(s: String): Option[Int] = {
    try {
      Some(s.toInt)
    } catch {
      case e: Exception => None
    }
  }

  def getProductByID(id: Int): HttpResponse[String] = {
    val product = Http("" + id.toString).auth(Woo.CONSUMER_KEY, Woo.CONSUMER_SECRET).asString
    product

  }


  def isRecordExistOnWebBySku(item: RECORD): Boolean = {
    println("Controlling: " + item.STOK_ADI)
    getItemRecordBySku(item.STOK_KODU).body != "[]"
  }


  def recordNewToJSON(item: RECORD): Json = {
    val stockName = item.STOK_ADI trim
    val status = Woo.STATUS_DRAFT
    val price = if (!isRecordBox(item)) item.SATISFIYATI1 toString else item.BIRIM2SATISFIYATI toString
    val manageStock = Woo.STOCK_MANAGEMENT_TRUE
    val quantity = if (item.BAKIYE.toInt < 0) 0 else item.BAKIYE.toInt
    val sku = item.STOK_KODU
    val imgDefaultId = 1172
    val meta_data = new Meta_data3("date_1", item.FIYAT_DEGISIM_TARIHI)
    val woo = new WooItem(None, Some(stockName), Some("simple"), Some(status), price, Some(stockName), Some(stockName), Some(sku), Some(manageStock), quantity, Some(List(new Categories(id = categoryNumGenel))), Some(List(new Images(id = imgDefaultId))), Some(List(meta_data)))
    woo.asJson
  }

  def recordExistedToJSON(item: RECORD): Json = {
    val price = if (!isRecordBox(item)) item.SATISFIYATI1 toString else item.BIRIM2SATISFIYATI toString
    val quantity = if (item.BAKIYE.toInt < 0) 0 else item.BAKIYE.toInt
    val fiyatDegisimTarihi = new Meta_data3("date_1", item.FIYAT_DEGISIM_TARIHI)
    val uretimYeri = new Meta_data3("text_2", item.URETIM_YERI_NO)
    val yerliUretim = new Meta_data3("checkbox_3", if (item.URETIM_YERI_NO == "Türkiye") "1" else "0")
    val metaDataList: List[Meta_data3] = List[Meta_data3](fiyatDegisimTarihi, uretimYeri, yerliUretim)
    val woo = new WooItem(None, None, None, None, price, None, None, None, None, quantity, None, None, Some(metaDataList))
    woo.asJson
  }

  def recordExistedToBatchWoo(item: RECORD): WooItem = {
    val price = if (!isRecordBox(item)) item.SATISFIYATI1 toString else item.BIRIM2SATISFIYATI toString
    val quantity = if (item.BAKIYE.toInt < 0) 0 else item.BAKIYE.toInt
    val fiyatDegisimTarihi = new Meta_data3("date_1", item.FIYAT_DEGISIM_TARIHI)
    val uretimYeri = new Meta_data3("text_2", item.URETIM_YERI_NO)
    val yerliUretim = new Meta_data3("checkbox_3", if (item.URETIM_YERI_NO == "Türkiye") "1" else "0")
    val metaDataList: List[Meta_data3] = List[Meta_data3](fiyatDegisimTarihi, uretimYeri, yerliUretim)
    val woo = new WooItem(None, None, None, None, price, None, None, None, None, quantity, None, None, Some(metaDataList))
    //woo.asJson
    woo
  }

  def responseToJSON(response: HttpResponse[String]): Json = {
    val bytes: Array[Byte] = response.body.getBytes(StandardCharsets.UTF_8)
    val responseBody: String = response.body
    val json: Json = parse(responseBody).getOrElse(Json.Null)
    json
  }

  def rootJsonToList(rootJson: Json): List[Json] = {
    val items = root.each.json
    val itemListJSON: List[Json] = items.getAll(rootJson)
    itemListJSON
  }


  /**
    * toWooJsonObject(p.toString).toSeq(0)
    * @param s
    * @return
    */
  def toWooJsonObject(s: String) = {
    decode[WooJsonObject](s)
  }

  def toWooItem(woo: WooJsonObject): WooItem = {
    val item = new WooItem(None, None, None, None, woo.price, None, None, Some(woo.sku), None, woo.stock_quantity.getOrElse(0), None, None)
    item
  }

  def removeNullsFromJson(json: Json): String = {
    json.pretty(Printer.noSpaces.copy(dropNullValues = true))
  }
  val jsonstr = ""
  val jsonstr2 = ""
  val jsonstr3 = ""

   val default_img = ""

}


class ProStoreRecordList {
  var recordList: List[RECORD] = List[RECORD]()

  def this(xmlPath: String) = {
    this()
    this.recordList = xmlToRecords(xmlPath)
  }

  def this(list: List[RECORD]) = {
    this()
    this.recordList = list
  }


  def xmlToRecords(path: String): List[RECORD] = {
    val xml: Elem = XML.load(path)
    val parsedAddress: LIST = scalaxb.fromXML[LIST](xml)
    val recordList: List[RECORD] = parsedAddress.RECORD.toList
    recordList
  }

  def recordInStock: List[RECORD] = {
    val recordInStock: List[RECORD] = recordList.filter(p => p.BAKIYE > 0)
    recordInStock
  }

  def recordBoxesInStock: List[RECORD] = {
    recordInStock.filter(p => p.BIRIM2 == "KT")
  }

  def recordBoxesInStock2WithPrices: List[RECORD] = {
    recordBoxesInStock.filter(p => p.BIRIM2SATISFIYATI.length > 0)
  }

  def recordRetailInStock: List[RECORD] = recordInStock diff recordBoxesInStock2WithPrices
}