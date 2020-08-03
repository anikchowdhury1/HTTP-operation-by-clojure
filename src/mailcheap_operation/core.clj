(ns mailcheap-operation.core
  (:gen-class)
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [clojure.inspector :as inspect_data]
            [clj-http.util :as utility]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [ajax.core :refer [GET POST]]
            [clojure.java.shell :as shell])


  (:use clojure.pprint))

(def AUTH_HEADER "Token ***")
(def USER_AGENT "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36")
(def ACCEPT_ENCD "gzip, deflate, br")
(def accept "*/*")
(def Accept-Language "en-GB,en-US;q=0.9,en;q=0.8")

(def error-msg {:password ["Password must contain at least 1 uppercase letter."]})
(def domain-api-url "https://mail.**.dk/api/v1/***/")
(def account-api-url "https://mail.**.dk/api/v1/**/")
(def list-domain-api-url "https://mail.**.dk/api/v1/**/?page=")
(def delete-domain-api-url "https://mail.**.dk/api/v1/**/")


(defn password-error?
  [response-body]
  (contains? response-body :password))

(defn gen-passwd []
  (->> (shell/sh "pwgen" "-c" "-n" "-y" "-1")
    (:out)
    (str/trim)))

(defn delete-domains [domain-id]
  (try
    (let [result (http/delete (str delete-domain-api-url domain-id "/")
                   {:headers               {:Authorization   AUTH_HEADER
                                            :User-Agent      USER_AGENT
                                            :accept-encoding ACCEPT_ENCD
                                            :Accept          accept
                                            :Accept-Language Accept-Language}
                    :content-type          :json
                    :as                    :json
                    :coerce                :always
                    :throw-exceptions      false
                    :throw-entire-message? true
                    :debug                 false})]

      (if (= (:status result) 201)
        {:status 201
         :body   "Domain successfully deleted."}
        (do
          (println (format "Failed to delete Domain: %s, %s"
                     (:status result)
                     (:body result)))
          #_(if (= (:body result) error-msg)
              (let [new_pass ()])))))
    (catch Exception e
      #_(do
          (println (format "Caught exception while creating domain: %s"
                     (.getMessage e)))
          {:status 500
           :body   "Failed to create domain."}))))

(defn list-domains-in-page
  [page]
  (try
    (let [result (:body (http/get (str list-domain-api-url page)
                          {:headers               {:Authorization   AUTH_HEADER
                                                   :User-Agent      USER_AGENT
                                                   :accept-encoding ACCEPT_ENCD
                                                   :Accept          accept
                                                   :Accept-Language Accept-Language}
                           :content-type          :json
                           :as                    :json
                           :coerce                :always
                           :throw-exceptions      false
                           :throw-entire-message? true
                           :debug                 false}))
          list-id (get-in (nth result page {}) [:pk])]

      (delete-domains list-id)



      (nth result page {}))
    (catch Exception e
      (do
        (println (format "Caught exception while creating domain: %s"
                   (.getMessage e)))
        {:status 500
         :body   "Failed to create domain."}))))


(defn list-all-domains
  ([]
   (list-all-domains 0 []))

  ([page domains]
   ;; Fetch domains in the new page.
   (let [new-domains (list-domains-in-page page)]
     ;; Check if new-domains are contain any element.
     ;; If it does not contain any element it means we
     ;; have gone through all pages.
     (if (seq new-domains)
       (recur
         ;; Increase page number.
         (inc page)
         ;; Add new-domains to the list of original domains.
         (concat domains new-domains))
       ;; Return all the domains.
       domains))))

(defn create-domains [domain-name]
  (try
    (let [domain domain-name
          result (http/post domain-api-url
                   {:form-params           {:name domain}
                    :headers               {:Authorization   AUTH_HEADER
                                            :User-Agent      USER_AGENT
                                            :accept-encoding ACCEPT_ENCD
                                            :Accept          accept
                                            :Accept-Language Accept-Language}
                    :content-type          :json
                    :as                    :json
                    :coerce                :always
                    :throw-exceptions      false
                    :throw-entire-message? true
                    :debug                 false})]

      (if (= (:status result) 201)
        {:status 201
         :body   "Domain successfully created."}
        (do
          (println (format "Failed to create Domain: %s, %s"
                     (:status result)
                     (:body result)))
          #_(if (= (:body result) error-msg)
              (let [new_pass ()])))))
    (catch Exception e
      (do
        (println (format "Caught exception while creating domain: %s"
                   (.getMessage e)))
        {:status 500
         :body   "Failed to create domain."}))))

(defn create-accounts [email password]
  (try
    (let [user_name email
          role      "SimpleUsers"
          pass      password
          result    (http/post account-api-url
                      {:form-params           {:username user_name
                                               :password pass
                                               :role     role}
                       :headers               {:Authorization   AUTH_HEADER
                                               :User-Agent      USER_AGENT
                                               :accept-encoding ACCEPT_ENCD
                                               :Accept          accept
                                               :Accept-Language Accept-Language}
                       :content-type          :json
                       :as                    :json
                       :coerce                :always
                       :throw-exceptions      false
                       :throw-entire-message? true
                       :debug                 false})]
      #_(println "mail pass" user_name pass)
      (if (= (:status result) 201)
        (do
          {:status 200
           :body   "Account successfully created."}
          (let [email_new email
                pass_new  password
                data_new  (str email_new "    " pass_new "\n")]
            (spit "new_email_pass.txt" data_new :append true)))

        (do
          (println (format "Failed to create account: %s, %s"
                     (:status result)
                     (:body result)))
          (if (password-error? (:body result))
            (let [new_pass (gen-passwd)]
              (println "new pass" new_pass)
              (create-accounts email new_pass))))))

    (catch Exception e
      #_(println "ERROR IS:" (:body e))
      #_(do
          (println (format "Caught exception while creating account: %s"
                     (.getMessage e)))

          (let [error (:body (.getMessage e))]
            (println "error" (.getMessage e)))))))




(defn file-modification [line-data]
  (let [email  (first line-data)
        pass   (str/trim (apply str (rest line-data)))
        domain (nth (re-find (re-matcher #"@(.*)" (apply str email))) 1)]
    ;(println "email pass domain" email pass domain)
    (create-domains domain)
    (create-accounts email pass)))




(defn process-file-by-lines
  "Process file reading it line-by-line"
  ([file]
   (process-file-by-lines file identity))
  ([file process-fn]
   (process-file-by-lines file process-fn file-modification))
  ([file process-fn output-fn]
   (with-open [rdr (clojure.java.io/reader file)]
     (doseq [line (line-seq rdr)]
       ;(println "test check" line)
       (output-fn
         (process-fn (str/split line #" ")))))))



(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (process-file-by-lines "email-pass11.txt"))


