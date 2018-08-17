import com.typesafe.sbt.SbtScalariform._

object Format {
  lazy val settings = projectSettings ++ Seq(
    autoImport.scalariformAutoformat := true,
    ScalariformKeys.preferences := formattingPreferences)

  lazy val formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences().
      setPreference(AlignParameters, false).
      setPreference(AlignSingleLineCaseStatements, true).
      setPreference(CompactControlReadability, false).
      setPreference(CompactStringConcatenation, false).
      setPreference(DoubleIndentConstructorArguments, true).
      setPreference(FormatXml, true).
      setPreference(IndentLocalDefs, false).
      setPreference(IndentPackageBlocks, true).
      setPreference(IndentSpaces, 2).
      setPreference(MultilineScaladocCommentsStartOnFirstLine, false).
      setPreference(PreserveSpaceBeforeArguments, false).
      setPreference(DanglingCloseParenthesis, Preserve).
      setPreference(RewriteArrowSymbols, false).
      setPreference(SpaceBeforeColon, false).
      setPreference(SpaceInsideBrackets, false).
      setPreference(SpacesAroundMultiImports, true).
      setPreference(SpacesWithinPatternBinders, true)
  }
}
