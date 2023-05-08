;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.main.ui.workspace.shapes.frame.thumbnail-render-alt
  (:require
   [app.common.data :as d]
   [app.common.data.macros :as dm]
   [app.common.geom.shapes :as gsh]
   [app.common.math :as mth]
   [app.config :as cf]
   [app.main.data.workspace.thumbnails :as dwt]
   [app.main.refs :as refs]
   [app.main.store :as st]
   [app.main.ui.hooks :as hooks]
   [app.main.ui.shapes.frame :as frame]
   [app.util.dom :as dom]
   [app.util.timers :as ts]
   [app.util.webapi :as wapi]
   [beicon.core :as rx]
   [cuerdas.core :as str]
   [debug :refer [debug?]]
   [promesa.core :as p]
   [rumext.v2 :as mf]))

;; async
;;   http/send! {:response-type :blob :method :get} 

;; sync
;;   data-uri->blob

(defn use-render-thumbnail
  "Hook that will create the thumbnail data"
  [page-id {:keys [id] :as shape} node-ref rendered? disable? force-render]

  (let [;; Children of the frame.
        all-children-ref (mf/use-memo (mf/deps id) #(refs/all-children-objects id))
        all-children (mf/deref all-children-ref)

        ;; Shape bounding box.
        {:keys [x y width height] :as shape-rect}
        (if (:show-content shape)
          (gsh/selection-rect (concat [shape] all-children))
          (-> shape :points gsh/points->selrect))

        ;; Fixed width and Fixed height
        ;; for when we need to render the thumbnail.
        [fixed-width fixed-height]
        (if (> width height)
          [(mth/clamp width 250 2000)
           (/ (* height (mth/clamp width 250 2000)) width)]
          [(/ (* width (mth/clamp height 250 2000)) height)
           (mth/clamp height 250 2000)])

        ;; Thumbnail data retrieved from :workspace-thumbnails.
        thumbnail-data-ref (mf/use-memo (mf/deps page-id id) #(refs/thumbnail-frame-data page-id id))
        thumbnail-data     (mf/deref thumbnail-data-ref)

        ;; State to indicate to the parent that should render the frame
        render-frame? (mf/use-state (not thumbnail-data))

        ;; SVG URL to render the frame.
        svg-url (mf/use-state nil)

        ;; Function exposed to the parent to render the frame.
        on-mount-frame
        (mf/use-callback
         (fn [node] (prn "on-mount-frame" node)))

        on-image-load
        (mf/use-callback 
         (fn [node] (prn "on-image-load" node)))
        
        _ (js/console.log thumbnail-data)]

    [on-mount-frame
     @render-frame?
     (mf/html
      [:& frame/frame-container {:bounds shape-rect
                                 :shape (cond-> shape
                                          (some? thumbnail-data)
                                          (assoc :thumbnail thumbnail-data))}
      ;; Render the frame when there is thumbnail-data
      ;; available
      (when (some? thumbnail-data)
        [:> frame/frame-thumbnail-image
         {:key (dm/str (:id shape))
          :bounds shape-rect
          :shape (cond-> shape
                   (some? thumbnail-data)
                   (assoc :thumbnail thumbnail-data))}])

      [:image {:x x
               :y y
               :src @svg-url
               :width fixed-width
               :height fixed-height
               :on-load on-image-load}]])]))
