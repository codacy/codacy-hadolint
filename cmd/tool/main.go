package main

import (
	"os"

	codacy "github.com/codacy/codacy-engine-golang-seed/v6"
	"github.com/codacy/codacy-hadolint/internal/tool"
)

func main() {
	codacyHadolint := tool.New()
	retCode := codacy.StartTool(codacyHadolint)

	os.Exit(retCode)
}
