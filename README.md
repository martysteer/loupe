# loupe

An OpenRefine extension for spotting data-quality problems in text — especially messy, multilingual, transliterated metadata.

> **Status:** 41 text quality checks and linguistic analysis tools across encoding, scripts, normalization, diacritics, casing, punctuation, content, glossary, and index.

## About

loupe will let you point OpenRefine at a column and get a plain-language report of the likely problems in it, so you can find and fix messy data without reading every cell by hand. It's aimed at multilingual and transliterated material — names and titles in Arabic, Persian, mixed scripts, and the like — but works on any text.

It will flag things such as:

- garbled or mis-encoded characters
- mixed or unexpected writing systems
- stray, hidden, or inconsistent spacing
- inconsistent accents and apostrophes in transliteration
- empty, placeholder, or obviously broken values

## Installation

For end users: Download `loupe.zip` from releases, unzip, and drag the `loupe` folder into your OpenRefine extensions directory. See [OpenRefine's extension installation docs](https://openrefine.org/docs/manual/installing#installing-extensions) for details.

### Development installation

1. Clone and build: `make extension`
2. Install: `make install`
3. Restart OpenRefine
4. Column dropdown → Facet → **Loupe** submenus

Requires: Leiningen, OpenRefine 3.10.x, macOS (default extensions path).

## Usage

Click any column header → Facet. Four Loupe submenu groups provide text quality checks and linguistic analysis:

- **Loupe: Quality** — composite checks, encoding issues, whitespace analysis, normalization status, reconciliation readiness
- **Loupe: Scripts & Characters** — script detection, directionality, diacritics analysis, case patterns
- **Loupe: Content Analysis** — punctuation, quotes, numeric systems, emoji, word stats, ligatures, character categories
- **Loupe: Glossary & Index** — word index by prefix, word/bigram/trigram glossary, KWIC concordance

Each check creates a text facet grouping cells by detected pattern or issue type. The concordance feature opens a dialog for keyword-in-context (KWIC) search across visible cells.

See `OpenRefine Data Quality Facets.md` for the current list of functions.

## License

CC-BY
