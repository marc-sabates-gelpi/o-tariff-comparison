(ns o-tariff-comparison.core
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [o-tariff-comparison.specs :refer :all]
            [clojure.string :as string])
  (:gen-class))

(def vat 1.05)
(def file-name "prices.json")
(def cost-command "cost")
(def usage-command "usage")
(def command-not-found (format "The command '%%s' is not implemented yet, please use '%s' or '%s'" cost-command usage-command))
(def cost-output-format "%s %.2f")
(def usage-output-format "%.2f")


(defn string-number [str]
  (try
    (let [n (read-string str)]
      (if (number? n) n nil))
    (catch Exception e nil)))

(defn get-fuel-cost
  [usage fuel-type a-tariff]
  (let [fuel-rate (get-in a-tariff [:rates fuel-type] :not-found)
        standing-charge (get a-tariff :standing_charge 0)]
    (if (and (> usage 0) (not (= :not-found fuel-rate) ))
      (+ (* 12 standing-charge) (* usage fuel-rate))
      0.0)))

(defn get-tariff-cost
  [power-usage gas-usage {:keys [tariff] :as a-tariff}]
  {:tariff tariff :cost (* (+
                            (get-fuel-cost power-usage :power a-tariff)
                            (get-fuel-cost gas-usage :gas a-tariff))
                           vat)})

(defn get-all-tariffs-costs
  [power-usage gas-usage tariffs]
  (sort-by :cost (map
                  (partial get-tariff-cost power-usage gas-usage)
                  tariffs)))

(defn print-cost
  [& args]
  (doseq [{:keys [tariff cost]} (apply get-all-tariffs-costs args)]
    (println (format cost-output-format tariff cost))))

(defn calculate-monthly-usage
  [fuel-type monthly-spend targeted-tariff]
  (let [energy-spend (- (/ monthly-spend vat) (:standing_charge targeted-tariff))]
    (if (> energy-spend 0.0)
      (if-let [fuel-rate (get-in targeted-tariff [:rates (keyword fuel-type)] false)]
        (/ energy-spend fuel-rate)
        0.0)
      0.0)))

(defn equal-by-tariff-name
  [searched-name {:keys [tariff]}]
  (= searched-name tariff))

(defn calculate-annual-usage
  [tariff-name fuel-type monthly-spend tariffs]
  (if-let [targeted-tariff (first (filter (partial equal-by-tariff-name tariff-name) tariffs))]
    (* 12 (calculate-monthly-usage fuel-type monthly-spend targeted-tariff))
    0.0))

(defn print-annual-usage
  [& args]
  (->> args
      (apply calculate-annual-usage)
      (format usage-output-format)
      println))

(defn print-not-found
  [command]
  (->> command
       (format command-not-found)
       println))

(defn -main
  "O Tariff comparison"
  [& [command arg1 arg2 arg3]]
  (-> file-name
      slurp
      (json/read-str :key-fn keyword)
      (as-> tariffs
          (cond (= command cost-command) (print-cost (string-number arg1) (string-number arg2) tariffs) 
                (= command usage-command) (print-annual-usage arg1 arg2 (string-number arg3) tariffs)
                :else (print-not-found command)))))

;; (s/fdef -main
;;         :args :o-tariff-comparison.specs/f-main)

(s/fdef get-tariff-cost
        :args :o-tariff-comparison.specs/f-get-tariff-cost
        :ret :o-tariff-comparison.specs/ret-get-tariff-cost
        :fn :o-tariff-comparison.specs/fn-get-tariff-cost)

(s/fdef get-fuel-cost
        :args :o-tariff-comparison.specs/f-get-fuel-cost
        :ret :o-tariff-comparison.specs/ret-get-fuel-cost
        :fn :o-tariff-comparison.specs/fn-get-fuel-cost)

(defn condition-data [[timestamp consumption]]
  (list (subs timestamp 0 7) (string-number consumption)))
(defn aggregate [elem]
  {:month (ffirst elem) :consumption (transduce (map second) + elem)})
(defn monthly-usage []
  (->> "/home/user/labs/clojure/o-tariff-comparison/consumption.csv"
      slurp
      string/split-lines
      rest
      (eduction (comp
                 (map #(string/split % #","))
                 (map condition-data)
                 (partition-by first)
                 (map aggregate)))
      (sort-by :month)))
