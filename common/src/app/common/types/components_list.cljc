;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KELEIDOS INC

(ns app.common.types.components-list
  (:require
   [app.common.data :as d]
   [app.common.data.macros :as dm]
   [app.common.files.features :as feat]
   [app.common.time :as dt]
   [app.common.types.component :as ctk]))

(defn components
  [file-data]
  (d/removem (fn [[_ component]] (:deleted component))
             (:components file-data)))

(defn components-seq
  [file-data]
  (remove :deleted (vals (:components file-data))))

(defn deleted-components-seq
  [file-data]
  (filter :deleted (vals (:components file-data))))

(defn- touch
  [component]
  (assoc component :modified-at (dt/now)))

(defn add-component
  [file-data {:keys [id name path main-instance-id main-instance-page shapes]}]
  (let [components-v2  (dm/get-in file-data [:options :components-v2])
        wrap-object-fn feat/*wrap-with-objects-map-fn*]
    (cond-> file-data
      :always
      (assoc-in [:components id]
                (touch {:id id
                        :name name
                        :path path}))

      (not components-v2)
      (assoc-in [:components id :objects]
                (->> shapes
                     (d/index-by :id)
                     (wrap-object-fn)))
      components-v2
      (update-in [:components id] assoc
                 :main-instance-id main-instance-id
                 :main-instance-page main-instance-page))))

(defn mod-component
  [file-data {:keys [id name path objects]}]
  (let [wrap-objects-fn feat/*wrap-with-objects-map-fn*]
    (d/update-in-when file-data [:components id]
                      (fn [component]
                        (let [objects (some-> objects wrap-objects-fn)]
                          (cond-> component
                            (some? name)
                            (assoc :name name)

                            (some? path)
                            (assoc :path path)

                            (some? objects)
                            (assoc :objects objects)
                            
                            :always
                            (touch)))))))

(defn get-component
  ([file-data component-id]
   (get-component file-data component-id false))

  ([file-data component-id include-deleted?]
  (let [component (get-in file-data [:components component-id])]
    (when (or include-deleted?
              (not (:deleted component)))
      component))))

(defn get-deleted-component
  [file-data component-id]
  (let [component (get-in file-data [:components component-id])]
    (when (:deleted component)
      component)))

(defn update-component
  [file-data component-id f & args]
  (d/update-in-when file-data [:components component-id] #(-> (apply f % args)
                                                              (touch))))

(defn set-component-modified
  [file-data component-id]
  (update-component file-data component-id identity))

(defn delete-component
  [file-data component-id]
  (update file-data :components dissoc component-id))

(defn mark-component-deleted
  [file-data component-id]
  (d/assoc-in-when file-data [:components component-id :deleted] true))

(defn mark-component-undeleted
  [file-data component-id]
  (d/dissoc-in file-data [:components component-id :deleted]))

(defn used-components-changed-since
  "Check if the shape is an instance of any component in the library, and
   the component has been modified after the date."
  [shape library since-date]
  (if (ctk/uses-library-components? shape (:id library))
    (let [component (get-component (:data library) (:component-id shape))]
      (if (> (:modified-at component) since-date)
        [[(:id shape) (:component-id shape) :component]]
        []))
    []))
