(ns o-tariff-comparison.core
  (:require [clojure.data.json :as json])
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
  [usage fuel tariff]
  (let [fuel-rate (get-in tariff [:rates fuel] :not-found)
        standing-charge (get tariff :standing_charge 0)]
    (if (and (> usage 0) (not (= :not-found fuel-rate) ))
      (+ (* 12 standing-charge) (* usage fuel-rate))
      0.0)))

(defn get-tariff-cost
  [power-usage gas-usage {tariff-name :tariff :as tariff}]
  {:tariff tariff-name :cost (* (+
                                 (get-fuel-cost power-usage :power tariff)
                                 (get-fuel-cost gas-usage :gas tariff))
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
