;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.main.data.workspace.annotation-helpers
  (:require
   [app.common.data.macros :as dm]
   [app.common.logging :as log]
   [app.common.types.components-list :as ctkl]
   [app.common.types.file :as ctf]
   [app.main.refs :as refs]))

;; Change this to :info :debug or :trace to debug this module, or :warn to reset to default
(log/set-level! :warn)

(defn- get-libraries
  "Retrieve all libraries, including the local file."
  []
  (let [workspace-data (deref refs/workspace-data)
        {:keys [id] :as local} workspace-data]
    (-> (deref refs/workspace-libraries)
        (assoc id {:id id
                   :data local}))))

(defn get-main-annotation
  [shape]
  (if (:main-instance? shape)
    (:annotation shape)
    (let [libraries      (get-libraries)
          library        (dm/get-in libraries [(:component-file shape) :data])
          component      (ctkl/get-component library (:component-id shape) true)
          shape-main     (ctf/get-ref-shape library component shape)]
      (:annotation shape-main))))
