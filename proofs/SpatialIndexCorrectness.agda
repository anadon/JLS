------------------------------------------------------------------------
-- Machine-checked correctness proofs for JLS's drag/drop collision
-- detection and repaint culling (issues #3, #17).
--
-- What is proven here, and where the corresponding Java lives:
--
--   THEOREM 1 (query-parity): a uniform-grid spatial index query --
--   "collect the elements stored in every cell the query rectangle
--   covers, then keep those whose bounds touch the rectangle" --
--   returns EXACTLY the elements a brute-force scan over all elements
--   would return. In particular the grid can never miss an element
--   (no false negatives), so the exact-predicate filter afterwards
--   makes the result identical to the full scan.
--     Java: jls.SpatialIndex.insert/query/boundsTouch, used by
--     jls.Circuit.elementsNear/elementsAt for hover hit-testing,
--     rubber-band selection, and the drag overlap (collision) checks
--     in jls.edit.SimpleEditor.overlap()/connect().
--
--   THEOREM 2 (culling-parity): enumerating repaint candidates
--   through the index with the clip grown by the draw margin, then
--   applying the exact visibility predicate, draws EXACTLY the
--   elements the old full scan drew. So index-driven dirty-region
--   repaints during a drag are pixel-identical to full scans.
--     Java: jls.Circuit.draw's candidate query (clip grown by
--     DRAW_MARGIN) followed by mayBeVisible.
--
-- Modelling assumptions, each pinned to the implementation by
-- test/jls/ProofBridgeTest.java:
--
--   (A1) Index intervals are non-empty (lo <= hi): SpatialIndex clamps
--        widths with Math.max(w, 0) on insert/remove/query.
--   (A2) The cell function is monotone: Math.floorDiv(_, CELL) with
--        CELL > 0 satisfies i <= j -> floorDiv(i) <= floorDiv(j).
--   (A3) boundsTouch(a, b) is the closed-interval overlap test
--        (a.x <= b.x + b.width && b.x <= a.x + a.width, same in y),
--        i.e. Touch below, per axis.
--   (A4) Rectangle.intersects on non-degenerate rectangles is the
--        open-interval overlap test (strict <), i.e. Cross below, and
--        Rectangle.grow(m, m) maps [lo, hi] to [lo - m, hi + m].
--   (A5) The draw margin is non-negative (DRAW_MARGIN = 8 * spacing).
--
-- An element is inserted into every grid cell its bounds cover, and a
-- query visits every cell the query rectangle covers (SpatialIndex
-- loops cx1..cx2 x cy1..cy2 on both sides), so "the cell loops find
-- element e" is exactly "e's cell range and the query's cell range
-- overlap on both axes" -- Visits below; lemma visits⇔shared checks
-- that reading against the shared-cell formulation.
--
-- Checked with: Agda 2.6.3, agda-stdlib as packaged for Ubuntu 24.04
-- (v1.7.3). Run: agda -i /usr/share/agda-stdlib -i proofs \
--                     proofs/SpatialIndexCorrectness.agda
------------------------------------------------------------------------

module SpatialIndexCorrectness where

open import Data.Empty using (⊥-elim)
open import Data.Integer.Base using (ℤ; _+_; _-_; -_; _≤_; _<_; 0ℤ)
open import Data.Integer.Properties
  using (≤-refl; ≤-trans; ≤-total; <⇒≤; _≤?_; _<?_;
         +-monoˡ-≤; +-monoʳ-≤; +-monoˡ-<; neg-mono-≤;
         +-assoc; +-inverseˡ; +-inverseʳ; +-identityʳ)
open import Data.List.Base using (List; []; _∷_; filter)
open import Data.Product using (_×_; _,_; proj₁; proj₂; ∃-syntax)
open import Data.Sum.Base using (inj₁; inj₂)
open import Function.Base using (_∘_)
open import Relation.Binary.PropositionalEquality
  using (_≡_; refl; sym; trans; cong; subst)
open import Relation.Nullary using (Dec; yes; no; ¬_)
open import Relation.Nullary.Product using (_×-dec_)

------------------------------------------------------------------------
-- Part 0: integer arithmetic helpers
------------------------------------------------------------------------

-- (i - m) + m ≡ i
-+-cancel : ∀ i m → (i - m) + m ≡ i
-+-cancel i m =
  trans (+-assoc i (- m) m)
        (trans (cong (i +_) (+-inverseˡ m)) (+-identityʳ i))

