;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.main.ui.workspace.sidebar.options.menus.component
  (:require
    [app.common.types.components-list :as ctkl]
    [app.common.types.file :as ctf]
    [app.main.data.modal :as modal]
    [app.main.data.workspace :as dw]
    [app.main.data.workspace.annotation-helpers :as dwah]
    [app.main.data.workspace.libraries :as dwl]
    [app.main.refs :as refs]
    [app.main.store :as st]
    [app.main.ui.components.context-menu :refer [context-menu]]
    [app.main.ui.context :as ctx]
    [app.main.ui.icons :as i]
    [app.util.dom :as dom]
    [app.util.i18n :as i18n :refer [tr]]
    [rumext.v2 :as mf]))

(def component-attrs [:component-id :component-file :shape-ref :main-instance? :annotation])


(mf/defc component-annotation
  [{:keys [id values shape] :as props}]
  (let [main-instance?        (:main-instance? values)
        annotation            (dwah/get-main-annotation shape)
        editing?              (mf/use-state false)
        size                  (mf/use-state (count annotation))
        ;; hack to create an autogrowing textarea
        ;; based on https://css-tricks.com/the-cleanest-trick-for-autogrowing-textareas/
        autogrow              #(let [textarea (dom/get-element "annotation-textarea")
                                     text (when textarea (.-value textarea))]
                                 (when textarea
                                   (reset! size (count text))
                                   (aset (.-dataset (.-parentNode textarea)) "replicatedValue" text)))

        discard               (fn [event]
                                (dom/stop-propagation event)
                                (let [textarea (dom/get-element "annotation-textarea")]
                                  (aset textarea "value" annotation)
                                  (reset! editing? false)
                                  (st/emit! (dw/set-annotations-create-id nil))))
        save                  (fn [event]
                                (dom/stop-propagation event)
                                (let [textarea (dom/get-element "annotation-textarea")
                                      text (.-value textarea)]
                                  (reset! editing? false)
                                  (st/emit!
                                   (dw/set-annotations-create-id nil)
                                   (dw/update-component-annotation id text))))
        workspace-annotations (mf/deref refs/workspace-annotations)
        annotations-expanded? (:expanded? workspace-annotations)
        creating?             (= id (:create-id workspace-annotations))

        expand                #(when-not (or @editing? creating?)
                                 (st/emit! (dw/set-annotations-expanded %)))
        edit                  (fn [event]
                                (dom/stop-propagation event)
                                (when main-instance?
                                  (let [textarea (dom/get-element "annotation-textarea")]
                                    (reset! editing? true)
                                    (dom/focus! textarea))))

        on-delete-annotation
        (mf/use-callback
         (mf/deps shape)
         (fn [event]
           (dom/stop-propagation event)
           (st/emit! (modal/show
                      {:type :confirm
                       :title (tr "modals.delete-component-annotation.title")
                       :message (tr "modals.delete-component-annotation.message")
                       :accept-label (tr "ds.confirm-ok")
                       :on-accept (fn []
                                    (st/emit!
                                     (dw/set-annotations-create-id nil)
                                     (dw/update-component-annotation id nil)))}))))]

    (mf/use-effect
     (fn []
       (autogrow)
       (when (and (not creating?) (:create-id workspace-annotations)) ;; cleanup set-annotations-create-id if we aren't on the marked component
         (st/emit! (dw/set-annotations-create-id nil)))
       (fn [] (st/emit! (dw/set-annotations-create-id nil))))) ;; cleanup set-annotations-create-id on unload

    (when (or creating? annotation)
      [:div.component-annotation {:class (dom/classnames :editing @editing?)}
       [:div.title {:class (dom/classnames :expandeable (not (or @editing? creating?)))
                    :on-click #(expand (not annotations-expanded?))}
        [:div (if (or @editing? creating?)
                (if @editing?
                  (tr "workspace.options.component.edit-annotation")
                  (tr "workspace.options.component.create-annotation"))
                [:* (if annotations-expanded?
                      [:div.expand i/arrow-down]
                      [:div.expand i/arrow-slide])
                 (tr "workspace.options.component.annotation")])]
        [:div
         (when (and main-instance? annotations-expanded?)
           (if (or @editing? creating?)
             [:*
              [:div.icon {:title (if creating? (tr "labels.create") (tr "labels.save"))
                          :on-click save} i/tick]
              [:div.icon {:title (tr "labels.discard")
                          :on-click discard} i/cross]]
             [:*
              [:div.icon {:title (tr "labels.edit")
                          :on-click edit} i/pencil]
              [:div.icon {:title (tr "labels.delete")
                          :on-click on-delete-annotation} i/trash]]))]]

       [:div {:class (dom/classnames :hidden (not annotations-expanded?))}
        [:div.grow-wrap
         [:div.texarea-copy]
         [:textarea
          {:id "annotation-textarea"
           :auto-focus true
           :maxLength 300
           :on-input autogrow
           :default-value annotation
           :read-only (not (or creating? @editing?))}]]
        (when (or @editing? creating?)
          [:div.counter (str @size "/300")])]])))



