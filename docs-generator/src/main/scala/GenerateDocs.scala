import java.util

import better.files.File
import com.codacy.plugins.api._
import com.codacy.plugins.api.results.Pattern.{Description, Specification}
import com.codacy.plugins.api.results.{Pattern, Result, Tool}
import com.vladsch.flexmark.ast.{Link, Node}
import com.vladsch.flexmark.ext.tables._
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import org.jsoup.Jsoup
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

object GenerateDocs {

  def main(args: Array[String]): Unit = {
    val options = new MutableDataSet
    options.set(Parser.EXTENSIONS, util.Arrays.asList(TablesExtension.create()))
    val parser = Parser.builder(options).build
    val file = File(args(0)).contentAsString
    val document = parser.parse(file)

    val (descriptionSet, specificationSet) =
      document.getChildIterator.asScala.toList
        .collect { case table: TableBlock => table }
        .flatMap(filterType[TableBody])
        .flatMap(filterType[TableRow])
        .map(tableRowToPattern)
        .map {
          case (ruleName, description) =>
            (Description(Pattern.Id(ruleName),
                         Pattern.Title(ruleName),
                         Option(Pattern.DescriptionText(description)),
                         None,
                         None),
             Specification(Pattern.Id(ruleName), Result.Level.Info, Pattern.Category.CodeStyle, None))
        }
        .foldLeft((Set.empty[Pattern.Description], Set.empty[Pattern.Specification])) {
          case ((descriptionSet, specificationSet), (description, specification)) =>
            (descriptionSet + description, specificationSet + specification)
        }

    val tool = Tool.Specification(Tool.Name("hadolint"), None, specificationSet)
    val patternsJsonContent = Json.prettyPrint(Json.toJson(tool))
    val patternsJsonFile = File("patterns.json")
    patternsJsonFile.overwrite(patternsJsonContent)
    val descriptionsJsonContent = Json.prettyPrint(Json.toJson(descriptionSet))
    val descriptionsJsonFile = File("description.json")
    descriptionsJsonFile.overwrite(descriptionsJsonContent)
    val content = descriptionsJsonFile.contentAsString
  }

  def filterType[A: ClassTag](node: Node): List[A] =
    node.getChildIterator.asScala.toList.collect {
      case a: A => a
    }

  def tableRowToPattern(tableRow: TableRow): (String, String) = {
    tableRow.getChildIterator.asScala.toList match {
      case List(rule: TableCell, description: TableCell) =>
        val ruleName = filterType[Link](rule).head.getText.toString
        val descriptionStr = description.getText.toString
        val parsedStr = Jsoup.parse(descriptionStr).text()
        (ruleName, parsedStr)
    }
  }
}
