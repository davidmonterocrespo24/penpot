;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

"Here we put the time functions that are common between frontend and backend.
In the future we may create an unified API for both."

(ns app.common.time
  #?(:cljs (:require
            ["luxon" :as lxn]))
  #?(:clj (:import
           java.time.Instant)))

#?(:clj
   (defn now
     []
     (Instant/now)))

#?(:cljs
   (def DateTime lxn/DateTime))

#?(:cljs
   (def Duration lxn/Duration))

#?(:cljs
   (defn now
     []
     (.local ^js DateTime)))