-- (i + m) - m ≡ i
+--cancel : ∀ i m → (i + m) - m ≡ i
+--cancel i m =
  trans (+-assoc i m (- m))
        (trans (cong (i +_) (+-inverseʳ m)) (+-identityʳ i))

-- a strict bound on a value shifted down by m weakens to a closed
-- bound with m moved to the other side: i - m < j  →  i ≤ j + m
unshift-lo : ∀ {i j} m → i - m < j → i ≤ j + m
unshift-lo {i} {j} m lt =
  <⇒≤ (subst (_< j + m) (-+-cancel i m) (+-monoˡ-< m lt))

-- and symmetrically: i < j + m  →  i - m ≤ j
unshift-hi : ∀ {i j} m → i < j + m → i - m ≤ j
unshift-hi {i} {j} m lt =
  <⇒≤ (subst (i - m <_) (+--cancel j m) (+-monoˡ-< (- m) lt))

------------------------------------------------------------------------
-- Part 1: intervals (one axis of a rectangle)
------------------------------------------------------------------------

-- A non-empty closed interval [lo, hi]. Non-emptiness models the
-- Math.max(width, 0) clamp in SpatialIndex (assumption A1).
record Ival : Set where
  constructor ival
  field
    lo hi : ℤ
    sane  : lo ≤ hi
open Ival

-- SpatialIndex.boundsTouch, one axis (assumption A3): closed-interval
-- overlap, counting zero-area contact.
Touch : Ival → Ival → Set
Touch a b = (lo a ≤ hi b) × (lo b ≤ hi a)

-- java.awt.Rectangle.intersects, one axis (assumption A4): strict.
Cross : Ival → Ival → Set
Cross a b = (lo a < hi b) × (lo b < hi a)

Touch? : ∀ a b → Dec (Touch a b)
Touch? a b = (lo a ≤? hi b) ×-dec (lo b ≤? hi a)

Cross? : ∀ a b → Dec (Cross a b)
Cross? a b = (lo a <? hi b) ×-dec (lo b <? hi a)

-- c lies within interval r
In : ℤ → Ival → Set
In c r = (lo r ≤ c) × (c ≤ hi r)

-- two touching intervals share a point (the larger of the two los)
common : ∀ p q → Touch p q → ∃[ c ] (In c p × In c q)
common p q (p≤q , q≤p) with ≤-total (lo p) (lo q)
... | inj₁ lp≤lq = lo q , (lp≤lq , q≤p) , (≤-refl , sane q)
... | inj₂ lq≤lp = lo p , (≤-refl , sane p) , (lq≤lp , p≤q)

------------------------------------------------------------------------
-- Part 2: rectangles
------------------------------------------------------------------------

record Rect : Set where
  constructor rect
  field
    rx ry : Ival
open Rect

-- SpatialIndex.boundsTouch on rectangles (assumption A3)
TouchR : Rect → Rect → Set
TouchR a b = Touch (rx a) (rx b) × Touch (ry a) (ry b)

TouchR? : ∀ a b → Dec (TouchR a b)
TouchR? a b = Touch? (rx a) (rx b) ×-dec Touch? (ry a) (ry b)

------------------------------------------------------------------------
-- Part 3: list filtering machinery for the set-equality statements
------------------------------------------------------------------------

module _ {A : Set} where

  -- pointwise-equivalent decidable predicates filter identically
  filter-ext : {P Q : A → Set}
               (P? : ∀ x → Dec (P x)) (Q? : ∀ x → Dec (Q x)) →
               (∀ x → P x → Q x) → (∀ x → Q x → P x) →
               ∀ xs → filter P? xs ≡ filter Q? xs
  filter-ext P? Q? p⇒q q⇒p [] = refl
  filter-ext P? Q? p⇒q q⇒p (x ∷ xs) with P? x | Q? x
  ... | yes p | yes q = cong (x ∷_) (filter-ext P? Q? p⇒q q⇒p xs)
  ... | yes p | no ¬q = ⊥-elim (¬q (p⇒q x p))
  ... | no ¬p | yes q = ⊥-elim (¬p (q⇒p x q))
  ... | no ¬p | no ¬q = filter-ext P? Q? p⇒q q⇒p xs

  -- pre-filtering by a weaker predicate changes nothing
  filter-subsume : {P Q : A → Set}
                   (P? : ∀ x → Dec (P x)) (Q? : ∀ x → Dec (Q x)) →
                   (∀ x → P x → Q x) →
                   ∀ xs → filter P? (filter Q? xs) ≡ filter P? xs
  filter-subsume P? Q? p⇒q [] = refl
  filter-subsume P? Q? p⇒q (x ∷ xs) with Q? x
  ... | yes q with P? x
  ...   | yes p = cong (x ∷_) (filter-subsume P? Q? p⇒q xs)
  ...   | no ¬p = filter-subsume P? Q? p⇒q xs
  filter-subsume P? Q? p⇒q (x ∷ xs) | no ¬q with P? x
  ...   | yes p = ⊥-elim (¬q (p⇒q x p))
  ...   | no ¬p = filter-subsume P? Q? p⇒q xs

