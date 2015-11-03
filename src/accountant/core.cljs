(ns accountant.core
  "The only namespace in this library."
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! <! chan]]
            [clojure.string :as str]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [accountant.history :as html5history])
  (:import goog.Uri))

(defn- listen [el type]
  (let [out (chan)]
    (events/listen el type
                   (fn [e] (put! out e)))
    out))

(defn- dispatch-on-navigate
  [secretary-dispatcher history]
  (let [navigation (listen history EventType/NAVIGATE)]
    (go
      (while true
        (let [token (.-token (<! navigation))]
          (secretary-dispatcher token))))))

(defn- find-a-attrs
  "Given a DOM element that may or may not be a link, traverse up the DOM tree
  to see if any of its parents are links. If so, return the href and the target content."
  [e]
  ((fn [el]
     (if-let [href (.-href el)]
       [href (.-target el)]
       (if-let [parent (.-parentNode el)]
         (recur parent)
         [nil nil]))) (.-target e)))

(defn- locate-route [routes needle]
  "check if route is handled by secretary routes stack"
  (some
   (fn [route]
     (when (secretary/route-matches route needle)
       route))
   routes))

(defn- prevent-reload-on-known-path
  "Create a click handler that blocks page reloads for known routes in
  Secretary."
  [routes-stack history]
  (events/listen
   js/document
   "click"
   (fn [e]
     (let [button (.-button e)
           [href a-target] (find-a-attrs e)
           parsed-uri (.parse Uri href)
           path (.getPath parsed-uri)
           domain (.getDomain parsed-uri)
           title (.-title (.-target e))]
       (when (and (= button 0)                           ; is left button click
                  (not (nil? href))                      ; the user has hit an element with href
                  (or (empty? domain)                    ; the domain is empty
                      (= domain (.. js/window -location -hostname))) ; or is the same of the current domain
                  (or (empty? a-target)                  ; the target of the a tag is empty
                      (= a-target "_self"))              ; or is _self
                  (locate-route routes-stack path))      ; path is handled by secretary
         (. history (setToken path title))
         (.preventDefault e))))))

(defn configure-navigation!
  "Create and configure HTML5 history navigation."
  [secretary-dispatcher routes-stack]
  (.setUseFragment html5history/history false)
  (.setPathPrefix html5history/history "")
  (.setEnabled html5history/history true)
  (dispatch-on-navigate secretary-dispatcher html5history/history)
  (prevent-reload-on-known-path routes-stack html5history/history))

(defn map->params [query]
  (let [params (map #(name %) (keys query))
        values (vals query)
        pairs (partition 2 (interleave params values))]
    (str/join "&" (map #(str/join "=" %) pairs))))

(defn navigate!
  "add a browser history entry. updates window/location"
  ([route] (navigate! route {}))
  ([route query]
     (let [token (.getToken html5history/history)
           old-route (first (str/split token "?"))
           query-string (map->params (reduce-kv (fn [valid k v]
                                                  (if v
                                                    (assoc valid k v)
                                                    valid)) {} query))
           with-params (if (empty? query-string)
                         route
                         (str route "?" query-string))]
       (if (= old-route route)
         (. html5history/history (replaceToken with-params))
         (. html5history/history (setToken with-params))))))

(defn dispatch-current! [secretary-dispatcher]
  "Dispatch current URI path."
  (let [path (-> js/window .-location .-pathname)
        query (-> js/window .-location .-search)]
    (secretary-dispatcher (str path query))))
