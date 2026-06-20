(ns loupe.checks.whitespace
  (:require [clojure.string :as str]))

(defn whitespace-types
  "Categorize whitespace characters present.
  Returns descriptive string."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [s (str value)]
      (cond
        (not (re-find #"[\s\u00A0\u2000-\u200B\u2009]" s)) "no whitespace"
        (re-find #"\u00A0" s) "has non-breaking space"
        (re-find #"\u2009" s) "has thin space"
        (re-find #"\u200B" s) "has zero-width space"
        (re-find #"[\u2000-\u2008\u200A]" s) "has special Unicode spaces"
        (and (re-find #"\s" s) (not (re-find #"[^\u0020\u0009\u000A\u000D]" s))) "normal spaces only"
        :else "normal spaces only"))))
