package tool

import (
	"os"
	"path"

	codacy "github.com/codacy/codacy-engine-golang-seed/v6"
	"github.com/samber/lo"
)

const sourceConfigurationFileName = ".hadolint.yaml"

func configurationFileAndDisabledPatternIDs(toolExecution codacy.ToolExecution) (bool, []string) {
	if toolExecution.Patterns == nil {
		// Use the tool's configuration file, if it exists.
		// Otherwise use the tool's default patterns.
		if sourceConfigurationFileExists(toolExecution.SourceDir) {
			return true, nil
		}

		return false, lo.FilterMap(
			*toolExecution.ToolDefinition.Patterns,
			func(pattern codacy.Pattern, index int) (string, bool) {
				return pattern.ID, !pattern.Enabled
			})
	}

	if len(*toolExecution.Patterns) == 0 {
		return false, nil
	}

	// if there are configured patterns, create a configuration file from them
	return false, lo.FilterMap(
		*toolExecution.ToolDefinition.Patterns,
		func(pattern codacy.Pattern, index int) (string, bool) {
			return pattern.ID,
				!lo.ContainsBy(
					*toolExecution.Patterns,
					func(p codacy.Pattern) bool {
						return p.ID == pattern.ID
					})
		})
}

func sourceConfigurationFileExists(sourceDir string) bool {
	if fileInfo, err := os.Stat(path.Join(sourceDir, sourceConfigurationFileName)); err != nil || fileInfo.IsDir() {
		return false
	}

	return true
}
