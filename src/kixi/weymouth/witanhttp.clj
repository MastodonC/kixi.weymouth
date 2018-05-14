(ns kixi.weymouth.witanhttp
  (:require [environ.core :refer [env]]
            [clj-http.client :as http]
            [taoensso.timbre :as log]
            [clojure.data.json :as json])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(def debug-http false)

(defn healthcheck
  "A basic check to the Witan API to confirm that the API is alive. The body of the response should say 'hello'"
  []
  (http/get (str (env :witan-endpoint) "/healthcheck")))

(defn json-response-to-map
  "Takes a token pair JSON string from the Witan API as an input and converts to a Clojure map, JSON key names are converted to Clojure keywords."
  [token-pair]
  (json/read-str token-pair :key-fn keyword))

(defn login-user
  "This function attempts to login a user based on their username and password (stored as environment variables. If the login attempt is successful (201 status code) then the body of the response is transformed into a Clojure map with the token-pair-to-map function. If the login attempt fails then a nil value is return from the function."
  []
  (let [response (try+
                  (http/post (str (env :witan-endpoint) "/api/login")
                             {:form-params {:username (env :witan-username)
                                            :password (env :witan-password)}
                              :content-type :json
                              :accept :json
                              :debug debug-http})
                  (catch [:status 401] {:keys [request-time headers body]}
                    (log/warn "401" request-time headers))
                  (catch [:status 403] {:keys [request-time headers body]}
                    (log/warn "403" request-time headers))
                  (catch Object _
                    (log/error "Unexpected response.")
                    (throw+)))]
    (if (= 201 (:status response))
      (json-response-to-map (:body response))
      nil)))

(defn query-receipt-id*
  "Receipts need to be queried to get their expected response result (uri etc). When a 200 status code is returned the function returns the results of the receipt call, otherwise a nil is returned and the calling function is expected to attempt the query again until a 200 status code is given. It is up to you to handle the body of the response of the receipt."
  [auth-token receipt-id]
  (let [response (try+
                  (http/get (str (env :witan-endpoint) "/api/receipts/" receipt-id)
                            {:headers {:authorization auth-token}
                             :content-type :json
                             :accept :json
                             :debug debug-http})
                  (catch [:status 401] {:keys [request-time headers body]}
                    (log/warn "401" request-time headers))
                  (catch [:status 402] {:keys [request-time headers body]}
                    (log/warn "402" request-time headers))
                  (catch [:status 403] {:keys [request-time headers body]}
                    (log/warn "403" request-time headers))
                  (catch Object {:keys [request-time headers body]}
                    (log/info "query-receipt-id* response code.... " (:status body))
                    (log/error "Unexpected response from query-receipt-id*" body))) ]
    (if (= 200 (:status response))
      (:body response)
      nil)))

(defn query-receipt-id
  ""
  [auth-token receipt-id]
  (if-let [body (query-receipt-id* auth-token receipt-id)]
    body
    (do
      (Thread/sleep 1000)
      (query-receipt-id* auth-token receipt-id))))

(defn request-file-download-link
  "Requests a file download link from the Witan API. The response is a receipt id which is then queried against the query-receipt-id function to get the download link. Depending on loads you may have to query the query-receipt-id a few times. nil is returned if the API refuses or can't find the file id you have requested."
  [auth-token file-id]
  (let [response (try+
                  (http/post (str (env :witan-endpoint) "/api/files/" file-id "/link")
                             {:headers {:authorization auth-token}
                              :debug debug-http
                              :content-type :json
                              :accept :json})
                  (catch [:status 401] {:keys [request-time headers body]}
                    (log/warn "401" request-time headers))
                  (catch [:status 403] {:keys [request-time headers body]}
                    (log/warn "403" request-time headers))
                  (catch Object _
                    (log/error "Unexpected response.")))]
    (if (= 202 (:status response))
      (:receipt-id (json-response-to-map (:body response)))
      nil)))

(defn download-file
  "Downloads the file from the link provided. Function will return the contents of the file."
  [uri]
  (slurp uri))

(defn download-file-from-witan
  "This function will take a file-id and do the full sequence of calls to download a file. The output will be the contents of the file, it is then up to you how the output is handled."
  [file-id]
  (let [auth-token (get-in (login-user) [:token-pair :auth-token])
        download-link-receipt-id (request-file-download-link auth-token file-id)
        file-link (:witan.httpapi.spec/uri (json-response-to-map
                                            (query-receipt-id auth-token download-link-receipt-id)))]
    {:file-id file-id
     :payload (download-file file-link)}))
