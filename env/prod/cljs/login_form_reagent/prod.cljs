(ns login-form-reagent.prod
  (:require [login-form-reagent.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
