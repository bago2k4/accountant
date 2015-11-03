(ns accountant.history
  (:import goog.history.Html5History))

; this is needed as of this
; https://code.google.com/p/closure-library/source/detail?spec=svn88dc096badf091f380b4c2b4a6514184511de657&r=88dc096badf091f380b4c2b4a6514184511de657
; setToken doen't replace the query string, it only attach it at the end
; solution here: https://github.com/Sparrho/supper/blob/master/src-cljs/supper/history.cljs
(defn build-transformer
  "Custom transformer is needed to replace query parameters, rather
  than adding to them.
  See: https://gist.github.com/pleasetrythisathome/d1d9b1d74705b6771c20"
  []
  (let [transformer (goog.history.Html5History.TokenTransformer.)]
    (set! (.. transformer -retrieveToken)
          (fn [path-prefix location]
            (str (.-pathname location) (.-search location))))
    (set! (.. transformer -createUrl)
          (fn [token path-prefix location]
            (str path-prefix token)))
    transformer))

(defonce history (goog.history.Html5History. js/window (build-transformer)))