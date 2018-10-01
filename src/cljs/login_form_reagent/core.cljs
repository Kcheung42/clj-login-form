(ns login-form-reagent.core
  (:require [reagent.core :as reagent :refer [atom]]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]))


;; -------------------------
;; Validation

;; Check if the password has 8 or more characters
;; Has special Characters
;; Has numbers

(defn check-nil-then-prediacte
  "Check if the value is nil then apply the predicate"
  [value predicate]
  (if (nil? value)
    false
    (predicate value)))

(defn eight-or-more-characters?
  [word]
  (check-nil-then-prediacte word (fn [arg] (> (count arg) 7))))

(defn has-special-character?
  [word]
  (check-nil-then-prediacte word (fn [arg] (boolean (first (re-seq #"\W+" arg))))))

(defn has-numbers?
  [word]
  (check-nil-then-prediacte word (fn [arg] (boolean (first (re-seq #"\d+" arg))))))


;; -------------------------
;; Views


;; functions are the building blocks of our UI
(defn input-element
  "A generic input element "
  [id name type value-atom in-focus-atom]
  [:input {:id id
           :name name
           :class "form-control"
           :type type
           :required ""
           :value @value-atom
           :on-change #(reset! value-atom (->> % .-target .-value))
           :on-focus #(swap! in-focus-atom not)
           :on-blur #(swap! in-focus-atom not)}])

;; Old
;; (defn email-input
;;   [email-address-atom]
;; [input-element "email" "email" "email" email-address-atom])


;; Sharing State between components

;; generic function
(defn prompt-message
  "A prompt that will animate to help the user with a given input"
  [message]
  [:div {:class "my-message"}
   [:div {:class "prompt message-animation"}
    [:p message]]])

(defn email-prompt
  []
  (prompt-message "What's your email address?"))

(defn input-and-prompt
  "Creates an input box and a prompt box that apears the input when input
  comes into focus."
  [label-value input-name input-type input-element-arg prompt-element required?]
  (let [input-focus (atom false)]
    (fn []
      [:div
       [:label (str label-value " ")]
       ;; (if @input-focus prompt-element [:div]) ;#1
       [:span [input-element input-name input-name input-type input-element-arg input-focus]] ;#2
       [:span (if @input-focus prompt-element [:div])]
       [:span (if (and required? (= "" @input-element-arg)) ;#3
                [:div "Field is Required"]
                [:div])]])))
;; #1 Deref and check the infocus atom to see if it is true. Display prompt element if it is.
;; #2 the input box from (defn input-element)
;; #3 Simple validation to display.

;; Putting it all together

(defn email-form
  [email-address-atom]
  (input-and-prompt "email"
                    "email"
                    "email"
                    email-address-atom
                    [prompt-message "What's your email"]
                    true)) ;; prompt element must be in []

;; The power of Generics.
;; We can now create UI elements using reusable, testable, utility functions.
(defn name-form
  [name-atom]
  (input-and-prompt "name"
                    "name"
                    "name"
                    name-atom
                    [prompt-message "What's your name"]
                    true)) ;; prompt element must be in []

;; (defn password-requirements
;;   []
;;   [:div
;;    [:ul [:li "hello"]
;;          [:li "boo"]]])

(defn password-requirements
  "Given an atom of password and an array of requirements.
  Return an unordered list of requirements that have been met"
  [password requirements]
  [:div
   [:ul (->> requirements
             (filter (fn [req] (not ((:check-fn req) @password))))
             (doall)
             (map (fn [req] ^{:key req} [:li (:message req)])))]])
;; (map (fn [req] [:li (:message req)])))]])

(defn password-form
  [password-atom]
  [:div
   [(input-and-prompt "password"
                      "password"
                      "password"
                      password-atom
                      [prompt-message "What's your password"]
                      true)]
   [password-requirements password-atom
    [{:message "8 or more Characters" :check-fn eight-or-more-characters?}
     {:message "has special characters" :check-fn has-special-character?}
     {:message "has numbers" :check-fn has-numbers?}]]]) ;; prompt element must be in []

(defn wrap-as-elment-in-form
  [element]
  [:div {:class "form-group"}
   element])

;; syntax is like hiccup.
;; (defn home-page []
;;  [:div [:h2 "Welcome to login-form-reagent"]
;; [:div [:a {:href "/about"} "go to about page"]]])

;; In Reagent, if we do let we must use fn to return the element
(defn signup-page []
  (let [email-address (atom nil)
        name (atom nil)
        password (atom nil)]
    (fn []
      [:div {:class "signup-wrapper"}
       [:h1 "welcome to test chimp"]
       [:form
        (wrap-as-elment-in-form [email-form email-address])
        (wrap-as-elment-in-form [name-form name])
        (wrap-as-elment-in-form [password-form password])]
       [:div [:a {:href "/"} "go to the home page"]]])))

(defn home-page []
  [:div [:h1 "Welcome"]
   [:div [:a {:href "/signup"} "go to Sign-up page"]]])

(defn about-page []
  [:div [:h2 "About login-form-reagent"]
   [:div [:a {:href "/"} "go to the home page"]]])


;; -------------------------
;; Routes

;; use an atom named page to store the session
(defonce page (atom #'home-page))

;; Session variable page
(defn current-page []
  [:div [@page]])

(secretary/defroute "/" []
  (reset! page #'home-page))

(secretary/defroute "/signup" []
  (reset! page #'signup-page))

(secretary/defroute "/about" []
  (reset! page #'about-page))

;; -------------------------
;; Initialize app

;; Render what's defined by the current-page and bind it to the element with id "app"
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (secretary/dispatch! path))
    :path-exists?
    (fn [path]
      (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
