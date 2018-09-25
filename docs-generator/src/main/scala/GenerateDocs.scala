import java.util

import better.files.File
import CreateMarkdowns.moveMarkdownFiles
import com.codacy.plugins.api._
import com.codacy.plugins.api.results.Pattern.{Category, Description, Specification}
import com.codacy.plugins.api.results.Result.Level
import com.codacy.plugins.api.results.{Pattern, Result, Tool}
import com.vladsch.flexmark.ast.{Link, Node}
import com.vladsch.flexmark.ext.tables._
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import org.jsoup.Jsoup
import play.api.libs.json.Json
import cats.implicits._
import scala.collection.JavaConverters._
import scala.reflect.ClassTag

object GenerateDocs {
  def main(args: Array[String]): Unit = {
    val file = File(args(0)).contentAsString
    val outputDir = args(2)
    val markdownInputDir = args(3)
    val hadolintRules = parseRulesFile(args(1))
    val (descriptionSet, specificationSet) = parseMarkdownTable(file, hadolintRules, outputDir)
    writeFiles(specificationSet, descriptionSet, outputDir)
    moveMarkdownFiles(specificationSet, markdownInputDir, outputDir)
  }

  def parseRulesFile(filepath: String): Map[String, Specification] = {
    val file = File(filepath)
    val lines = file.lines.dropWhile(_.contains("--  RULES  --"))
    val codeLines = lines.filter(x => (x.contains("code =") || x.contains("severity =")))
    val parsedCodeLines: List[String] = codeLines.map(line =>
      line.replaceAll("code =", "").replaceAll("severity =", "").replaceAll("\"", "").trim)(collection.breakOut)

    (for {
      List(ruleName, level) <- parsedCodeLines.grouped(2).toList
    } yield (ruleName, Specification(Pattern.Id(ruleName), parseRuleLevel(level), parseRuleCategory(level), None)))(
      collection.breakOut)
  }

  def parseRuleLevel(level: String): Level = {
    level match {
      case "WarningC" => Result.Level.Warn
      case "ErrorC"   => Result.Level.Err
      case _          => Result.Level.Info
    }
  }

  def parseRuleCategory(level: String): Category = {
    level match {
      case "InfoC" => Pattern.Category.CodeStyle
      case _       => Pattern.Category.ErrorProne
    }
  }

  def parseMarkdownTable(file: String, hadolintRules: Map[String, Specification], dir: String): (Set[Description], Set[Specification]) = {
    val options = new MutableDataSet
    options.set(Parser.EXTENSIONS, util.Arrays.asList(TablesExtension.create()))
    val parser = Parser.builder(options).build
    val document = parser.parse(file)

    document.getChildIterator.asScala.toList.collect { case table: TableBlock => table }
        .flatMap(filterType[TableBody])
        .flatMap(filterType[TableRow])
        .map(tableRowToPattern)
        .map {
          case (ruleName, description) =>
            (Set(
               Description(Pattern.Id(ruleName),
                           Pattern.Title(ruleName),
                           Option(Pattern.DescriptionText(description)),
                           None,
                           None)),
             Set(
               hadolintRules.getOrElse(
                 ruleName,
                 Specification(Pattern.Id(ruleName), Result.Level.Info, Pattern.Category.CodeStyle, None))))
        }
        .combineAll
  }

  def writeFiles(specificationSet: Set[Specification], descriptionSet: Set[Description], dir: String): Unit = {
    val tool = Tool.Specification(Tool.Name("hadolint"), None, specificationSet)
    val patternsJsonContent = Json.prettyPrint(Json.toJson(tool))
    val patternsJsonFile = File(s"$dir/patterns.json")
    patternsJsonFile.overwrite(patternsJsonContent)
    val descriptionsJsonContent = Json.prettyPrint(Json.toJson(descriptionSet))
    val descriptionsJsonFile = File(s"$dir/description/description.json")
    descriptionsJsonFile.overwrite(descriptionsJsonContent)
  }

  def filterType[A: ClassTag](node: Node): List[A] =
    node.getChildIterator.asScala.toList.collect {
      case a: A => a
    }

  def tableRowToPattern(tableRow: TableRow): (String, String) = {
    tableRow.getChildIterator.asScala.toList match {
      case List(rule: TableCell, description: TableCell) =>
        val ruleName = filterType[Link](rule).headOption
          .fold[String](throw new Exception("Failed parsing tableRow to Pattern"))(_.getText.toString)
        val descriptionStr = description.getText.toString
        val parsedStr = Jsoup.parse(descriptionStr).text()
        (ruleName, parsedStr)
    }
  }
}
