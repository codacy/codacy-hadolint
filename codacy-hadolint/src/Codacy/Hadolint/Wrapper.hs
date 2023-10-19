{-# LANGUAGE RecordWildCards #-}
{-# LANGUAGE DuplicateRecordFields #-}
{-# LANGUAGE DeriveGeneric #-}

module Codacy.Hadolint.Wrapper where

import qualified Data.ByteString.Lazy.Char8 as B
import Codacy.Hadolint.Configuration
import Data.Aeson hiding (Result)
import Data.Text (Text, pack)
import Data.List (find, (\\))
import qualified Data.Set as Set
import qualified Hadolint.Lint as Hadolint 
import qualified Hadolint.Config.Configuration as Configuration
import qualified Hadolint.Rule as Rule
import qualified Hadolint.Formatter.Format as Format
import qualified Hadolint.Formatter as Formatter
import System.Exit (exitFailure, exitSuccess)
import System.Directory (doesFileExist)
import qualified System.FilePath.Find as Find
import System.FilePath.Find (FindClause, (==?))

import qualified Data.List.NonEmpty as NonEmpty
import Data.List.NonEmpty (NonEmpty( (:|) ))

type IgnoreRule = Text
type FileName = String

data Configs = Configs
    { configurationFile :: Maybe FileName
    , ignoredRules :: [IgnoreRule]
    , filesPaths :: [String]
    } deriving (Show)

configFileName :: String
configFileName = "/.codacyrc"

readPatternsFile :: IO B.ByteString
readPatternsFile = B.readFile "/docs/patterns.json"

readAndParsePatternsFile :: IO PatternList
readAndParsePatternsFile = do
    patternsFileContent <- readPatternsFile
    case eitherDecode patternsFileContent of 
        Left err -> putStrLn err >> exitFailure
        Right config -> return config

readConfigFile :: IO (Maybe B.ByteString)
readConfigFile = do
    fileExists <- doesFileExist configFileName
    case fileExists of
        False -> return Nothing
        True -> fmap Just (B.readFile "/.codacyrc")

readAndParseConfigFile :: IO (Maybe CodacyConfig)
readAndParseConfigFile = do
    configFileContentMaybe <- readConfigFile
    case sequence $ fmap eitherDecode configFileContentMaybe of 
        Left err -> putStrLn err >> exitFailure
        Right config -> return config

defaultConfig :: Configuration
defaultConfig = Configuration
      False
      False
      False
      defaultConfig
      mempty
      mempty
      mempty
      mempty
      mempty
      mempty
      mempty
      False
      False
      defaultConfig

convertToHadolintConfigs :: [DocsPattern] -> Maybe CodacyConfig -> Configuration
convertToHadolintConfigs docs (Just (CodacyConfig _ tools)) =
    case findTool tools of
        Just (Tool _ (Just patterns)) -> Configuration
            False
            False
            False
            Format.OutputFormat.Json
            mempty
            mempty
            mempty
            mempty
            ignoredFromPatterns docs patterns
            mempty
            mempty
            False
            False
            Rule.DLSeverity.DLInfoC
        _ -> defaultConfig
convertToHadolintConfigs _ _ = defaultConfig

ignoredFromPatterns :: [DocsPattern] -> [Pattern] -> [IgnoreRule]
ignoredFromPatterns allPatterns configPatterns = map pack patternsToIgnore
    where
        patternsToIgnore = allPatternIds \\ configPatternIds
        allPatternIds = map (\rule -> patternId (rule :: DocsPattern)) allPatterns
        configPatternIds = map (\rule -> patternId (rule :: Pattern)) configPatterns

findTool :: [Tool] -> Maybe Tool
findTool = find (\tool -> name tool == "hadolint")

readHadolintConfigFile :: Configuration -> IO (Either String Configuration)
readHadolintConfigFile = Config.applyPartialConfiguration Nothing 

filesOrFind :: Maybe CodacyConfig -> IO (NonEmpty.NonEmpty String)
filesOrFind (Just (CodacyConfig (x : xs) _)) = return (x :| xs)
filesOrFind _ = do
    filePaths <- Find.find Find.always (Find.fileName ==? "Dockerfile" Find.||? Find.extension ==? ".dockerfile") "."
    case filePaths of
        (x : xs) -> return (x :| xs)
        _ -> exitSuccess

replace :: Eq a => [a] -> [a] -> [a] -> [a]
replace [] _ _ = []
replace str toRepl repl =
    if take (length toRepl) str == toRepl
        then repl ++ (replace (drop (length toRepl) str) toRepl repl)
        else [head str] ++ (replace (tail str) toRepl repl)

parseFileNames :: NonEmpty.NonEmpty String -> IO (NonEmpty.NonEmpty String)
parseFileNames filePaths = do
    return (NonEmpty.map(\str -> replace str "./" "") filePaths)
    
readHadolintConfig :: Either String Configuration -> IO (Configuration)
readHadolintConfig hadolintConfigEither = 
    case hadolintConfigEither of 
    Left err -> putStrLn err >> exitFailure
    Right conf -> return conf

lint :: IO ()
lint = do
    maybeConfig <- readAndParseConfigFile
    patternsFileContent <- readPatternsFile
    PatternList(parsedPatterns) <- readAndParsePatternsFile
    hadolintConfigEither <- readHadolintConfigFile $ convertToHadolintConfigs parsedPatterns maybeConfig
    hadolintConfig <- readHadolintConfig $ hadolintConfigEither
    filePaths <- filesOrFind maybeConfig
    fileNames <- parseFileNames filePaths
    res <- Hadolint.lint hadolintConfig fileNames
    Formatter.printResults Format.Codacy False res res