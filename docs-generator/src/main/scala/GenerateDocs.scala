import java.util

import CreateMarkdowns.moveMarkdownFiles
import better.files.File
import cats.implicits._
import com.codacy.plugins.api._
import com.codacy.plugins.api.results.Pattern.{Category, Description, Specification, Subcategory}
import com.codacy.plugins.api.results.Result.Level
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

  val defaultPatterns = List("DL3000",
                             "DL3001",
                             "DL3002",
                             "DL3003",
                             "DL3004",
                             "DL3005",
                             "DL3006",
                             "DL3007",
                             "DL3008",
                             "DL3009",
                             "DL3010",
                             "DL3011",
                             "DL3012",
                             "DL3013",
                             "DL3014",
                             "DL3015",
                             "DL3020",
                             "DL3021",
                             "DL3022",
                             "DL3023",
                             "DL3024",
                             "DL4000",
                             "DL4001",
                             "DL4003",
                             "DL4004",
                             "DL4005",
                             "SC1000",
                             "SC1001",
                             "SC1007",
                             "SC1010",
                             "SC1018",
                             "SC1035",
                             "SC1045",
                             "SC1065",
                             "SC1066",
                             "SC1068",
                             "SC1077",
                             "SC1078",
                             "SC1079",
                             "SC1081",
                             "SC1083",
                             "SC1086",
                             "SC1087",
                             "SC1095",
                             "SC1097",
                             "SC1098",
                             "SC1099",
                             "SC2002",
                             "SC2015",
                             "SC2026",
                             "SC2028",
                             "SC2035",
                             "SC2046",
                             "SC2086",
                             "SC2140",
                             "SC2154",
                             "SC2164")

  def main(args: Array[String]): Unit = {
    val tmpDir = args(0)
    val outputDir = args(1)
    val repoDir = tmpDir + "/hadolintRepo"
    val hadolintRules = parseRulesDirectory(repoDir)
    val (descriptionSet, specificationSet) = parseMarkdownTable(repoDir, hadolintRules)
    writeFiles(specificationSet, descriptionSet, outputDir)
    moveMarkdownFiles(specificationSet, tmpDir + "/markdown", outputDir)
  }

  def parseRulesDirectory(hadolintRepoPath: String): Map[String, Specification] = {
    val ruleDirPath = "/src/Hadolint/Rule"
    val dir = File(hadolintRepoPath + ruleDirPath )

    val ruleFiles: List[File] = dir.list.toList
      .filter(file => file.name.contains("DL"))


    val parsedCodeLines = for {
      ruleFile <- ruleFiles
      codeLines = ruleFile.lineIterator.filter(line => line.contains("code =") || line.contains("severity =")).toList
      parsedCodeLines: List[String] = codeLines.view
        .map(line => line.replaceAll("code =", "").replaceAll("severity =", "").replaceAll("\"", "").trim)
        .toList
    } yield parsedCodeLines

    val rules = for {
      List(ruleName, level) <- parsedCodeLines.flatten.grouped(2).toList
      (category, subcategory) = parseRuleCategory(level, ruleName)
    } yield
      (ruleName,
        Specification(Pattern.Id(ruleName),
          parseRuleLevel(level),
          category,
          subcategory,
          Set.empty,
          enabled = defaultPatterns.contains(ruleName)))

    rules.toMap
  }

  def parseRuleLevel(level: String): Level = {
    level match {
      case "WarningC" => Result.Level.Warn
      case "ErrorC" => Result.Level.Err
      case _ => Result.Level.Info
    }
  }

  def parseRuleCategory(level: String, ruleName: String): (Category, Option[Subcategory]) = {
    ruleName match {
      case "DL3002" | "DL3004" => (Pattern.Category.Security, Some(Subcategory.Auth))
      case "DL3015" | "DL3026" => (Pattern.Category.Security, Some(Subcategory.InsecureModulesLibraries))

      case _ =>
        level match {
          case "InfoC" => (Pattern.Category.CodeStyle, None)
          case _ => (Pattern.Category.ErrorProne, None)
        }
    }
  }

  def parseMarkdownTable(hadolintRepoPath: String,
                         hadolintRules: Map[String, Specification]): (Set[Description], Set[Specification]) = {
    val options = new MutableDataSet
    options.set(Parser.EXTENSIONS, util.Arrays.asList(TablesExtension.create()))
    val parser = Parser.builder(options).build
    val document = parser.parse(File(hadolintRepoPath + "/README.md").contentAsString)

    document.getChildIterator.asScala.toList
      .collect { case table: TableBlock => table }
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
                         Set.empty)
           ),
           Set(
             hadolintRules
               .getOrElse(ruleName,
                          Specification(Pattern.Id(ruleName),
                                        Result.Level.Info,
                                        Pattern.Category.CodeStyle,
                                        None,
                                        Set.empty,
                                        enabled = defaultPatterns.contains(ruleName)))
           ))
      }
      .combineAll
  }

  private def getVersion: String = {
    val repoRoot: File = File("../.hadolint-version")
    repoRoot.lines.mkString("")
  }

  def writeFiles(specificationSet: Set[Specification], descriptionSet: Set[Description], dir: String): Unit = {
    val tool = Tool.Specification(Tool.Name("hadolint"), Some(Tool.Version(getVersion)), specificationSet)
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
      case List(rule: TableCell, severity: TableCell, description: TableCell) =>
        val ruleName = filterType[Link](rule).headOption
          .fold[String](throw new Exception("Failed parsing tableRow to Pattern"))(_.getText.toString)
        val descriptionStr = description.getText.toString
        val parsedStr = Jsoup.parse(descriptionStr).text()
        (ruleName, parsedStr)
    }
  }
}
