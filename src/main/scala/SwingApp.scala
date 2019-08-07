package com.selmank

import scala.swing.FileChooser.{Result, SelectionMode}
import swing._
import swing.event._
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import com.norbitltd.spoiwo.model.{Row, Sheet}
import com.norbitltd.spoiwo.natures.xlsx.Model2XlsxConversions._
import com.norbitltd.spoiwo.model.enums._
import com.norbitltd.spoiwo.model._
import org.joda.time.LocalDate
import javax.swing.filechooser.FileNameExtensionFilter

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import com.selmank._
import io.circe.Json
import gnieh.diffson.circe._
import scalaj.http.HttpResponse

import scala.swing.GridBagPanel.{Anchor, Fill}

object SwingApp extends SimpleSwingApplication {

  val ui = new GridBagPanel {
    val c = new Constraints
    val shouldFill = true
    if (shouldFill) {
      c.fill = Fill.Horizontal
    }
    var numclicks = 0

    object button extends Button {
      text = Utils.addSpaces("Sitedeki Ürünleri Excel olarak Kaydet",5)
      //opaque = true
      foreground = Colors.BLUE
      var loadedProducts = 0
      var numProducts = 0
      reactions += {
        case ButtonClicked(_) => {
          this.enabled = false
          label.text = "Dosya Hazırlanıyor"
          numProducts = comp.getProductCount()
          progressBar.max = numProducts
          val woos: IndexedSeq[Future[List[Json]]] = comp.getAllProducts2()
          val sequence = Future.sequence(woos)
          val list = woos.map(p => {
            p.onComplete {
              case Success(value) =>
                {
                  loadedProducts += value.length
                  progressLabel.text = s"$loadedProducts/$numProducts"

                  progressBar.value = loadedProducts

                  println(value.length)
                }
              case Failure(exception) => exception.printStackTrace()
            }

            //Await.result(p, scala.concurrent.duration.Duration.Inf)
          })
          sequence.onComplete {
            case Success(value) => {
              val wooList: List[WooJsonObject] = value.flatten.map(p => comp.toWooJsonObject(p.toString).toSeq(0)).toList
              saveAsExcel(wooList)
            }
          }
        }


          def saveAsExcel(list:List[WooJsonObject]) = {
            val values = list.map(p=> List[String](p.sku,p.name,p.price,p.stock_quantity.getOrElse(0).toString))
            val rows = values.map(Row().withCellValues(_))

            val headerStyle = CellStyle(fillPattern = CellFill.Solid, fillForegroundColor = Color.AquaMarine, fillBackgroundColor = Color.AquaMarine, font = Font(bold = true))
            val headerRow = Row(style = headerStyle).withCellValues("STOK KODU", "URUN", "FIYAT", "STOK ADEDI")

            val mergeRows = List(headerRow) ++ rows
            label.text = "Dosya Kaydedildi"

            val firstColumn = Column(index = 0, style = CellStyle(font = Font(bold = true)), autoSized = true)
            val column = Column(autoSized = true)

            val columns = (0 until rows.length).map (p=>Column(index = p, autoSized = true))
            val mergeColumns = List(firstColumn) ++ columns.drop(1)


            val gettingStartedSheet: Sheet = Sheet(name = "")
              .withRows(mergeRows)
              .withColumns(mergeColumns)
            val fileName = s"urunler_${Utils.getDateString()}.xlsx"
            gettingStartedSheet.saveAsXlsx(fileName)
          }
      }
    }


    c.weightx = 0.0
    c.fill = Fill.Horizontal
    c.gridx = 0
    c.gridy = 0
    c.gridwidth = 2
    c.anchor = Anchor.PageStart
    c.insets = new Insets(10, 0, 0, 0) //top padding
    layout(button) = c

    object progressBar extends ProgressBar {
      min = 0
      max = 10
      value = 0
    }
    c.weightx = 0.0
    c.fill = Fill.Horizontal
    c.gridwidth = 2
    c.insets = new Insets(0, 7, 0, 7) //top padding
    c.gridx = 0
    c.gridy = 1
    layout(progressBar) = c

    object label extends Label {
      val prefix = "Hemi Gıda"
      text = prefix

    }

    dLabel = label
    c.weightx = 0.9
    c.fill = Fill.Horizontal
    c.gridx = 0
    c.gridy = 2
    c.gridwidth = 1
    layout(label) = c

    object progressLabel extends Label {
      val prefix = "0/0"
      text = prefix
    }
    c.weightx = 0.1
    c.fill = Fill.Horizontal
    c.gridx = 1
    c.gridy = 2
    c.gridwidth = 1
    layout(progressLabel) = c
  }

  def top = new MainFrame {
    title = "Hemi Gıda"
    //preferredSize = new Dimension(500, 171)
    contents = ui
  }

  def labelString(s: String) = {
    val max = AppProperties.MAX_LABEL_SIZE
    if (s.length > max) s.take(max) + "..."
    else s
  }


  var dLabel: Label = null
  var selectedFile: File = null

}

object AppProperties {
  val MAX_LABEL_SIZE = 25
  val TYPE = "batch"
  val BATCH_CHUNK_SIZE = 20
}

object Utils {
  def addSpaces(str:String, num:Int) = {
    val space = (1 to num) map (p=> " ") mkString("")
    space + str + space
  }
  def getDateString():String = {
    new SimpleDateFormat("YYYYMMdd_HHmmss").format(new Date)
  }
}
