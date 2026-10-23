# OnMyWay AI delivery scope

Build a native Pleos Connect errand planner for a solo general-division contest entry. Prioritize a short, reliable demonstration over feature breadth. Korean UI; English source and engineering documentation.

Core flow: natural-language errands -> editable structured requirements -> sourced place candidates -> deterministic time-window plan -> explicit user approval -> Pleos next-stop navigation -> explicit errand completion -> replan remaining errands.

The planner must include travel, waiting, dwell, closing times and final arrival deadline. Never silently drop mandatory errands. Unknown hours/service or missing road times must not be reported as verified feasibility. Fixture mode is visibly separate from live providers. Arrival is not completion. Driving restrictions fail closed when vehicle state is unknown.

Use a single monorepo: Kotlin native Android app and Python FastAPI backend, no external database. OpenAI key will be supplied by the user later through server-only environment configuration. Never embed secrets in APK, source, screenshots, logs or submissions.

Required delivery evidence: passing tests, actual APK build/install/run, actual Pleos integration, real API smoke tests once credentials are supplied, recorded demo, filled anonymous project section of official application, Korean pitch deck, email draft and submission checklist. Keep incomplete evidence explicit. User retains legal consent, identity/signature, paid or public actions and final submission.
