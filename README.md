# loupe

An OpenRefine extension for spotting data-quality problems in text — especially messy, multilingual, transliterated metadata.

> **Status:** Phase 1 complete. Full v2 facet library ported. 30+ text quality checks across encoding, scripts, normalization, diacritics, casing, punctuation, and content analysis.

## About

loupe will let you point OpenRefine at a column and get a plain-language report of the likely problems in it, so you can find and fix messy data without reading every cell by hand. It's aimed at multilingual and transliterated material — names and titles in Arabic, Persian, mixed scripts, and the like — but works on any text.

It will flag things such as:

- garbled or mis-encoded characters
- mixed or unexpected writing systems
- stray, hidden, or inconsistent spacing
- inconsistent accents and apostrophes in transliteration
- empty, placeholder, or obviously broken values

## Installation (for testing)

1. Clone and build: `make extension`
2. Install: `make install`
3. Restart OpenRefine
4. Column dropdown → Facet → **Loupe** submenus

Requires: Leiningen, OpenRefine 3.10.x, macOS (default extensions path).

## Usage

Click any column header → Facet. Three Loupe submenu groups provide 31 text quality checks:

- **Loupe: Quality** — composite checks, encoding issues, whitespace analysis, normalization status, reconciliation readiness
- **Loupe: Scripts & Characters** — script detection, directionality, diacritics analysis, case patterns
- **Loupe: Content Analysis** — punctuation, quotes, numeric systems, emoji, word stats, ligatures, character categories

Each check creates a text facet grouping cells by detected pattern or issue type.

## Documentation

See [`loupe-research-plan.md`](./docs/2026-06-20 loupe-research-plan) for the planned design and roadmap.

## License

CC-BY
