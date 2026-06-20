(ns loupe.checks
  (:require [clojure.string :as str]))

(defn encoding-issues
  "Detect encoding issues in a string value.
  Returns pipe-separated issue labels or \"CLEAN\".
  Returns \"EMPTY\" for nil/blank input."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [s (str value)
          issues
          (filterv
           some?
           [(when (re-find #"\uFFFD" s)
              "REPLACEMENT_CHAR")

            (when (re-find #"[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F-\u009F]" s)
              "CONTROL_CHAR")

            (when (re-find #"\uFEFF" s)
              "BOM")

            (when (re-find #"[\u200B-\u200F\u2028-\u202E\u2060-\u2069]" s)
              "INVISIBLE_FORMATTER")

            (when (re-find #"\u00C3[\u0080-\u00BF]" s)
              "MOJIBAKE")])]
      (if (empty? issues)
        "CLEAN"
        (str/join "|" issues)))))