------------------------------------------------------------------------
-- Part 4: the uniform grid (jls.SpatialIndex)
--
-- Parameterised over the cell function; Math.floorDiv(_, CELL) is one
-- (assumption A2, monotonicity pinned by ProofBridgeTest).
------------------------------------------------------------------------

module Grid (cellOf      : ℤ → ℤ)
            (cellOf-mono : ∀ {i j} → i ≤ j → cellOf i ≤ cellOf j) where

  -- the (contiguous) range of cell coordinates an interval covers:
  -- cx1..cx2 in SpatialIndex.insert/query
  cells : Ival → Ival
  cells a = ival (cellOf (lo a)) (cellOf (hi a)) (cellOf-mono (sane a))

  -- "the query's double cell loop finds element b": their cell ranges
  -- overlap on both axes
  Visits : Rect → Rect → Set
  Visits b r = Touch (cells (rx b)) (cells (rx r))
             × Touch (cells (ry b)) (cells (ry r))

  Visits? : ∀ b r → Dec (Visits b r)
  Visits? b r = Touch? (cells (rx b)) (cells (rx r))
          ×-dec Touch? (cells (ry b)) (cells (ry r))

  -- a grid cell, and membership of a cell in a rectangle's cell set
  Cell : Set
  Cell = ℤ × ℤ

  InCells : Cell → Rect → Set
  InCells (cx , cy) r = In cx (cells (rx r)) × In cy (cells (ry r))

  -- sanity of the Visits reading: cell ranges overlap on both axes
  -- exactly when some concrete grid cell holds both rectangles --
  -- which is what "insert stored b in that cell and the query loop
  -- reaches that cell" means
  visits⇒shared : ∀ b r → Visits b r → ∃[ c ] (InCells c b × InCells c r)
  visits⇒shared b r (tx , ty)
    with common (cells (rx b)) (cells (rx r)) tx
       | common (cells (ry b)) (cells (ry r)) ty
  ... | cx , inbx , inrx | cy , inby , inry =
      (cx , cy) , (inbx , inby) , (inrx , inry)

  shared⇒visits : ∀ b r → ∃[ c ] (InCells c b × InCells c r) → Visits b r
  shared⇒visits b r ((cx , cy) , (inbx , inby) , (inrx , inry)) =
      (≤-trans (proj₁ inbx) (proj₂ inrx) , ≤-trans (proj₁ inrx) (proj₂ inbx))
    , (≤-trans (proj₁ inby) (proj₂ inry) , ≤-trans (proj₁ inry) (proj₂ inby))

  -- membership in the query result: the cell loops found b AND the
  -- exact boundsTouch filter kept it (SpatialIndex.query's inner if)
  QueryHit : Rect → Rect → Set
  QueryHit b r = TouchR b r × Visits b r

  QueryHit? : ∀ b r → Dec (QueryHit b r)
  QueryHit? b r = TouchR? b r ×-dec Visits? b r

  -- no false positives: everything returned really touches
  query-sound : ∀ b r → QueryHit b r → TouchR b r
  query-sound b r = proj₁

  -- no false negatives: touching rectangles share a grid cell, because
  -- the monotone cell function maps each touching pair of interval
  -- endpoints to a touching pair of cell ranges
  query-complete : ∀ b r → TouchR b r → QueryHit b r
  query-complete b r t@(tx , ty) =
    t , (cellOf-mono (proj₁ tx) , cellOf-mono (proj₂ tx))
      , (cellOf-mono (proj₁ ty) , cellOf-mono (proj₂ ty))

  --------------------------------------------------------------------
  -- THEOREM 1. Grid query parity: over any element list, the index
  -- query equals the brute-force boundsTouch scan. This is the
  -- contract SpatialIndexTest.queriesMatchBruteForceOnWiredCircuit
  -- checks empirically, here established for ALL geometries.
  --------------------------------------------------------------------
  query-parity : {E : Set} (bounds : E → Rect) (r : Rect) →
                 ∀ els → filter (λ e → QueryHit? (bounds e) r) els
                       ≡ filter (λ e → TouchR?  (bounds e) r) els
  query-parity bounds r =
    filter-ext (λ e → QueryHit? (bounds e) r)
               (λ e → TouchR? (bounds e) r)
               (λ e → query-sound (bounds e) r)
               (λ e → query-complete (bounds e) r)

  ----------------------------------------------------------------------
  -- Part 5: draw culling (jls.Circuit.draw + mayBeVisible)
  --
  -- m is the draw margin (DRAW_MARGIN = 8 * spacing >= 0, assumptions
  -- A4/A5).
  ----------------------------------------------------------------------

  module Culling (m : ℤ) (0≤m : 0ℤ ≤ m) where

    -- java.awt.Rectangle.grow(m, m), one axis: [lo, hi] to
    -- [lo - m, hi + m]; non-negative m keeps the interval sane
    grow : Ival → Ival
    grow a = ival (lo a - m) (hi a + m) sane′
      where
      -- - m ≤ 0 (from 0 ≤ m), so growing only widens
      lo-m≤lo : lo a - m ≤ lo a
      lo-m≤lo = subst (lo a - m ≤_) (+-identityʳ (lo a))
                      (+-monoʳ-≤ (lo a) (neg-mono-≤ 0≤m))
      sane′ : lo a - m ≤ hi a + m
      sane′ = ≤-trans lo-m≤lo
              (≤-trans (sane a)
                (subst (_≤ hi a + m) (+-identityʳ (hi a))
                       (+-monoʳ-≤ (hi a) 0≤m)))

    -- Circuit.mayBeVisible: element bounds grown by the margin,
    -- strictly intersected with the clip (assumption A4)
    MayBeVisible : Rect → Rect → Set
    MayBeVisible b clip = Cross (grow (rx b)) (rx clip)
                        × Cross (grow (ry b)) (ry clip)

    MayBeVisible? : ∀ b clip → Dec (MayBeVisible b clip)
    MayBeVisible? b clip = Cross? (grow (rx b)) (rx clip)
                     ×-dec Cross? (grow (ry b)) (ry clip)

    -- the clip as Circuit.draw's candidate query grows it
    growR : Rect → Rect
    growR (rect x y) = rect (grow x) (grow y)

    -- KEY LEMMA: the margin transfers -- if the grown element bounds
    -- strictly cross the clip, the raw bounds (closed-)touch the grown
    -- clip, so the index query over the grown clip cannot cull a
    -- visible element
    margin-touch : ∀ a c → Cross (grow a) c → Touch a (grow c)
    margin-touch a c (l< , <h) = unshift-lo m l< , unshift-hi m <h

    margin-touchR : ∀ b clip → MayBeVisible b clip → TouchR b (growR clip)
    margin-touchR b clip (vx , vy) =
      margin-touch (rx b) (rx clip) vx , margin-touch (ry b) (ry clip) vy

    -- every possibly-visible element is a candidate
    culling-complete : ∀ b clip → MayBeVisible b clip →
                       QueryHit b (growR clip)
    culling-complete b clip v =
      query-complete b (growR clip) (margin-touchR b clip v)

    --------------------------------------------------------------------
    -- THEOREM 2. Draw-culling parity: filtering the index candidates
    -- (clip grown by the margin) with the exact visibility predicate
    -- selects exactly the elements a full scan selects -- the pixels
    -- drawn are unchanged. This is the contract
    -- DrawCullingParityTest.culledCandidatesMatchFullScan checks
    -- empirically, here established for ALL geometries.
    --------------------------------------------------------------------
    culling-parity : {E : Set} (bounds : E → Rect) (clip : Rect) →
      ∀ els →
        filter (λ e → MayBeVisible? (bounds e) clip)
               (filter (λ e → QueryHit? (bounds e) (growR clip)) els)
      ≡ filter (λ e → MayBeVisible? (bounds e) clip) els
    culling-parity bounds clip =
      filter-subsume (λ e → MayBeVisible? (bounds e) clip)
                     (λ e → QueryHit? (bounds e) (growR clip))
                     (λ e → culling-complete (bounds e) clip)
