(ns ^:figwheel-no-load login-form-reagent.dev
  (:require
    [login-form-reagent.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
