(ns homeautomation.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [homeautomation.core-test]))

(doo-tests 'homeautomation.core-test)

