(ns fn8-io.io.sound)

(def sound-output (new js/AudioContext))

(def oscillator (.createOscillator sound-output))
(def gain (.createGain sound-output))

(.connect oscillator gain)
(.connect gain (.-destination sound-output))

(set! (.-value (.-gain gain)) 100)
(set! (.-value (.-frequency oscillator)) 3200)
(set! (.-type oscillator) "sine")
(.suspend sound-output)
(.start oscillator)

(defn start-sound!
  []
  (.resume sound-output))

(defn stop-sound!
  []
  (.suspend sound-output))
