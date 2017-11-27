(ns o-tariff-comparison.core-test
  (:require [clojure.test :refer :all]
            [o-tariff-comparison.core :refer :all])
  (:gen-class))

;; Comments
;; I really don't know how to test in Clojure, i.e. be able to mock, check the code coverage and etc
;; And because of the that reason this exercise hasn't been developed using TDD

(def tariffs
  [{:tariff "A" :rates {:power 0.1 :gas 0.1} :standing_charge 1.0}
   {:tariff "B" :rates {:power 0.2 :gas 0.2} :standing_charge 2.0}
   {:tariff "C" :rates {:power 0.3} :standing_charge 3.0}
   {:tariff "D" :rates {:power 0.4 :gas 0.4} :standing_charge 4.0}])

(testing "get-all-tariffs-costs"
  (deftest should-return-empty-collection-when-empty-tariffs
    (testing "should return empty collection when empty tariffs"
      (is (empty? (get-all-tariffs-costs 0 0 '())))))

  (deftest should-return-sorted-collection
    (let [result (get-all-tariffs-costs 1 1 tariffs)
          costs (map :cost result)]
      (testing "should return a sorted by cost collection"
        (is (= (sort costs) costs))))))

(testing "get-fuel-cost"
  (deftest should-return-0.0-when-no-fuel-type-in-tariff
    ;; Not sure about this one. Could there be a sort of discount for the fuel rate that makes it 0 but the standing_charge still apply?
    (testing "Should return 0.0 when the fuel type is not defined in the tariff"
      (is (= 0.0 (get-fuel-cost 1.0 :non-existent {:tariff "A" :rates {:power 1 :gas 1} :standing_charge 1})))))

  (deftest should-return-0.0-when-no-usage
    (testing "Should return 0.0 when the usage is negative or zero"
      (is (= 0.0 (get-fuel-cost -10.0 :gas {:tariff "A" :rates {:power 1 :gas 1} :standing_charge 1})))))

  (deftest should-return-proper-calculation
    (testing "Should return 12 standing charges plus the usage times the fuel rate"
      (is (= 32.0 (get-fuel-cost 10.0 :gas {:tariff "A" :rates {:power 1 :gas 2} :standing_charge 1})))))

  (deftest should-return-proper-calculation-when-standing-charge-zero
    (testing "Should return the usage times the fuel rate only when the standing charge is 0"
      (is (= 20.0 (get-fuel-cost 10.0 :gas {:tariff "A" :rates {:power 1 :gas 2} :standing_charge 0}))))))

(testing "get-tariff-cost"
  (deftest should-return-0.0-cost-when-power-and-gas-costs-zero
    (testing "Should return a map with cost 0.0 when the power and gast costs are zero"
      (is (= {:tariff "A" :cost 0.0} (get-tariff-cost 0.0 0.0 {:tariff "A" :rates {:power 1 :gas 1} :standing_charge 1})))))

  (deftest should-return-the-power-cost-VAT-included-when-only-power-consumption
    (testing "Should return the proper calculation of the power cost VAT included when there is only power consumption"
      (is (= {:tariff "A" :cost (* 32.0 1.05)} (get-tariff-cost 10.0 0.0 {:tariff "A" :rates {:power 2 :gas 1} :standing_charge 1})))))

  (deftest should-return-the-power-cost-VAT-included-when-only-gas-consumption
    (testing "Should return the proper calculation of the gas cost VAT included when there is only gas consumption"
      (is (= {:tariff "A" :cost (* 32.0 1.05)} (get-tariff-cost 0.0 10.0 {:tariff "A" :rates {:power 1 :gas 2} :standing_charge 1})))))

  (deftest should-return-the-power-cost-VAT-included-when-power-and-gas-consumption
    (testing "Should return the proper calculation of the power and gas cost VAT included"
      (is (= {:tariff "A" :cost (* 2 32.0 1.05)} (get-tariff-cost 10.0 10.0 {:tariff "A" :rates {:power 2 :gas 2} :standing_charge 1}))))))

(testing "calculate-annual-usage"
  (deftest should-return-0.0-when-the-targeted-tariff-doesnt-exist
    (testing "Should return 0.0 when the targeted tariff doesn't exist"
      (is (= 0.0 (calculate-annual-usage "non-existent" "power" 0.0 tariffs)))))

  (deftest should-return-0.0-when-the-targeted-fuel-type-doesnt-exist
    (testing "Should return 0.0 when the targeted tariff doesn't have the wanted fuel type"
      (is (= 0.0 (calculate-annual-usage "C" "gas" 0.0 tariffs)))))

  (deftest should-return-0.0-when-the-monthly-spend-0.0
    (testing "Should return 0.0 when there is no montly spend available"
      (is (= 0.0 (calculate-annual-usage "A" "power" 0.0 tariffs)))))

  (deftest should-return-the-proper-kWh
    (testing "Should return 12 times the amount of kWh obtainable from the amount strictly used for paying the fuel costs of the monthly spend. The amount strictly used for fuel has had the VAT and the standing charge substracted."
      (is (= 1080.0 (calculate-annual-usage "A" "power" 10.5 tariffs))))))

(testing "equal-by-tariff-name"
  (deftest should-return-true
    (testing "Should return true when the names match"
      (is (equal-by-tariff-name "A" {:tariff "A" :other-fields "other-fields"}))))

  (deftest should-return-false
    (testing "Should return false when the name doesn't match"
      (is (not (equal-by-tariff-name "non-existent" {:tariff "A" :other-fields "other-fields"}))))))

(testing "calculate-monthly-usage"
  (deftest should-return-0.0-when-not-enough-monthly-spend-after-removing-VAT
    (testing "Should return 0.0 if after removing VAT there is not enough amount for fuel"
      (is (= 0.0 (calculate-monthly-usage "power" 0.0 {:tariff "A" :rates {:power 0.1} :standing_charge 1})))))

  (deftest should-return-0.0-when-not-enough-monthly-spend-after-removing-standing-charge
    (testing "Should return 0.0 if after removing the standing charge there is not enough amount for fuel"
      (is (= 0.0 (calculate-monthly-usage "power" 10.0 {:tariff "A" :rates {:power 0.1} :standing_charge 100})))))

  (deftest should-return-proper-amount
    (testing "Should return the kWh available to buy after removing VAT and standing charge"
      (is (= 90.0 (calculate-monthly-usage "power" 10.5 {:tariff "A" :rates {:power 0.1} :standing_charge 1}))))))
