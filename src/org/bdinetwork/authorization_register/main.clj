;;; SPDX-FileCopyrightText: 2024 Jomco B.V.
;;; SPDX-FileCopyrightText: 2024 Topsector Logistiek
;;; SPDX-FileContributor: Joost Diepenmaat <joost@jomco.nl>
;;; SPDX-FileContributor: Remco van 't Veer <remco@jomco.nl>
;;;
;;; SPDX-License-Identifier: AGPL-3.0-or-later

(ns org.bdinetwork.authorization-register.main
  (:require [org.bdinetwork.authorization-register.system :as system]
            [buddy.core.keys :as keys]
            [nl.jomco.resources :refer [close]]
            [nl.jomco.envopts :as envopts]
            [environ.core :refer [env]])
  (:gen-class))

(def opt-specs
  {:private-key              ["Server private key pem file" :private-key]
   :public-key               ["Server public key pem file" :public-key]
   :x5c                      ["Server certificate chain pem file" :x5c]
   :association-server-id    ["Association Server id" :str]
   :association-server-url   ["Assocation Server url" ]
   :server-id                ["Server ID (EORI)" :str]
   :hostname                 ["Server hostname" :str :default "localhost"]
   :port                     ["Server HTTP Port" :int :default 8080]
   :access-token-ttl-seconds ["Access token time to live in seconds" :int :default 600]})

(defmethod envopts/parse :private-key
  [s _]
  [(keys/private-key s)])

(defmethod envopts/parse :public-key
  [s _]
  [(keys/public-key s)])

(defmethod envopts/parse :x5c
  [s _]
  [(system/x5c s)])

(defn wait-until-interrupted
  []
  (loop []
    (when-not (try (Thread/sleep 10000)
                   false
                   (catch InterruptedException _
                     true))
      (recur))))

(defn config
  []
  (let [[config errs] (envopts/opts env opt-specs)]
    (if errs
      (do (.println *err* "Error in environment configuration")
          (.println *err* (envopts/errs-description errs))
          (.println *err* "Available environment vars:")
          (.println *err* (envopts/specs-description opt-specs))
          nil)
      (do (prn (keys config))
          config))))

(defn -main [& _]
  (if-let [c (config)]
    (let [system (system/run-system c)]
      (.addShutdownHook (Runtime/getRuntime)
                        (Thread. (fn []
                                   (close system)
                                   (shutdown-agents))))
      (wait-until-interrupted))
    (System/exit 1)))