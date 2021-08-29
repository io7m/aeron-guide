#!/usr/bin/env runhaskell

import Text.Pandoc.JSON
import qualified Data.Text.IO as TIO
import qualified Data.Text as T

doInclude :: Block -> IO Block
doInclude cb@(CodeBlock (id, classes, namevals) contents) =
  case lookup (T.pack "include") namevals of
    Just f     -> CodeBlock (id, classes, namevals) <$> TIO.readFile (T.unpack f)
    Nothing    -> return cb
doInclude x = return x

main :: IO ()
main = toJSONFilter doInclude
