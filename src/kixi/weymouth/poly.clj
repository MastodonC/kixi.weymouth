(ns kixi.weymouth.poly)

(defn get-min-max-bounds [v]
  (let [xvals (into [] (map first v))
        yvals(into [] (map second v))]
    {:minx (apply min xvals)
     :miny (apply min yvals)
     :maxx (apply max xvals)
     :maxy (apply max yvals)}))


(defn inside-bounds? [xym {:keys [minx miny maxx maxy] :as polymap}]
  (and (>= (first xym) minx)
       (<= (first xym) maxx)
       (>= (second xym) miny)
       (<= (second xym) maxy)))


(comment

  (def marker [375295.0 413465.0])


  (def polyvec [[ 375294.67, 413467.08 ], [ 375289.870000000111759, 413462.880000000819564 ], [ 375285.070000000298023, 413468.380000000819564 ], [ 375289.769999999552965, 413472.380000000819564 ], [ 375294.67, 413467.08 ]])

  (def polymap (get-min-max-bounds polyvec))
  (def is-true? (inside-bounds? marker polymap )))
