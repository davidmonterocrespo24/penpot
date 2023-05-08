;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.worker.object-thumbnails
  (:require
   [app.worker.impl :as impl]
   [promesa.core :as p]))

(defmethod impl/handler :object-thumbnails/generate
  [_ ibpm]
  (let [canvas (js/OffscreenCanvas. (.-width ^js ibpm) (.-height ^js ibpm))
        ctx    (.getContext ^js canvas "bitmaprenderer")]

    (.transferFromImageBitmap ^js ctx ibpm)

    (->> (.convertToBlob ^js canvas #js {:type "image/png"})
         (p/fmap (fn [blob]
                   (js/console.log "[worker]: generated thumbnail")
                   {:result (.createObjectURL js/URL blob)}))
         (p/fnly (fn [_]
                   (.close ^js ibpm))))))
