import better.files.File
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GenerateDocsSpecs extends AnyWordSpec with Matchers{

  "parseRulesDirectory" should {

    "parse multiple DLXXXX.hs file into a Specification by providing a hadolint repo" in {

      val value = GenerateDocs.parseRulesDirectory("docs-generator/src/test/resources/hadolint")

      value.size shouldBe 2
      value.contains("DL1001") shouldBe true
      value.contains("DL3000") shouldBe true

    }
  }

  "parseMarkdownTable" should {

    "parse description from hadolint readme file" in {
      val rules = GenerateDocs.parseRulesDirectory("docs-generator/src/test/resources/hadolint")
      val descriptionAndSpec = GenerateDocs.parseMarkdownTable("docs-generator/src/test/resources/hadolint", rules)

      descriptionAndSpec._1.size shouldBe 99
      descriptionAndSpec._2.size shouldBe 99
    }
  }

}
