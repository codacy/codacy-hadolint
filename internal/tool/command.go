package tool

import (
	"bytes"
	"encoding/json"
	"io"
	"os/exec"

	codacy "github.com/codacy/codacy-engine-golang-seed/v6"
	"github.com/samber/lo"
)

func createCommand(sourceDir string, files []string, useConfigurationFile bool, disabledPatternIDs []string) *exec.Cmd {
	params := createCommandParameters(files, useConfigurationFile, disabledPatternIDs)
	cmd := exec.Command("hadolint", params...)
	cmd.Dir = sourceDir

	return cmd
}

func createCommandParameters(filesToAnalyse []string, useConfigurationFile bool, disabledPatternIDs []string) []string {
	cmdParams := []string{
		"--no-fail",
		"--verbose",
		"--format", "codacy",
	}

	if !useConfigurationFile {
		lo.ForEach(disabledPatternIDs, func(patternID string, index int) {
			cmdParams = append(cmdParams, "--ignore", patternID)
		})
	}

	cmdParams = append(
		cmdParams,
		filesToAnalyse...,
	)
	return cmdParams
}

func runCommand(cmd *exec.Cmd) (*string, *string, error) {
	var stderr bytes.Buffer
	cmd.Stderr = &stderr
	cmdOutput, err := cmd.Output()
	if err != nil {
		stderrString := stderr.String()
		return nil, &stderrString, err
	}
	cmdOutputString := string(cmdOutput)
	return &cmdOutputString, nil, nil
}

func parseCommandOutput(commandOutput string) ([]codacy.Result, error) {
	var result []codacy.Result

	// Convert the JSON string to a []byte slice
	jsonData := []byte(commandOutput)
	// Create a bytes.Reader from the []byte slice
	reader := bytes.NewReader(jsonData)
	// Create a JSON decoder
	decoder := json.NewDecoder(reader)
	// Read and process the JSON stream
	for {
		var hadolintOutput codacy.Issue
		if err := decoder.Decode(&hadolintOutput); err != nil {
			if err == io.EOF {
				break // End of input
			}
			return nil, err
		}

		// Process the data
		result = append(result, hadolintOutput)
	}

	return result, nil
}
