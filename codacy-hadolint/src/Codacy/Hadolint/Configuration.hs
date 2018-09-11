{-# LANGUAGE DeriveGeneric #-}
{-# LANGUAGE DuplicateRecordFields #-}

module Codacy.Hadolint.Configuration where

import Data.Aeson hiding (Result)
import GHC.Generics

data CodacyConfig = CodacyConfig
    { files :: [String]
    , tools :: [Tool]
    } deriving (Generic, Show)

data Tool = Tool 
    { name :: String
    , patterns :: Maybe [Pattern]
    } deriving (Generic, Show)

data Pattern = Pattern
    { patternId :: String
    } deriving (Generic, Show)

instance FromJSON CodacyConfig
instance FromJSON Tool
instance FromJSON Pattern


data DocsPattern = DocsPattern {patternId :: String} deriving (Generic)

data PatternList = PatternList {patterns :: [DocsPattern]} deriving (Generic)

instance FromJSON DocsPattern
instance FromJSON PatternList
