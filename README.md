# loupe

An OpenRefine extension for spotting data-quality problems in text — especially messy, multilingual, transliterated metadata.

> **Status:** Phase 0 complete. Clojure binding validated. One-click encoding issue detection works.

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
4. Column dropdown → Facet → **Loupe: Encoding Issues**

Requires: Leiningen, OpenRefine 3.10.x, macOS (default extensions path).

## Usage

Click any column header → Facet → **Loupe: Encoding Issues**. A text facet appears grouping cells by encoding issue type (CLEAN, EMPTY, REPLACEMENT_CHAR, CONTROL_CHAR, BOM, INVISIBLE_FORMATTER, MOJIBAKE).

## Documentation

See [`loupe-research-plan.md`](./docs/2026-06-20 loupe-research-plan) for the planned design and roadmap.

## License

CC-BY
