import better.files.File
import com.codacy.plugins.api.results.Pattern.Specification

object CreateMarkdowns {

  def moveMarkdownFiles(specificationSet: Set[Specification], inputDir: String, outputDir: String): Unit = {
    for {
      spec <- specificationSet.toList
      filename = s"**/${spec.patternId.value}.md"
      file <- File(inputDir).glob(filename).toStream.headOption
    } file.copyToDirectory(File(s"$outputDir/description"))
  }
}
