;; Copyright © 2013, JUXT LTD. All Rights Reserved.
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;;
;; By using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.
;;
;; You must not remove this notice, or any other, from this software.

(ns jig.console.todo
  (:require
   [ring.util.response :as ring-resp]
   [clojure.java.io :as io]
   [hiccup.core :refer (html h)]
   jig
   [jig.console :refer (add-extension ->Boilerplate)])
  (:import (jig Lifecycle)))

(defn find-todo-lines [f]
  (filter :todo
          (map
           (partial zipmap [:file :line :todo])
           (map vector
                (repeat (str f))
                (map inc (range))
                (map #(second (re-find (re-pattern (str \T \O \D \O \: "?\\s*([^\r\n\"]*)")) %))
                     (line-seq (io/reader f)))))))

(defn extract-snippet [file line]
  (apply str
         (interpose \newline
                    (->> file (io/reader) line-seq (drop (- line 5)) (take 10)))))

(defn todo-finder [dir]
  (apply concat
         (for [f (.listFiles dir)]
           (cond
            (.isDirectory f) (todo-finder f)
            (.isFile f) (find-todo-lines f)))))

(defn todos-page [request]
  (ring-resp/response
             (html
              ;; string split so this doesn't end up as a TO-DO
              [:h1 (str \T \O \D \O \s)]
              [:p (format "Developers sometimes think of things to do while coding which, for whatever reason, isn't possible to do at the time. A common convention is to label the comment with '%s'." (str \T \O \D \O))]

              (let [content (for [project (-> request :jig/system :jig/projects)]
                              (let [paths (->> project :project :source-paths (map (comp (memfn getCanonicalFile) io/file)))
                                    todos (mapcat todo-finder paths)]
                                (when (pos? (count todos))
                                  [:div
                                   [:h2 (:name project)]
                                   [:p (format "%d remaining %ss" (count todos) (str \T \O \D \O))]
                                   [:ul
                                    (for [{:keys [file line todo]} todos]
                                      [:div
                                       [:h4 [:i todo]]
                                       [:p "File: " file]
                                       [:p "Line: " line]
                                       [:pre (extract-snippet file line)]
                                       ])]])))]
                (if (empty? content)
                  [:p (format "There are currently no %s. That's good. But if you add them, they'll appear here."
                              (str \T \O \D \O \s))]
                  (list
                   [:p (format "Here are the %s items that remain in the code-base." (str \T \O \D \O))]
                   content)))
              )))

(deftype JigComponent [config]
  Lifecycle
  (init [_ system]
    (add-extension
     system config
     :route ["" (->Boilerplate todos-page)]
     :menuitems [[(str \T \O \D \O \s) todos-page]]))

  (start [_ system] system)
  (stop [_ system] system))
