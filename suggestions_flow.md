# Suggestions + Next-Word Prediction Flow (Signal Chain)

This is a “visual map” of what happens in NewSoftKeyboard when a user types, how the suggestion strip gets populated, and when the next‑word prediction engines actually run.

## Legend

- **Typed-word suggestions**: completions/corrections for the _current composing word_ (e.g., typing `t` → `the`, `to`, …).
- **Next-word suggestions**: predictions for the _next word after a committed word_ (e.g., committed `keep` → next suggestions `me`, `informed`, …).
- **Important internal state**:
  - `WordComposer`: the current composing word (characters not yet committed as a word).
  - `SuggestImpl.mNextSuggestions`: “last computed next-word list” (computed on commit/pick/space flows, not per keystroke).

---

## A) User types the letter `T` (composing starts or continues)

```text
Keypress 't'
  │
  ▼
ImeSuggestionsController.handleCharacter(...)
  │
  ▼
CharacterInputHandler.handleCharacter(...)                  ime/.../CharacterInputHandler.java:48
  │
  ├─ WordComposer.add('t')                                  (updates composing buffer)
  │
  ├─ InputConnection.setComposingText("t", 1)               (editor shows composing text)
  │
  └─ SuggestionsUpdater.postUpdateSuggestions()             (debounced)
        │
        ▼
      SuggestionRefresher.performUpdateSuggestions(...)     ime/.../SuggestionRefresher.java:18
        │
        └─ SuggestImpl.getSuggestions(wordComposer)         ime/.../SuggestImpl.java:210
             │
             ├─ suggestions[0] = typed word ("t")
             ├─ query dictionaries (abbrev/autotext/user/main/contacts…)
             ├─ inject prefix-matches from mNextSuggestions (if any)
             ├─ de-dupe + trim to max
             └─ return list
        │
        ▼
      CandidateView.setSuggestions(list, highlightedIndex)
```

### What _does not_ happen here

- **Next-word engines do not run when you type `t`.**
- Typing a character only rebuilds **typed-word suggestions** for the composing word.
- `mNextSuggestions` is only used as an optional _overlay_ (“prefix matches”) if it already exists.

---

## B) User finishes the word (presses `SPACE`, punctuation, or `ENTER`)

This is the moment where the keyboard:

1. commits the word (typed or autocorrected), then
2. computes **next‑word suggestions** for that committed word, and
3. puts those next‑word suggestions into the suggestion strip (unless end-of-sentence).

```text
Keypress separator (SPACE / punctuation / ENTER)
  │
  ▼
ImeSuggestionsController.handleSeparator(...)
  │
  ▼
SeparatorHandler.handleSeparator(...)                       ime/.../SeparatorHandler.java:13
  │
  ├─ (if was predicting) commit composed word to editor
  │     │
  │     ▼
  │   ImeSuggestionsController.commitWordToInput(...)
  │     ├─ recordLastCommittedWordForNextSuggestions(...)
  │     └─ SuggestImpl.notifyWordCommitted(committedWord)   ime/.../SuggestImpl.java:203
  │           └─ SuggestionsProvider.notifyWordCommitted(...) ime/.../SuggestionsProvider.java:264
  │                 └─ NextWordSuggestionsPipeline.notifyWordCommitted(...)
  │                       └─ NextWordPredictionEngines.notifyWordCommitted(...)
  │                            (adds to engine context window; default target is 20 words)
  │
  ├─ if end-of-sentence:
  │     ├─ Suggest.resetNextWordSentence()
  │     ├─ clear last committed word
  │     └─ clear suggestion strip
  │
  └─ else (normal space/punctuation inside sentence):
        └─ SuggestImpl.getNextSuggestions(committedWord)    ime/.../SuggestImpl.java:150
              └─ SuggestionsProvider.getNextWords(...)      ime/.../SuggestionsProvider.java:257
                    └─ NextWordSuggestionsPipeline.appendNextWords(...) ime/nextword/.../NextWordSuggestionsPipeline.java:71
                          └─ NextWordPredictionEngines.appendNextWords(...) ime/nextword/.../NextWordPredictionEngines.java:216
              └─ CandidateView.setSuggestions(nextWords, -1)
```

---

## C) User starts the _next_ word (types `T` after a space)

At this point, `SuggestImpl.mNextSuggestions` usually contains the next‑word list computed in (B).

So when the user now types `t`:

- `getSuggestions("t")` builds normal dictionary completions for `t`, **and**
- it scans `mNextSuggestions` and injects a few items that start with `t` (so predicted next words like `to/the/that…` can show up early).

This is why the UX can “feel random” for 1-letter prefixes if next‑word candidates dominate: you’re seeing a _prefix-filtered view of predicted-next-word_ mixed into typed-word completions.

---

## D) Manual pick from the suggestion strip (user taps a candidate)

Manual pick is similar to committing via SPACE, except it can:

- insert an auto-space (with fallbacks for editors that ignore `commitText(" ")`), and
- either show the “add to dictionary” hint or immediately show next‑word suggestions for the picked word.

```text
User taps a suggestion chip
  │
  ▼
ImeSuggestionsController.pickSuggestionManually(...)
  │
  └─ SuggestionPicker.pickSuggestionManually(...)           ime/.../SuggestionPicker.java:60
        ├─ commit picked word to editor
        ├─ (optional) auto-space insertion
        └─ AddToDictionaryHintController.handlePostPick(...) ime/.../AddToDictionaryHintController.java:32
              ├─ show "Add to dictionary" hint (sometimes)
              └─ else: setSuggestions(suggest.getNextSuggestions(pickedWord), -1)
```
