;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.main.ui.workspace.palette.text-palette.context-menu.text-palette-ctx-menu
  (:require-macros [app.main.style :refer [css]])
  (:require
   [app.common.data.macros :as dm]
   [app.main.refs :as refs]
   [app.main.ui.components.dropdown :refer [dropdown]]
   [app.main.ui.icons :as i]
   [app.util.i18n :refer [tr]]
   [cuerdas.core :as str]
   [rumext.v2 :as mf]))

(mf/defc text-palette-ctx-menu
  [{:keys [show-menu? close-menu selected]}]
  (let [file-typographies (mf/deref refs/workspace-file-typography)
        shared-libs   (mf/deref refs/workspace-libraries)]
    [:& dropdown {:show show-menu?
                  :on-close close-menu}
     [:ul.workspace-context-menu.palette-menu
      (for [[idx cur-library] (map-indexed vector (vals shared-libs))]
        (let [typographies (-> cur-library (get-in [:data :typographies]) vals)]
          [:li.palette-library
           {:key (str "library-" idx)
            :on-click #(reset! selected (:id cur-library))}

           (when (= @selected (:id cur-library)) i/tick)

           [:div.library-name (str (:name cur-library) " " (str/format "(%s)" (count typographies)))]]))

      [:li.palette-library
       {:on-click #(reset! selected :file)}
       (when (= selected :file) i/tick)
       [:div.library-name (str (tr "workspace.libraries.colors.file-library")
                               (str/format " (%s)" (count file-typographies)))]]]]))
