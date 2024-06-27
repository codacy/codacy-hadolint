package tool

import (
	"context"
	"errors"

	codacy "github.com/codacy/codacy-engine-golang-seed/v6"
)

// New creates a new instance of Codacy Hadolint.
func New() codacyHadolint {
	return codacyHadolint{}
}

// Codacy Hadolint tool implementation
type codacyHadolint struct {
}

// https://github.com/uber-go/guide/blob/master/style.md#verify-interface-compliance
var _ codacy.Tool = (*codacyHadolint)(nil)

// Run runs the Hadolint implementation
func (s codacyHadolint) Run(ctx context.Context, toolExecution codacy.ToolExecution) ([]codacy.Result, error) {
	useConfigurationFile, disabledPatternIDs := configurationFileAndDisabledPatternIDs(toolExecution)
	if !useConfigurationFile && disabledPatternIDs == nil {
		return nil, nil
	}

	result, err := run(toolExecution, useConfigurationFile, disabledPatternIDs)
	if err != nil {
		return nil, err
	}

	return result, nil
}

func run(toolExecution codacy.ToolExecution, useConfigurationFile bool, disabledPatternIDs []string) ([]codacy.Result, error) {
	hadolintCmd := createCommand(toolExecution.SourceDir, *toolExecution.Files, useConfigurationFile, disabledPatternIDs)

	hadolintOutput, hadolintError, err := runCommand(hadolintCmd)
	if err != nil {
		return nil, errors.New("Error running hadolint: " + *hadolintError + "\n" + err.Error())
	}

	output, err := parseCommandOutput(*hadolintOutput)
	if err != nil {
		return nil, err
	}
	return output, nil
}
