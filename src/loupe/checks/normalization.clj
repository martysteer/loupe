(ns loupe.checks.normalization)

(defn normalization-status
  "Check NFC/NFD normalization status.
  Returns ASCII/Simple, NFC normalized, NFD normalized, or Non-normalized."
  [value]
  (if (or (nil? value) (clojure.string/blank? (str value)))
    "EMPTY"
    (let [s (str value)
          nfc (java.text.Normalizer/normalize s java.text.Normalizer$Form/NFC)
          nfd (java.text.Normalizer/normalize s java.text.Normalizer$Form/NFD)]
      (cond
        (and (= s nfc) (= s nfd)) "ASCII/Simple"
        (= s nfc) "NFC normalized"
        (= s nfd) "NFD normalized"
        :else "Non-normalized"))))
