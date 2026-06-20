# Loupe v0.1.0 Release Notes

**Release Date:** June 20, 2026

## Summary

First stable release of Loupe - a comprehensive OpenRefine extension for text data quality inspection. Provides 30+ specialized checks for multilingual and transliterated metadata through an organized menu interface.

## Features

### Three Menu Groups with 31 Quality Checks

**Loupe: Quality (9 checks)**
- All Issues — comprehensive scan running 27+ checks
- Quality Grade (A-F) — letter grade assessment
- Quality Score (0-100) — numeric quality score
- Encoding Issues — detect mojibake, replacement chars, control chars
- Whitespace Types — identify invisible spacing problems
- Normalization Status — check NFC/NFD normalization
- Reconciliation Ready — pre-reconciliation validation
- Fresh Hell Detector — extreme anomaly detection
- Autopsy Report — complete character breakdown

**Loupe: Scripts & Characters (11 checks)**
- Primary Script — detect dominant writing system
- Mixed Scripts — find multi-script mixing
- Directionality — LTR/RTL/bidirectional detection
- Diacritics: Unique Marks — extract combining marks
- Diacritics: Letters — find accented letters
- Diacritics: Non-ASCII — all non-ASCII characters
- Diacritics: Type — classify diacritic types
- Diacritics: Count — count combining marks
- Case Pattern — detect capitalization patterns
- Case Complexity — percentage uppercase
- Word Case Pattern — per-word capitalization

**Loupe: Content Analysis (11 checks)**
- Punctuation — extract unique punctuation
- Quote Style — smart/straight/CJK quotes
- Numeric System — detect numeral scripts
- Emoji — emoji detection and count
- Word Stats — word count and average length
- Ligatures — typographic ligature detection
- Character Count — string length
- Byte Count — UTF-8 byte count
- String Shape — abstract pattern (Aaaa_000)
- Character Categories — Unicode category breakdown
- Dominant Character Type — letter/digit/punct dominant

## Installation

1. Download `loupe.zip` from this release
2. Unzip into your OpenRefine workspace extensions folder:
   - **macOS:** `~/Library/Application Support/OpenRefine/extensions/`
   - **Windows:** `%APPDATA%\OpenRefine\extensions\`
   - **Linux:** `~/.local/share/openrefine/extensions/`
3. Restart OpenRefine
4. Look for **Loupe** submenus under any column's Facet menu

## Requirements

- OpenRefine 3.10.x or later
- No additional dependencies required (all bundled)

## Technical Details

- **Implementation:** Clojure 1.12.3, AOT-compiled
- **Architecture:** 9 namespaces with pure String → String functions
- **Test Coverage:** 31 tests, 107 assertions, 100% passing
- **Self-contained:** All dependencies bundled, no external JARs needed

## Use Cases

- **Multilingual metadata QA** — detect script mixing, normalization issues, encoding problems
- **Pre-reconciliation cleanup** — validate data before Wikidata/VIAF matching
- **Transliteration consistency** — spot apostrophe/accent/quote inconsistencies
- **Import validation** — catch encoding corruption, invisible characters, format violations
- **Data profiling** — understand text composition and identify outliers

## Documentation

- Full function reference with usage examples: `OpenRefine Data Quality Facets.md`
- Each check includes "What it does" and "When to use" explanations

## Known Limitations

- Text facets only (no custom dialogs or aggregated reports in this release)
- Tested on macOS; Windows/Linux paths documented but not tested
- No CLI version yet (planned for future release)

## What's Next

Future releases may include:
- Aggregated per-column quality reports
- Custom result dialogs
- Command-line tool using the same core library
- Additional quality checks based on user feedback

## License

CC-BY

## Credits

Built with Clojure and love for clean data.

Co-Authored-By: Claude Sonnet 4.5