(mf/defc component-menu
  [{:keys [ids values shape-name shape] :as props}]
  (let [current-file-id     (mf/use-ctx ctx/current-file-id)
        components-v2       (mf/use-ctx ctx/components-v2)

        id                  (first ids)
        local               (mf/use-state {:menu-open false})

        component-id        (:component-id values)
        library-id          (:component-file values)
        show?               (some? component-id)
        main-instance?      (if components-v2
                              (:main-instance? values)
                              true)
        main-component?     (:main-instance? values)
        lacks-annotation?   (nil? (:annotation values))
        local-component?    (= library-id current-file-id)
        workspace-data      (deref refs/workspace-data)
        workspace-libraries (deref refs/workspace-libraries)
        is-dangling?        (nil? (if local-component?
                                    (ctkl/get-component workspace-data component-id)
                                    (ctf/get-component workspace-libraries library-id component-id)))

        on-menu-click
        (mf/use-callback
         (fn [event]
           (dom/prevent-default event)
           (dom/stop-propagation event)
           (swap! local assoc :menu-open true)))

        on-menu-close
        (mf/use-callback
         #(swap! local assoc :menu-open false))

        do-detach-component
        #(st/emit! (dwl/detach-component id))

        do-reset-component
        #(st/emit! (dwl/reset-component id))

        do-update-component
        #(st/emit! (dwl/update-component-sync id library-id))

        do-restore-component
        #(st/emit! (dwl/restore-component library-id component-id))

        do-update-remote-component
        #(st/emit! (modal/show
                    {:type :confirm
                     :message ""
                     :title (tr "modals.update-remote-component.message")
                     :hint (tr "modals.update-remote-component.hint")
                     :cancel-label (tr "modals.update-remote-component.cancel")
                     :accept-label (tr "modals.update-remote-component.accept")
                     :accept-style :primary
                     :on-accept do-update-component}))

        do-show-component #(st/emit! (dw/go-to-component component-id))
        do-show-in-assets #(st/emit! (if components-v2
                                       (dw/show-component-in-assets component-id)
                                       (dw/go-to-component component-id)))
        do-create-annotation #(st/emit! (dw/set-annotations-create-id id))
        do-navigate-component-file #(st/emit! (dwl/nav-to-component-file library-id))]
    (when show?
      [:div.element-set
       [:div.element-set-title
        [:span (tr "workspace.options.component")]]
       [:div.element-set-content
        [:div.row-flex.component-row
         (if main-instance?
           i/component
           i/component-copy)
         shape-name
         [:div.row-actions
          {:on-click on-menu-click}
          i/actions
          ;; WARNING: this menu is the same as the shape context menu.
          ;;          If you change it, you must change equally the file
          ;;          app/main/ui/workspace/context_menu.cljs
          [:& context-menu {:on-close on-menu-close
                            :show (:menu-open @local)
                            :options
                            (if main-component?
                              [[(tr "workspace.shape.menu.show-in-assets") do-show-in-assets]
                               (when (and components-v2 lacks-annotation?)
                                 [(tr "workspace.shape.menu.create-annotation") do-create-annotation])]
                              (if local-component?
                                (if is-dangling?
                                  [[(tr "workspace.shape.menu.detach-instance") do-detach-component]
                                   [(tr "workspace.shape.menu.reset-overrides") do-reset-component]
                                   (when components-v2
                                     [(tr "workspace.shape.menu.restore-main") do-restore-component])]

                                  [[(tr "workspace.shape.menu.detach-instance") do-detach-component]
                                   [(tr "workspace.shape.menu.reset-overrides") do-reset-component]
                                   [(tr "workspace.shape.menu.update-main") do-update-component]
                                   [(tr "workspace.shape.menu.show-main") do-show-component]])

                                (if is-dangling?
                                  [[(tr "workspace.shape.menu.detach-instance") do-detach-component]
                                   [(tr "workspace.shape.menu.reset-overrides") do-reset-component]
                                   (when components-v2
                                     [(tr "workspace.shape.menu.restore-main") do-restore-component])]
                                  [[(tr "workspace.shape.menu.detach-instance") do-detach-component]
                                   [(tr "workspace.shape.menu.reset-overrides") do-reset-component]
                                   [(tr "workspace.shape.menu.update-main") do-update-remote-component]
                                   [(tr "workspace.shape.menu.go-main") do-navigate-component-file]])))}]]]

        (when components-v2
          [:& component-annotation {:id id :values values :shape shape}])]])))
