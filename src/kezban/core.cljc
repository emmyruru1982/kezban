(ns kezban.core
  (:require [clojure.pprint :as pp]
            [clojure.walk :as walk]
            [clojure.string :as str]
            #?(:clj [clojure.java.io :as io]))
  #?(:cljs (:require-macros [cljs.core :as core]
                            [cljs.support :refer [assert-args]]))
  #?(:clj (:import
            (java.io StringWriter)
            (java.util.concurrent TimeUnit)
            (java.net URL)
            (clojure.lang RT))))

#?(:cljs
   (defmacro assert-all
     "Internal - do not use!"
     [fnname & pairs]
     `(do (when-not ~(first pairs)
            (throw (ex-info ~(str fnname " requires " (second pairs)) {:clojure.error/phase :macro-syntax-check})))
          ~(let [more (nnext pairs)]
             (when more
               (list* `assert-all fnname more))))))

#?(:clj
   (defmacro assert-all
     [& pairs]
     `(do (when-not ~(first pairs)
            (throw (IllegalArgumentException.
                     (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))))
          ~(let [more (nnext pairs)]
             (when more
               (list* `assert-all more))))))


(defmacro def-
  [name x]
  (list `def (with-meta name (assoc (meta name) :private true)) x))


(defmacro when-let*
  "Multiple binding version of when-let"
  [bindings & body]
  (when (seq bindings)
    (assert-all #?(:cljs when-let*)
      (vector? bindings) "a vector for its binding"
      (even? (count bindings)) "exactly even forms in binding vector"))
  (if (seq bindings)
    `(when-let [~(first bindings) ~(second bindings)]
       (when-let* ~(vec (drop 2 bindings)) ~@body))
    `(do ~@body)))


(defmacro if-let*
  "Multiple binding version of if-let"
  ([bindings then]
   `(if-let* ~bindings ~then nil))
  ([bindings then else]
   (when (seq bindings)
     (assert-all #?(:cljs if-let*)
       (vector? bindings) "a vector for its binding"
       (even? (count bindings)) "exactly even forms in binding vector"))
   (if (seq bindings)
     `(if-let [~(first bindings) ~(second bindings)]
        (if-let* ~(vec (drop 2 bindings)) ~then ~else)
        ~else)
     then)))


(defmacro ->>>
  "Takes a set of functions and value at the end of the arguments.
   Returns a result that is the composition
   of those funtions.Applies the rightmost of fns to the args(last arg is the value/input!),
   the next fn (left-to-right) to the result, etc."
  [& form]
  `((comp ~@(reverse (rest form))) ~(first form)))


(defn drop-first
  "Return a lazy sequence of all, except first value"
  [coll]
  (drop 1 coll))


(defn nth-safe
  "Returns the value at the index. get returns nil if index out of
   bounds,unlike nth nth-safe does not throw an exception, returns nil instead.nth-safe
   also works for strings, Java arrays, regex Matchers and Lists, and,
   in O(n) time, for sequences."
  ([coll n]
   (nth-safe coll n nil))
  ([coll n not-found]
   (nth coll n not-found)))


(defn nnth
  [coll index & indices]
  (reduce #(nth-safe %1 %2) coll (cons index indices)))


(defn third
  "Gets the third element from collection"
  [coll]
  (nth-safe coll 2))


(defn fourth
  "Gets the fourth element from collection"
  [coll]
  (nth-safe coll 3))


(defn fifth
  "Gets the fifth element from collection"
  [coll]
  (nth-safe coll 4))


(defn sixth
  "Gets the sixth element from collection"
  [coll]
  (nth-safe coll 5))


(defn seventh
  "Gets the seventh element from collection"
  [coll]
  (nth-safe coll 6))


(defn eighth
  "Gets the eighth element from collection"
  [coll]
  (nth-safe coll 7))


(defn ninth
  "Gets the ninth element from collection"
  [coll]
  (nth-safe coll 8))


(defn tenth
  "Gets the tenth element from collection"
  [coll]
  (nth-safe coll 9))


(defn- xor-result
  [x y]
  (if (and x y)
    false
    (or x y)))


#?(:clj
   (defmacro xor
     ([] true)
     ([x] x)
     ([x & next]
      (let [first  x
            second `(first '(~@next))
            ;; used this eval approach because of lack of private function usage in macro!
            result (xor-result (eval first) (eval second))]
        `(if (= (count '~next) 1)
           ~result
           (xor ~result ~@(rest next)))))))


#?(:clj
   (defmacro pprint-macro
     [form]
     `(pp/pprint (walk/macroexpand-all '~form))))


#?(:clj
   (defn eval-when
     [test form]
     (when test (eval form))))


;;does not support syntax-quote
(defmacro quoted?
  [form]
  (if (coll? form)
    (let [f (first form)
          s (str f)]
      `(= "quote" ~s))
    false))


(defn- type->str
  [type]
  (case type
    :char "class [C"
    :byte "class [B"
    :short "class [S"
    :int "class [I"
    :long "class [J"
    :float "class [F"
    :double "class [D"
    :boolean "class [Z"
    nil))


(defn array?
  ([arr]
   (array? nil arr))
  ([type arr]
   (let [c (class arr)]
     (if type
       (and (= (type->str type) (str c)) (.isArray c))
       (or (some-> c .isArray) false)))))


(defmacro error?
  "Returns true if executing body throws an error(exception, error etc.), false otherwise."
  [& body]
  `(try
     ~@body
     false
     (catch #?(:clj Throwable) #?(:cljs js/Error)  _#
       true)))


(defn lazy?
  [x]
  (= "class clojure.lang.LazySeq" (str (type x))))


(defn any?
  [pred coll]
  ((complement not-any?) pred coll))


(defn any-pred
  [& preds]
  (complement (apply every-pred (map complement preds))))


(defmacro try->
  [x & forms]
  `(try
     (-> ~x ~@forms)
     (catch #?(:clj Throwable) #?(:cljs js/Error)  _#)))


(defmacro try->>
  [x & forms]
  `(try
     (->> ~x ~@forms)
     (catch #?(:clj Throwable) #?(:cljs js/Error) _#)))


(defn has-ns?
  [ns]
  (boolean (try-> ns find-ns)))


(defn multi-comp
  ([fns a b]
   (multi-comp fns < a b))
  ([[f & others :as fns] order a b]
   (if (seq fns)
     (let [result   (compare (f a) (f b))
           f-result (if (= order >) (* -1 result) result)]
       (if (= 0 f-result)
         (recur others order a b)
         f-result))
     0)))


#?(:clj
   (defmacro with-out-str-data-map
     [& body]
     `(let [s# (StringWriter.)]
        (binding [*out* s#]
          (let [r# ~@body]
            {:result r#
             :str    (str s#)})))))


#?(:clj
   (defmacro with-err-str
     [& body]
     `(let [s# (StringWriter.)]
        (binding [*err* s#]
          (eval '(do ~@body))
          (str s#)))))


(defmacro letm
  [bindings]
  (assert-all #?(:cljs letm)
    (vector? bindings) "a vector for its binding"
    (even? (count bindings)) "an even number of forms in binding vector")
  `(let* ~(destructure bindings)
     (merge ~@(map #(hash-map (keyword %) %) (take-nth 2 bindings)))))


(defn in?
  [x coll]
  (boolean (some #(= x %) coll)))


(defn take-while-and-n-more
  [pred n coll]
  (let [[head tail] (split-with pred coll)]
    (concat head (take n tail))))


(defn dissoc-in
  ([m ks]
   (if-let [[k & ks] (seq ks)]
     (if (seq ks)
       (let [v (dissoc-in (get m k) ks)]
         (if (empty? v)
           (dissoc m k)
           (assoc m k v)))
       (dissoc m k))
     m))
  ([m ks & kss]
   (if-let [[ks' & kss] (seq kss)]
     (recur (dissoc-in m ks) ks' kss)
     (dissoc-in m ks))))


#?(:clj
   (defmacro with-timeout
     [msec & body]
     `(.get (future ~@body) ~msec TimeUnit/MILLISECONDS)))


#?(:clj
   (defn url->file
     [src f]
     (let [source (URL. src)]
       (with-open [i (.openStream source)
                   o (io/output-stream f)]
         (io/copy i o))
       f)))


(defmacro cond-let
  [bindings & forms]
  (assert-all #?(:cljs cond-let)
    (vector? bindings) "a vector for its binding"
    (even? (count bindings)) "an even number of forms in binding vector")
  `(let* ~(destructure bindings)
     (cond ~@forms)))


(defn keywordize
  [s]
  (-> s
      str/lower-case
      str/trim
      (str/replace #"\s+" "-")
      keyword))


#?(:clj
   (defn source-clj-file
     [ns]
     (require ns)
     (some->> ns
              ns-publics
              vals
              first
              meta
              :file
              (.getResourceAsStream (RT/baseLoader))
              slurp)))


#?(:clj
   (defmacro time*
     [& forms]
     `(time
        (do
          ~@forms))))


(defn process-lazy-seq
  [f threshold lazy-s]
  (when-let [data (seq (take threshold lazy-s))]
    (doseq [d data]
      (f d))
    (recur f threshold (drop threshold lazy-s))))


(defmacro forall
 [seq-exprs body-expr]
 `(doall (for ~seq-exprs ~body-expr)))